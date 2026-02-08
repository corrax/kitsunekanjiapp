package com.kitsune.kanji.japanese.flashcards.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kitsune.kanji.japanese.flashcards.data.local.DailySchedulePreferences
import com.kitsune.kanji.japanese.flashcards.domain.time.DailyChallengeClock
import kotlinx.coroutines.runBlocking

object DailyChallengeNotificationScheduler {
    const val actionResetCheck = "com.kitsune.kanji.japanese.flashcards.ACTION_DAILY_RESET_CHECK"
    const val actionEveningCheck = "com.kitsune.kanji.japanese.flashcards.ACTION_DAILY_EVENING_CHECK"

    fun schedule(context: Context) {
        val schedule = runBlocking {
            DailySchedulePreferences(context.applicationContext).getSchedule()
        }
        scheduleAction(
            context = context,
            requestCode = 1001,
            action = actionResetCheck,
            triggerAtMillis = DailyChallengeClock.nextResetNotificationTime(schedule = schedule).toInstant().toEpochMilli()
        )
        scheduleAction(
            context = context,
            requestCode = 1002,
            action = actionEveningCheck,
            triggerAtMillis = DailyChallengeClock.nextEveningReminderTime(schedule = schedule).toInstant().toEpochMilli()
        )
    }

    private fun scheduleAction(
        context: Context,
        requestCode: Int,
        action: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyChallengeNotificationReceiver::class.java).setAction(action)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
