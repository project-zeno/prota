package com.aaryannaidu.prota.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.aaryannaidu.prota.R
import com.aaryannaidu.prota.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        
        // Notification channels
        private const val CHANNEL_ID = "chat_suggestions_channel"
        private const val CHANNEL_NAME = "Chat Suggestions"
        private const val CHANNEL_DESC = "Notifications for AI-generated chat reply suggestions"
        
        private const val CONTROL_CHANNEL_ID = "chat_assist_control_channel"
        private const val CONTROL_CHANNEL_NAME = "Chat Assist Control"
        private const val CONTROL_CHANNEL_DESC = "Persistent notification with analyze button"
        
        // Notification IDs
        private const val NOTIFICATION_ID = 1001
        private const val CONTROL_NOTIFICATION_ID = 1000
        
        // Actions
        private const val ACTION_COPY_SUGGESTION = "com.aaryannaidu.prota.COPY_SUGGESTION"
        private const val ACTION_ANALYZE_NOW = "com.aaryannaidu.prota.ANALYZE_NOW"
        private const val EXTRA_SUGGESTION_TEXT = "suggestion_text"
        private const val EXTRA_SUGGESTION_INDEX = "suggestion_index"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channels (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Suggestions channel (high importance)
            val suggestionsChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Control channel (low importance, persistent)
            val controlChannel = NotificationChannel(
                CONTROL_CHANNEL_ID,
                CONTROL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CONTROL_CHANNEL_DESC
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(suggestionsChannel)
            notificationManager.createNotificationChannel(controlChannel)
            Log.d(TAG, "Notification channels created")
        }
    }

    /**
     * Show notification with 3 suggestions and 3 copy buttons
     * 
     * @param suggestions List of exactly 3 suggestion strings
     */
    fun showSuggestions(suggestions: List<String>) {
        if (suggestions.isEmpty()) {
            Log.w(TAG, "No suggestions to show")
            return
        }

        // Ensure we have exactly 3 suggestions (pad if needed)
        val finalSuggestions = suggestions.take(3).let { list ->
            when (list.size) {
                3 -> list
                2 -> list + "Thanks!"
                1 -> list + listOf("Thanks!", "Sounds good!")
                else -> listOf("Thanks!", "Sounds good!", "Sure!")
            }
        }

        Log.d(TAG, "Showing notification with ${finalSuggestions.size} suggestions")

        // Build notification content (expanded view shows all suggestions)
        val notificationText = finalSuggestions.mapIndexed { index, suggestion ->
            "${index + 1}. $suggestion"
        }.joinToString("\n\n")

        // Intent to open the app when notification is tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // App icon
            .setContentTitle("💬 Chat Reply Suggestions")
            .setContentText(finalSuggestions[0]) // First suggestion in compact view
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
                    .setBigContentTitle("💬 Suggested Replies")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true) // Dismiss when tapped
            .setContentIntent(contentPendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250)) // Short vibration pattern
            // Add 3 action buttons for copying each suggestion
            .addAction(
                createCopyAction(finalSuggestions[0], 1, "Copy 1")
            )
            .addAction(
                createCopyAction(finalSuggestions[1], 2, "Copy 2")
            )
            .addAction(
                createCopyAction(finalSuggestions[2], 3, "Copy 3")
            )
            .build()

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification shown successfully")
    }

    /**
     * Create an action (button) for copying a suggestion
     */
    private fun createCopyAction(
        suggestionText: String,
        index: Int,
        buttonLabel: String
    ): NotificationCompat.Action {
        val copyIntent = Intent(context, CopySuggestionReceiver::class.java).apply {
            action = ACTION_COPY_SUGGESTION
            putExtra(EXTRA_SUGGESTION_TEXT, suggestionText)
            putExtra(EXTRA_SUGGESTION_INDEX, index)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            index, // Unique request code for each action
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            0, // No icon needed for action buttons
            buttonLabel,
            pendingIntent
        ).build()
    }

    /**
     * Dismiss the suggestions notification
     */
    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Notification dismissed")
    }

    /**
     * Show persistent control notification with "Analyze Now" button
     * This stays visible while the accessibility service is active
     */
    fun showControlNotification() {
        // Intent to open app when notification is tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Analyze Now" button
        val analyzeIntent = Intent(context, AnalyzeNowReceiver::class.java).apply {
            action = ACTION_ANALYZE_NOW
        }
        val analyzePendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            analyzeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build persistent notification
        val notification = NotificationCompat.Builder(context, CONTROL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💬 Prota Chat Assist")
            .setContentText("Ready to analyze chats")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Makes it persistent (non-dismissible)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                "📊 Analyze Chat Now",
                analyzePendingIntent
            )
            .build()

        notificationManager.notify(CONTROL_NOTIFICATION_ID, notification)
        Log.d(TAG, "Control notification shown")
    }

    /**
     * Dismiss the control notification
     */
    fun dismissControlNotification() {
        notificationManager.cancel(CONTROL_NOTIFICATION_ID)
        Log.d(TAG, "Control notification dismissed")
    }

    /**
     * BroadcastReceiver to handle copy suggestion actions
     * This receives the button clicks from the notification
     */
    class CopySuggestionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_COPY_SUGGESTION) {
                val suggestionText = intent.getStringExtra(EXTRA_SUGGESTION_TEXT)
                val suggestionIndex = intent.getIntExtra(EXTRA_SUGGESTION_INDEX, 0)

                if (!suggestionText.isNullOrEmpty()) {
                    // Copy to clipboard
                    copyToClipboard(context, suggestionText, suggestionIndex)
                    
                    // Show toast feedback
                    Toast.makeText(
                        context,
                        "📋 Suggestion $suggestionIndex copied!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Dismiss the notification after copying
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(NOTIFICATION_ID)

                    Log.d(TAG, "Suggestion $suggestionIndex copied: $suggestionText")
                } else {
                    Log.e(TAG, "Empty suggestion text received")
                }
            }
        }

        /**
         * Copy text to system clipboard
         */
        private fun copyToClipboard(context: Context, text: String, index: Int) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Chat Suggestion $index", text)
            clipboard.setPrimaryClip(clip)
        }
    }

    /**
     * BroadcastReceiver to handle "Analyze Now" button clicks from control notification
     */
    class AnalyzeNowReceiver : BroadcastReceiver() {
        companion object {
            @Volatile
            private var lastClickTime = 0L
            private const val MIN_CLICK_INTERVAL_MS = 2000L // 2 seconds between clicks
        }
        
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_ANALYZE_NOW) {
                // Prevent rapid repeated clicks
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < MIN_CLICK_INTERVAL_MS) {
                    Log.w(TAG, "Analyze Now clicked too soon, ignoring (debouncing)")
                    return
                }
                lastClickTime = currentTime
                
                Log.d(TAG, "Analyze Now button clicked from notification")
                
                // Trigger analysis through the bridge module
                val analysisIntent = Intent(ACTION_ANALYZE_NOW).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(analysisIntent)
            }
        }
    }
}

