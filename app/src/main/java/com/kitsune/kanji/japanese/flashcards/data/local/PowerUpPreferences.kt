package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.kitsune.kanji.japanese.flashcards.domain.model.PowerUpInventory
import java.time.LocalDate
import kotlinx.coroutines.flow.first

private val Context.powerUpDataStore by preferencesDataStore(name = "powerups_prefs")

class PowerUpPreferences(private val context: Context) {
    suspend fun ensureStarterBundle() {
        context.powerUpDataStore.edit { prefs ->
            if (!prefs.contains(KEY_LUCKY_COIN)) prefs[KEY_LUCKY_COIN] = 1
            if (!prefs.contains(KEY_HINT_BRUSH)) prefs[KEY_HINT_BRUSH] = 2
            if (!prefs.contains(KEY_INSIGHT_LENS)) prefs[KEY_INSIGHT_LENS] = 1
        }
    }

    suspend fun getInventory(): List<PowerUpInventory> {
        val prefs = context.powerUpDataStore.data.first()
        val luckyCoin = prefs[KEY_LUCKY_COIN] ?: 0
        val hintBrush = prefs[KEY_HINT_BRUSH] ?: 0
        val insightLens = prefs[KEY_INSIGHT_LENS] ?: 0

        return listOf(
            PowerUpInventory(
                id = POWER_UP_LUCKY_COIN,
                title = "Lucky Coin",
                count = luckyCoin,
                description = "Reroll the current card once to test a different challenge."
            ),
            PowerUpInventory(
                id = POWER_UP_HINT_BRUSH,
                title = "Fude Hint",
                count = hintBrush,
                description = "Reveal a starter hint for the current answer."
            ),
            PowerUpInventory(
                id = POWER_UP_INSIGHT_LENS,
                title = "Insight Lens",
                count = insightLens,
                description = "Remove one wrong option or show extra context on text cards."
            )
        )
    }

    suspend fun awardDailyReward(localDate: LocalDate) {
        val todayIso = localDate.toString()
        context.powerUpDataStore.edit { prefs ->
            if (prefs[KEY_LAST_DAILY_REWARD_DATE] == todayIso) {
                return@edit
            }

            val selector = localDate.dayOfYear % 3
            when (selector) {
                0 -> prefs[KEY_LUCKY_COIN] = (prefs[KEY_LUCKY_COIN] ?: 0) + 1
                1 -> prefs[KEY_HINT_BRUSH] = (prefs[KEY_HINT_BRUSH] ?: 0) + 1
                else -> prefs[KEY_INSIGHT_LENS] = (prefs[KEY_INSIGHT_LENS] ?: 0) + 1
            }
            prefs[KEY_LAST_DAILY_REWARD_DATE] = todayIso
        }
    }

    suspend fun consumePowerUp(id: String): Boolean {
        var consumed = false
        context.powerUpDataStore.edit { prefs ->
            val key = when (id) {
                POWER_UP_LUCKY_COIN -> KEY_LUCKY_COIN
                POWER_UP_HINT_BRUSH -> KEY_HINT_BRUSH
                POWER_UP_INSIGHT_LENS -> KEY_INSIGHT_LENS
                else -> null
            } ?: return@edit
            val current = prefs[key] ?: 0
            if (current > 0) {
                prefs[key] = current - 1
                consumed = true
            }
        }
        return consumed
    }

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
        const val POWER_UP_LUCKY_COIN = "lucky_coin"
        const val POWER_UP_HINT_BRUSH = "hint_brush"
        const val POWER_UP_INSIGHT_LENS = "insight_lens"

        private val KEY_LUCKY_COIN = intPreferencesKey(POWER_UP_LUCKY_COIN)
        private val KEY_HINT_BRUSH = intPreferencesKey(POWER_UP_HINT_BRUSH)
        private val KEY_INSIGHT_LENS = intPreferencesKey(POWER_UP_INSIGHT_LENS)
        private val KEY_LAST_DAILY_REWARD_DATE = stringPreferencesKey("last_daily_reward_date")
        private val KEY_LAST_REMINDER_DISMISSED_DATE = stringPreferencesKey("last_reminder_dismissed_date")
    }
}
