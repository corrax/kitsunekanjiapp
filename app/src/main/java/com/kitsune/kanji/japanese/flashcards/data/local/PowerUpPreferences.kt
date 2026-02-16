package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first

private val Context.powerUpDataStore by preferencesDataStore(name = "powerups_prefs")

class PowerUpPreferences(private val context: Context) {
    suspend fun shouldShowDailyReminder(localDate: LocalDate): Boolean {
        val prefs = context.powerUpDataStore.data.first()
        val dismissedDate = prefs[KEY_LAST_REMINDER_DISMISSED_DATE]
        return dismissedDate != localDate.toString()
    }

    suspend fun markDailyReminderDismissed(localDate: LocalDate) {
        context.powerUpDataStore.edit { prefs ->
            prefs[KEY_LAST_REMINDER_DISMISSED_DATE] = localDate.toString()
        }
    }

    companion object {
        private val KEY_LAST_REMINDER_DISMISSED_DATE = stringPreferencesKey("last_reminder_dismissed_date")
    }
}
