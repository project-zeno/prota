package com.aaryannaidu.prota.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aaryannaidu.prota.notification.NotificationHelper

class ChatAssistAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ChatAssistA11yService"
        private var instance: ChatAssistAccessibilityService? = null

        fun getInstance(): ChatAssistAccessibilityService? {
            return instance
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service Connected")

        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            // Event types: We only care about window changes since analysis is manual
            // Reduced to just WINDOW_STATE_CHANGED to minimize event processing
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            
            // Package names to observe (WhatsApp only)
            packageNames = arrayOf("com.whatsapp")
            
            // Feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            
            // Flags
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            
            // Notification timeout (increased to reduce frequency)
            // Since we only do manual analysis, we don't need frequent events
            notificationTimeout = 500
        }
        
        setServiceInfo(info)
        
        // Show persistent control notification
        val notificationHelper = NotificationHelper(this)
        notificationHelper.showControlNotification()
        Log.d(TAG, "Control notification shown")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle events automatically
        // All functionality is triggered manually via button press
        // This method is required but can be empty for our use case
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        
        // Dismiss control notification when service is disabled
        val notificationHelper = NotificationHelper(this)
        notificationHelper.dismissControlNotification()
        
        Log.d(TAG, "Accessibility Service Destroyed")
    }

    /**
     * Helper method to get chat messages specifically
     * This is a more refined version that tries to extract just the chat messages
     */
    fun readChatMessages(): String {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "Unable to get root window")
            return ""
        }

        val messages = mutableListOf<String>()
        extractChatMessages(rootNode, messages)
        rootNode.recycle()

        // Join messages with newlines, limiting to last ~10 messages
        val result = messages.takeLast(10).joinToString("\n")
        Log.d(TAG, "Extracted ${messages.size} messages, using last ${messages.takeLast(10).size}")
        
        return result
    }

    /**
     * Try to extract chat messages specifically (heuristic-based)
     * WhatsApp chat messages typically have certain view IDs or patterns
     */
    private fun extractChatMessages(node: AccessibilityNodeInfo, messages: MutableList<String>) {
        // Check if this node looks like a message (heuristic-based)
        val text = node.text?.toString()

        // WhatsApp message bubbles often have specific view IDs
        // Common patterns: "message_text", "conversation_row", etc.
        if (!text.isNullOrEmpty()) {
            // Filter out UI elements like timestamps, status text, etc.
            if (text.length > 1 && !isUIElement(text)) {
                messages.add(text)
            }
        }

        // Recursively process children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractChatMessages(child, messages)
                child.recycle()
            }
        }
    }

    /**
     * Heuristic to filter out UI elements
     */
    private fun isUIElement(text: String): Boolean {
        // Skip common UI patterns (timestamps, status messages, etc.)
        val uiPatterns = listOf(
            "^\\d{1,2}:\\d{2}$", // Time format (12:34)
            "^\\d{1,2}:\\d{2} [AP]M$", // Time format with AM/PM
            "Today", "Yesterday",
            "Type a message",
            "Tap to view",
            "Photo", "Video", "Audio", "Document"
        )
        
        return uiPatterns.any { pattern ->
            text.matches(Regex(pattern))
        }
    }
}

