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
import com.aaryannaidu.prota.accessibility.ChatAssistAccessibilityService
import com.aaryannaidu.prota.api.LlmApiClient
import com.aaryannaidu.prota.notification.NotificationHelper
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * React Native Bridge Module for Accessibility Chat Assist
 * 
 * Exposes methods to React Native for:
 * - Triggering chat analysis (main flow)
 * - Checking accessibility service status
 * - Opening accessibility settings
 * - Checking notification permissions
 */
@ReactModule(name = AccessibilityBridgeModule.NAME)
class AccessibilityBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "AccessibilityBridge"
        private const val TAG = "AccessibilityBridge"
        private const val ACTION_ANALYZE_NOW = "com.aaryannaidu.prota.ANALYZE_NOW"
    }

    private val llmClient = LlmApiClient()
    private val notificationHelper = NotificationHelper(reactContext)
    
    // Coroutine scope for async operations
    private val moduleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Thread-safe guard to prevent concurrent analyses
    private val isAnalyzing = AtomicBoolean(false)
    
    // Timestamp of last analysis to implement debouncing
    @Volatile
    private var lastAnalysisTime = 0L
    
    // Minimum time between analyses (3 seconds)
    private val MIN_ANALYSIS_INTERVAL_MS = 3000L

    // BroadcastReceiver for analyze now action
    private val analyzeNowReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ANALYZE_NOW) {
                Log.d(TAG, "Received ANALYZE_NOW broadcast, triggering analysis")
                triggerAnalysisInternal()
            }
        }
    }

    init {
        // Register receiver for analyze now action
        val filter = IntentFilter(ACTION_ANALYZE_NOW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reactApplicationContext.registerReceiver(analyzeNowReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            reactApplicationContext.registerReceiver(analyzeNowReceiver, filter)
        }
        Log.d(TAG, "Registered ANALYZE_NOW broadcast receiver")
    }

    override fun getName(): String = NAME

    /**
     * Internal method to trigger analysis (can be called from broadcast or React Native)
     */
    private fun triggerAnalysisInternal() {
        // Check if already analyzing (thread-safe)
        if (!isAnalyzing.compareAndSet(false, true)) {
            Log.w(TAG, "Analysis already in progress, ignoring request")
            return
        }
        
        // Check debounce interval
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAnalysis = currentTime - lastAnalysisTime
        if (timeSinceLastAnalysis < MIN_ANALYSIS_INTERVAL_MS) {
            Log.w(TAG, "Analysis called too soon (${timeSinceLastAnalysis}ms ago), please wait ${MIN_ANALYSIS_INTERVAL_MS - timeSinceLastAnalysis}ms")
            isAnalyzing.set(false) // Reset flag before returning
            return
        }
        
        // Update timestamp
        lastAnalysisTime = currentTime
        
        moduleScope.launch {
            try {
                Log.d(TAG, "Starting analysis flow...")

                // Check if accessibility service is running
                val service = ChatAssistAccessibilityService.getInstance()
                if (service == null) {
                    Log.e(TAG, "Accessibility service not enabled")
                    return@launch
                }

                // Step 1: Read messages from screen
                val messages = withContext(Dispatchers.Main) {
                    service.readChatMessages()
                }

                if (messages.isEmpty()) {
                    Log.w(TAG, "No messages found on screen")
                    return@launch
                }

                Log.d(TAG, "Extracted ${messages.length} characters of chat text")

                // Step 2: Call Gemini API for suggestions
                val suggestions = withContext(Dispatchers.IO) {
                    llmClient.getSuggestions(messages)
                }

                if (suggestions.isEmpty()) {
                    Log.e(TAG, "API returned no suggestions")
                    return@launch
                }

                Log.d(TAG, "Received ${suggestions.size} suggestions from API")

                // Step 3: Show notification with suggestions
                withContext(Dispatchers.Main) {
                    notificationHelper.showSuggestions(suggestions)
                }

                Log.d(TAG, "Analysis complete - notification shown")

            } catch (e: Exception) {
                Log.e(TAG, "Error in analysis flow: ${e.message}", e)
            } finally {
                // Always reset the flag when done (thread-safe)
                isAnalyzing.set(false)
            }
        }
    }

    /**
     * Main method: Trigger the complete analysis flow
     * 
     * Flow:
     * 1. Check if accessibility service is enabled
     * 2. Read messages from WhatsApp screen
     * 3. Send to Gemini API for suggestions
     * 4. Show notification with suggestions
     * 
     * @param promise Returns success/error to React Native
     */
    @ReactMethod
    fun triggerAnalysis(promise: Promise) {
        Log.d(TAG, "triggerAnalysis() called from React Native")

        try {
            // Check if already analyzing (thread-safe)
            if (!isAnalyzing.compareAndSet(false, true)) {
                Log.w(TAG, "Analysis already in progress")
                promise.reject(
                    "ANALYSIS_IN_PROGRESS",
                    "Analysis is already running. Please wait for it to complete."
                )
                return
            }
            
            // Check debounce interval
            val currentTime = System.currentTimeMillis()
            val timeSinceLastAnalysis = currentTime - lastAnalysisTime
            if (timeSinceLastAnalysis < MIN_ANALYSIS_INTERVAL_MS) {
                val waitTime = MIN_ANALYSIS_INTERVAL_MS - timeSinceLastAnalysis
                Log.w(TAG, "Analysis called too soon, must wait ${waitTime}ms")
                isAnalyzing.set(false) // Reset flag before returning
                promise.reject(
                    "RATE_LIMIT",
                    "Please wait ${waitTime / 1000} seconds before analyzing again."
                )
                return
            }
            
            // Check if accessibility service is running
            val service = ChatAssistAccessibilityService.getInstance()
            if (service == null) {
                Log.e(TAG, "Accessibility service not enabled")
                isAnalyzing.set(false) // Reset flag before returning
                promise.reject(
                    "SERVICE_NOT_ENABLED",
                    "Accessibility service is not enabled. Please enable it in Settings."
                )
                return
            }

            // Check notification permission
            if (!isNotificationPermissionGranted()) {
                Log.e(TAG, "Notification permission not granted")
                isAnalyzing.set(false) // Reset flag before returning
                promise.reject(
                    "NOTIFICATION_PERMISSION_DENIED",
                    "Notification permission is required. Please grant it in Settings."
                )
                return
            }

            // Update timestamp
            lastAnalysisTime = currentTime
            
            // Run the analysis flow asynchronously
            moduleScope.launch {
                try {
                    Log.d(TAG, "Starting analysis flow...")

                    // Step 1: Read messages from screen (on main thread - accessibility API requirement)
                    val messages = withContext(Dispatchers.Main) {
                        service.readChatMessages()
                    }

                    if (messages.isEmpty()) {
                        Log.w(TAG, "No messages found on screen")
                        promise.reject(
                            "NO_MESSAGES",
                            "No chat messages found. Please open a WhatsApp chat and try again."
                        )
                        return@launch
                    }

                    Log.d(TAG, "Extracted ${messages.length} characters of chat text")

                    // Step 2: Call Gemini API for suggestions (on IO thread - network call)
                    val suggestions = withContext(Dispatchers.IO) {
                        llmClient.getSuggestions(messages)
                    }

                    if (suggestions.isEmpty()) {
                        Log.e(TAG, "API returned no suggestions")
                        promise.reject(
                            "API_ERROR",
                            "Failed to generate suggestions. Please try again."
                        )
                        return@launch
                    }

                    Log.d(TAG, "Received ${suggestions.size} suggestions from API")

                    // Step 3: Show notification with suggestions (on main thread)
                    withContext(Dispatchers.Main) {
                        notificationHelper.showSuggestions(suggestions)
                    }

                    Log.d(TAG, "Analysis complete - notification shown")

                    // Return success with suggestions
                    val result = Arguments.createMap().apply {
                        putString("status", "success")
                        putInt("suggestionCount", suggestions.size)
                        putArray("suggestions", Arguments.createArray().apply {
                            suggestions.forEach { pushString(it) }
                        })
                    }
                    promise.resolve(result)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in analysis flow: ${e.message}", e)
                    promise.reject(
                        "ANALYSIS_ERROR",
                        "Error during analysis: ${e.message}",
                        e
                    )
                } finally {
                    // Always reset the flag when done (thread-safe)
                    isAnalyzing.set(false)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            // Reset flag on error before returning (thread-safe)
            isAnalyzing.set(false)
            promise.reject(
                "UNEXPECTED_ERROR",
                "Unexpected error: ${e.message}",
                e
            )
        }
    }

    /**
     * Check if accessibility service is currently enabled and running
     * 
     * @param promise Returns true/false
     */
    @ReactMethod
    fun isServiceEnabled(promise: Promise) {
        try {
            val service = ChatAssistAccessibilityService.getInstance()
            val isEnabled = service != null
            Log.d(TAG, "Accessibility service enabled: $isEnabled")
            promise.resolve(isEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service status: ${e.message}", e)
            promise.reject("CHECK_ERROR", "Error checking service status", e)
        }
    }

    /**
     * Open Android Accessibility Settings page
     * User can enable the service from there
     * 
     * @param promise Returns success/error
     */
    @ReactMethod
    fun openAccessibilitySettings(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            reactApplicationContext.startActivity(intent)
            Log.d(TAG, "Opened accessibility settings")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings: ${e.message}", e)
            promise.reject("SETTINGS_ERROR", "Failed to open settings", e)
        }
    }

    /**
     * Check if notification permission is granted
     * 
     * @param promise Returns true/false
     */
    @ReactMethod
    fun checkNotificationPermission(promise: Promise) {
        try {
            val isGranted = isNotificationPermissionGranted()
            Log.d(TAG, "Notification permission granted: $isGranted")
            promise.resolve(isGranted)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification permission: ${e.message}", e)
            promise.reject("PERMISSION_CHECK_ERROR", "Error checking permission", e)
        }
    }

    /**
     * Request notification permission (Android 13+)
     * For older versions, this always returns true
     * 
     * Note: The actual permission request must be handled in MainActivity
     * This method just checks if we need to request it
     * 
     * @param promise Returns true if permission is granted or not needed
     */
    @ReactMethod
    fun shouldRequestNotificationPermission(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires runtime permission
                val isGranted = isNotificationPermissionGranted()
                promise.resolve(!isGranted) // Return true if we SHOULD request
            } else {
                // Older Android versions don't need runtime permission
                promise.resolve(false) // No need to request
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification permission: ${e.message}", e)
            promise.reject("PERMISSION_ERROR", "Error checking permission", e)
        }
    }

    /**
     * Open app notification settings
     * 
     * @param promise Returns success/error
     */
    @ReactMethod
    fun openNotificationSettings(promise: Promise) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, reactApplicationContext.packageName)
                } else {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.parse("package:${reactApplicationContext.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            reactApplicationContext.startActivity(intent)
            Log.d(TAG, "Opened notification settings")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification settings: ${e.message}", e)
            promise.reject("SETTINGS_ERROR", "Failed to open settings", e)
        }
    }

    /**
     * Dismiss any active suggestion notifications
     * 
     * @param promise Returns success
     */
    @ReactMethod
    fun dismissNotification(promise: Promise) {
        try {
            notificationHelper.dismissNotification()
            Log.d(TAG, "Notification dismissed")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification: ${e.message}", e)
            promise.reject("DISMISS_ERROR", "Failed to dismiss notification", e)
        }
    }

    /**
     * Test method to verify bridge connection
     * 
     * @param message Test message from React Native
     * @param promise Returns echo response
     */
    @ReactMethod
    fun testBridge(message: String, promise: Promise) {
        try {
            val response = "Bridge working! Received: $message"
            Log.d(TAG, response)
            promise.resolve(response)
        } catch (e: Exception) {
            promise.reject("TEST_ERROR", "Test failed", e)
        }
    }

    /**
     * Helper: Check if notification permission is granted
     */
    private fun isNotificationPermissionGranted(): Boolean {
        return NotificationManagerCompat.from(reactApplicationContext)
            .areNotificationsEnabled()
    }

    /**
     * Export constants to React Native
     * These can be accessed via NativeModules.AccessibilityBridge.XXX
     */
    override fun getConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "MODULE_NAME" to NAME,
            "REQUIRES_NOTIFICATION_PERMISSION" to (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        )
    }

    /**
     * Clean up when module is destroyed
     */
    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        
        // Unregister broadcast receiver
        try {
            reactApplicationContext.unregisterReceiver(analyzeNowReceiver)
            Log.d(TAG, "Unregistered ANALYZE_NOW broadcast receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        
        moduleScope.cancel() // Cancel all pending coroutines
        Log.d(TAG, "Module destroyed, coroutines cancelled")
    }
}

