package com.kitsune.kanji.japanese.flashcards.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kitsune.kanji.japanese.flashcards.KitsuneApp
import com.kitsune.kanji.japanese.flashcards.MainActivity
import com.kitsune.kanji.japanese.flashcards.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyChallengeNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                DailyChallengeNotificationScheduler.schedule(context)
            }

            DailyChallengeNotificationScheduler.actionResetCheck,
            DailyChallengeNotificationScheduler.actionEveningCheck -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as KitsuneApp
                        app.appContainer.repository.initialize()
                        val snapshot = app.appContainer.repository.getHomeSnapshot()
                        if (snapshot.shouldShowDailyReminder) {
                            maybeNotify(context, intent.action ?: "")
                        }
                    } finally {
                        DailyChallengeNotificationScheduler.schedule(context)
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun maybeNotify(context: Context, action: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            2001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val body: String
        val notificationId: Int
        if (action == DailyChallengeNotificationScheduler.actionResetCheck) {
            title = "Daily Challenge just reset"
            body = "Your new 15-card deck is ready. Keep your streak alive."
            notificationId = 3011
        } else {
            title = "Evening study reminder"
            body = "Quick run now helps lock today’s kanji before tomorrow."
            notificationId = 3012
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) return
        val channel = NotificationChannel(
            channelId,
            "Daily Challenge",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily deck reset and reminder alerts."
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val channelId = "daily_challenge_channel"
    }
}
