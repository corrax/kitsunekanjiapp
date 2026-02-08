package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.kitsune.kanji.japanese.flashcards.domain.time.DailySchedule
import java.time.LocalTime
import kotlinx.coroutines.flow.first

private val Context.dailyScheduleDataStore by preferencesDataStore(name = "daily_schedule_prefs")

class DailySchedulePreferences(private val context: Context) {
    suspend fun getSchedule(): DailySchedule {
        val defaults = DailySchedule.defaultForLocale()
        val prefs = context.dailyScheduleDataStore.data.first()
        val resetHour = prefs[KEY_RESET_HOUR] ?: defaults.resetTime.hour
        val resetMinute = prefs[KEY_RESET_MINUTE] ?: defaults.resetTime.minute
        val reminderHour = prefs[KEY_REMINDER_HOUR] ?: defaults.reminderTime.hour
        val reminderMinute = prefs[KEY_REMINDER_MINUTE] ?: defaults.reminderTime.minute
        return DailySchedule(
            resetTime = LocalTime.of(resetHour, resetMinute),
            reminderTime = LocalTime.of(reminderHour, reminderMinute)
        )
    }

    suspend fun setSchedule(schedule: DailySchedule) {
        context.dailyScheduleDataStore.edit { prefs ->
            prefs[KEY_RESET_HOUR] = schedule.resetTime.hour
            prefs[KEY_RESET_MINUTE] = schedule.resetTime.minute
            prefs[KEY_REMINDER_HOUR] = schedule.reminderTime.hour
            prefs[KEY_REMINDER_MINUTE] = schedule.reminderTime.minute
        }
    }

    suspend fun resetToLocaleDefaults() {
        setSchedule(DailySchedule.defaultForLocale())
    }

    companion object {
        private val KEY_RESET_HOUR = intPreferencesKey("reset_hour")
        private val KEY_RESET_MINUTE = intPreferencesKey("reset_minute")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }
}
