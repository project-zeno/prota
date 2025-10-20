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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.aaryannaidu.prota.R
import com.aaryannaidu.prota.MainActivity

/**
 * Handles all notifications for the AI assistant
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val ACTION_COPY = "com.aaryannaidu.prota.COPY_SUGGESTION"
        private const val ACTION_ANALYZE = "com.aaryannaidu.prota.ANALYZE_NOW"
        private const val SCREENSHOT_DELAY_MS = 2500L
    }

    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    /**
     * Create notification channels
     */
    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Results channel (high priority)
            NotificationChannel(
                "ai_results",
                "AI Results",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AI analysis results"
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }

            // Control channel (low priority, persistent)
            NotificationChannel(
                "ai_control",
                "AI Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Control notification"
                enableVibration(false)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    /**
     * Show AI insights with copy buttons
     */
    fun showSuggestions(insights: List<String>) {
        if (insights.isEmpty()) return

        // Take first 3, pad if needed
        val items = insights.take(3).toMutableList()
        while (items.size < 3) {
            items.add("...")
        }

        val text = items.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n\n")

        val notification = NotificationCompat.Builder(context, "ai_results")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🤖 AI Insights")
            .setContentText(items[0])
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250))
            .addAction(createCopyAction(items[0], 1))
            .addAction(createCopyAction(items[1], 2))
            .addAction(createCopyAction(items[2], 3))
            .build()

        notificationManager.notify(1001, notification)
    }

    /**
     * Show persistent control notification
     */
    fun showControlNotification() {
        val analyzeIntent = PendingIntent.getBroadcast(
            context,
            100,
            Intent(context, AnalyzeReceiver::class.java).apply { action = ACTION_ANALYZE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "ai_control")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🤖 AI Screen Assistant")
            .setContentText("Ready to analyze")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "📸 Analyze", analyzeIntent)
            .build()

        notificationManager.notify(1000, notification)
    }

    fun dismissNotification() {
        notificationManager.cancel(1001)
    }

    fun dismissControlNotification() {
        notificationManager.cancel(1000)
    }

    private fun createCopyAction(text: String, index: Int): NotificationCompat.Action {
        val intent = PendingIntent.getBroadcast(
            context,
            index,
            Intent(context, CopyReceiver::class.java).apply {
                action = ACTION_COPY
                putExtra("text", text)
                putExtra("index", index)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(0, "Copy $index", intent).build()
    }

    // ========== Broadcast Receivers ==========

    class CopyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val text = intent.getStringExtra("text") ?: return
            val index = intent.getIntExtra("index", 0)

            // Copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("AI Insight", text))

            // Show feedback
            Toast.makeText(context, "📋 Copied!", Toast.LENGTH_SHORT).show()

            // Dismiss notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1001)
        }
    }

    class AnalyzeReceiver : BroadcastReceiver() {
        companion object {
            @Volatile
            private var lastClick = 0L
        }

        override fun onReceive(context: Context, intent: Intent) {
            // Debounce
            val now = System.currentTimeMillis()
            if (now - lastClick < 5000) {
                Toast.makeText(context, "⏳ Please wait...", Toast.LENGTH_SHORT).show()
                return
            }
            lastClick = now

            // Collapse notification panel
            try {
                context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to collapse panel: ${e.message}")
            }

            // Show countdown
            Toast.makeText(
                context,
                "📸 Analyzing in ${SCREENSHOT_DELAY_MS / 1000} seconds...",
                Toast.LENGTH_SHORT
            ).show()

            // Wait then trigger
            Handler(Looper.getMainLooper()).postDelayed({
                context.sendBroadcast(Intent(ACTION_ANALYZE).apply {
                    setPackage(context.packageName)
                })
            }, SCREENSHOT_DELAY_MS)
        }
    }
}
