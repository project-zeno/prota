package com.aaryannaidu.prota.bridge

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.provider.Settings
import android.util.Log
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.aaryannaidu.prota.accessibility.AIAssistAccessibilityService
import com.aaryannaidu.prota.api.LlmApiClient
import com.aaryannaidu.prota.notification.NotificationHelper
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bridge between React Native and Android accessibility service
 * Handles screen analysis requests and settings
 */
@ReactModule(name = AccessibilityBridgeModule.NAME)
class AccessibilityBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "AccessibilityBridge"
        private const val TAG = "AccessibilityBridge"
        private const val ACTION_ANALYZE_NOW = "com.aaryannaidu.prota.ANALYZE_NOW"
        private const val MIN_ANALYSIS_INTERVAL_MS = 3000L
    }

    private val llmClient = LlmApiClient()
    private val notificationHelper = NotificationHelper(reactContext)
    private val moduleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isAnalyzing = AtomicBoolean(false)
    
    @Volatile
    private var lastAnalysisTime = 0L

    // Broadcast receiver for notification button clicks
    private val analyzeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ANALYZE_NOW) {
                performAnalysis()
            }
        }
    }

    init {
        // Register receiver
        val filter = IntentFilter(ACTION_ANALYZE_NOW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reactApplicationContext.registerReceiver(analyzeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            reactApplicationContext.registerReceiver(analyzeReceiver, filter)
        }
    }

    override fun getName(): String = NAME

    /**
     * Main analysis flow - captures screenshot and gets AI insights
     */
    private fun performAnalysis(
        prompt: String = "Analyze this screen and provide 3 helpful insights or suggestions.",
        promise: Promise? = null
    ) {
        // Debouncing
        if (!isAnalyzing.compareAndSet(false, true)) {
            Log.w(TAG, "Analysis already in progress")
            promise?.reject("IN_PROGRESS", "Analysis already running")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < MIN_ANALYSIS_INTERVAL_MS) {
            isAnalyzing.set(false)
            promise?.reject("RATE_LIMIT", "Please wait before analyzing again")
            return
        }
        lastAnalysisTime = currentTime

        // Check service
        val service = AIAssistAccessibilityService.getInstance()
        if (service == null) {
            isAnalyzing.set(false)
            promise?.reject("SERVICE_NOT_ENABLED", "Accessibility service not enabled")
            return
        }

        // Run analysis
        moduleScope.launch {
            try {
                // Capture screenshot
                val screenshot = withContext(Dispatchers.Main) {
                    suspendCoroutine<String?> { continuation ->
                        service.captureScreenshot { continuation.resume(it) }
                    }
                }

                if (screenshot == null) {
                    promise?.reject("SCREENSHOT_FAILED", "Failed to capture screenshot")
                    return@launch
                }

                // Get AI insights
                val insights = withContext(Dispatchers.IO) {
                    llmClient.analyzeScreenshot(screenshot, prompt)
                }

                if (insights.isEmpty()) {
                    promise?.reject("API_ERROR", "No insights generated")
                    return@launch
                }

                // Show notification
                withContext(Dispatchers.Main) {
                    notificationHelper.showSuggestions(insights)
                }

                // Return result to React Native
                promise?.resolve(Arguments.createMap().apply {
                    putString("status", "success")
                    putArray("insights", Arguments.createArray().apply {
                        insights.forEach { pushString(it) }
                    })
                })

            } catch (e: Exception) {
                Log.e(TAG, "Analysis error: ${e.message}")
                promise?.reject("ERROR", e.message)
            } finally {
                isAnalyzing.set(false)
            }
        }
    }

    // ========== React Native Methods ==========

    @ReactMethod
    fun triggerAnalysis(promise: Promise) {
        performAnalysis(promise = promise)
    }

    @ReactMethod
    fun analyzeScreenWithPrompt(customPrompt: String?, promise: Promise) {
        val prompt = customPrompt ?: "Analyze this screen and provide helpful insights."
        performAnalysis(prompt, promise)
    }

    @ReactMethod
    fun isServiceEnabled(promise: Promise) {
        try {
            val isEnabled = AIAssistAccessibilityService.getInstance() != null
            promise.resolve(isEnabled)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun openAccessibilitySettings(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            reactApplicationContext.startActivity(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun checkNotificationPermission(promise: Promise) {
        try {
            val isGranted = NotificationManagerCompat
                .from(reactApplicationContext)
                .areNotificationsEnabled()
            promise.resolve(isGranted)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun dismissNotification(promise: Promise) {
        try {
            notificationHelper.dismissNotification()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun testBridge(message: String, promise: Promise) {
        promise.resolve("Bridge working! Received: $message")
    }

    override fun getConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "MODULE_NAME" to NAME,
            "REQUIRES_NOTIFICATION_PERMISSION" to (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        )
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        try {
            reactApplicationContext.unregisterReceiver(analyzeReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        moduleScope.cancel()
    }
}
