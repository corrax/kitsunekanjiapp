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
            if (!prefs.contains(KEY_SECOND_CHANCE)) prefs[KEY_SECOND_CHANCE] = 1
            if (!prefs.contains(KEY_HINT_BRUSH)) prefs[KEY_HINT_BRUSH] = 2
            if (!prefs.contains(KEY_REVEAL_RADICAL)) prefs[KEY_REVEAL_RADICAL] = 1
        }
    }

    suspend fun getInventory(): List<PowerUpInventory> {
        val prefs = context.powerUpDataStore.data.first()
        val secondChance = prefs[KEY_SECOND_CHANCE] ?: 0
        val hintBrush = prefs[KEY_HINT_BRUSH] ?: 0
        val revealRadical = prefs[KEY_REVEAL_RADICAL] ?: 0

        return listOf(
            PowerUpInventory(
                id = POWER_UP_SECOND_CHANCE,
                title = "Kitsune Charm",
                count = secondChance,
                description = "Retry one card without score penalty in a deck."
            ),
            PowerUpInventory(
                id = POWER_UP_HINT_BRUSH,
                title = "Fude Hint",
                count = hintBrush,
                description = "Shows a faint guide stroke to help your next attempt."
            ),
            PowerUpInventory(
                id = POWER_UP_REVEAL_RADICAL,
                title = "Radical Lens",
                count = revealRadical,
                description = "Highlights the key radical area for tough kanji."
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
                0 -> prefs[KEY_SECOND_CHANCE] = (prefs[KEY_SECOND_CHANCE] ?: 0) + 1
                1 -> prefs[KEY_HINT_BRUSH] = (prefs[KEY_HINT_BRUSH] ?: 0) + 1
                else -> prefs[KEY_REVEAL_RADICAL] = (prefs[KEY_REVEAL_RADICAL] ?: 0) + 1
            }
            prefs[KEY_LAST_DAILY_REWARD_DATE] = todayIso
        }
    }

    suspend fun consumePowerUp(id: String): Boolean {
        var consumed = false
        context.powerUpDataStore.edit { prefs ->
            val key = when (id) {
                POWER_UP_SECOND_CHANCE -> KEY_SECOND_CHANCE
                POWER_UP_HINT_BRUSH -> KEY_HINT_BRUSH
                POWER_UP_REVEAL_RADICAL -> KEY_REVEAL_RADICAL
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
        const val POWER_UP_SECOND_CHANCE = "second_chance"
        const val POWER_UP_HINT_BRUSH = "hint_brush"
        const val POWER_UP_REVEAL_RADICAL = "reveal_radical"

        private val KEY_SECOND_CHANCE = intPreferencesKey(POWER_UP_SECOND_CHANCE)
        private val KEY_HINT_BRUSH = intPreferencesKey(POWER_UP_HINT_BRUSH)
        private val KEY_REVEAL_RADICAL = intPreferencesKey(POWER_UP_REVEAL_RADICAL)
        private val KEY_LAST_DAILY_REWARD_DATE = stringPreferencesKey("last_daily_reward_date")
        private val KEY_LAST_REMINDER_DISMISSED_DATE = stringPreferencesKey("last_reminder_dismissed_date")
    }
}
