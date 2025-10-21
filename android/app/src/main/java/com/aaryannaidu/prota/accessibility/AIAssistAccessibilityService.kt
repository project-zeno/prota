package com.aaryannaidu.prota.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.aaryannaidu.prota.notification.NotificationHelper
import java.io.ByteArrayOutputStream

/**
 * Accessibility service that captures screenshots for AI analysis
 * Works on any app, triggered manually via notification button
 */
class AIAssistAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AIAssistService"
        private var instance: AIAssistAccessibilityService? = null

        fun getInstance(): AIAssistAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")

        // Configure service - works with all apps
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            packageNames = null // Works with ANY app
        }

        // Show control notification with "Analyze Screen" button
        NotificationHelper(this).showControlNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        NotificationHelper(this).dismissControlNotification()
        Log.d(TAG, "Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed - we trigger analysis manually
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    /**
     * Capture screenshot and return as base64 string
     * Uses different methods based on Android version
     */
    fun captureScreenshot(callback: (String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: Use modern screenshot API
            captureScreenshotModern(callback)
        } else {
            // Android 10-: Use fallback (may not work on all devices)
            callback(null)
            Log.e(TAG, "Screenshot requires Android 11+")
        }
    }

    /**
     * Modern screenshot using Android 11+ API
     * How it works:
     * 1. Android gives us a HardwareBuffer (GPU memory)
     * 2. Convert to regular Bitmap (CPU memory)
     * 3. Compress to JPEG (reduce size)
     * 4. Encode to base64 string (for API)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureScreenshotModern(callback: (String?) -> Unit) {
        try {
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        try {
                            // Step 1: Get bitmap from hardware buffer
                            val hwBitmap = Bitmap.wrapHardwareBuffer(
                                result.hardwareBuffer,
                                result.colorSpace
                            )

                            if (hwBitmap == null) {
                                Log.e(TAG, "Failed to create bitmap")
                                result.hardwareBuffer.close()
                                callback(null)
                                return
                            }

                            // Step 2: Convert to software bitmap (required for compression)
                            val bitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            hwBitmap.recycle()
                            result.hardwareBuffer.close()

                            // Step 3: Compress and encode
                            val base64 = bitmapToBase64(bitmap)
                            bitmap.recycle()

                            Log.d(TAG, "Screenshot captured (${base64.length} chars)")
                            callback(base64)

                        } catch (e: Exception) {
                            Log.e(TAG, "Screenshot error: ${e.message}")
                            result.hardwareBuffer.close()
                            callback(null)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Screenshot failed: $errorCode")
                        callback(null)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot error: ${e.message}")
            callback(null)
        }
    }

    /**
     * Convert bitmap to base64 string
     * Compresses to JPEG at 80% quality to reduce size
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
