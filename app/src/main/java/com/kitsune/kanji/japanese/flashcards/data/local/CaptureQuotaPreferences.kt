package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.captureQuotaDataStore by preferencesDataStore(name = "capture_quota_prefs")

class CaptureQuotaPreferences(private val context: Context) {

    /** UTC week number since epoch (days / 7). Resets every 7 days. */
    private fun currentWeekKey(): Int = (System.currentTimeMillis() / 1000L / 86400L / 7L).toInt()

    val weeklyUsedFlow: Flow<Int> = context.captureQuotaDataStore.data.map { prefs ->
        val storedWeek = prefs[KEY_WEEK] ?: -1
        if (storedWeek != currentWeekKey()) 0 else prefs[KEY_COUNT] ?: 0
    }

    suspend fun getWeeklyUsed(): Int {
        val prefs = context.captureQuotaDataStore.data.first()
        val storedWeek = prefs[KEY_WEEK] ?: -1
        return if (storedWeek != currentWeekKey()) 0 else prefs[KEY_COUNT] ?: 0
    }

    /** Increments the weekly counter and returns the new value. */
    suspend fun incrementWeeklyUsed(): Int {
        val week = currentWeekKey()
        var newCount = 0
        context.captureQuotaDataStore.edit { prefs ->
            val storedWeek = prefs[KEY_WEEK] ?: -1
            val current = if (storedWeek == week) prefs[KEY_COUNT] ?: 0 else 0
            newCount = current + 1
            prefs[KEY_WEEK] = week
            prefs[KEY_COUNT] = newCount
        }
        return newCount
    }

    companion object {
        const val FREE_WEEKLY_LIMIT = 3

        private val KEY_WEEK = intPreferencesKey("capture_week")
        private val KEY_COUNT = intPreferencesKey("capture_count")
    }
}
