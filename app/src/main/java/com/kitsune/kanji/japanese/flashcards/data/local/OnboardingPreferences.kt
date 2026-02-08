package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

enum class LearnerLevel {
    BEGINNER_N5,
    BEGINNER_PLUS_N4,
    INTERMEDIATE_N3,
    ADVANCED_N2,
    UNSURE
}

class OnboardingPreferences(private val context: Context) {
    suspend fun shouldShowOnboarding(): Boolean {
        return context.userPrefsDataStore.data
            .map { prefs -> !(prefs[KEY_ONBOARDING_COMPLETED] ?: false) }
            .first()
    }

    suspend fun setOnboardingCompleted() {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setLearnerLevel(level: LearnerLevel) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_LEARNER_LEVEL] = level.name
        }
    }

    suspend fun getLearnerLevel(): LearnerLevel {
        val raw = context.userPrefsDataStore.data.map { prefs ->
            prefs[KEY_LEARNER_LEVEL]
        }.first()
        return raw?.let {
            runCatching { LearnerLevel.valueOf(it) }.getOrNull()
        } ?: LearnerLevel.BEGINNER_N5
    }

    suspend fun isPlacementApplied(): Boolean {
        return context.userPrefsDataStore.data
            .map { prefs -> prefs[KEY_PLACEMENT_APPLIED] ?: false }
            .first()
    }

    suspend fun setPlacementApplied(applied: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_PLACEMENT_APPLIED] = applied
        }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_LEARNER_LEVEL = stringPreferencesKey("learner_level")
        private val KEY_PLACEMENT_APPLIED = booleanPreferencesKey("placement_applied")
    }
}
