package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.deckSelectionDataStore by preferencesDataStore(name = "deck_selection_prefs")

class DeckSelectionPreferences(private val context: Context) {
    suspend fun getSelectedThemeId(defaultThemeId: String): String {
        val prefs = context.deckSelectionDataStore.data.first()
        return prefs[KEY_SELECTED_THEME_ID] ?: defaultThemeId
    }

    suspend fun setSelectedThemeId(themeId: String) {
        context.deckSelectionDataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME_ID] = themeId
        }
    }

    suspend fun getSelectedTrackId(defaultTrackId: String): String {
        val prefs = context.deckSelectionDataStore.data.first()
        return prefs[KEY_SELECTED_TRACK_ID] ?: defaultTrackId
    }

    suspend fun setSelectedTrackId(trackId: String) {
        context.deckSelectionDataStore.edit { prefs ->
            prefs[KEY_SELECTED_TRACK_ID] = trackId
        }
    }

    companion object {
        private val KEY_SELECTED_THEME_ID = stringPreferencesKey("selected_deck_theme_id")
        private val KEY_SELECTED_TRACK_ID = stringPreferencesKey("selected_deck_track_id")
    }
}
