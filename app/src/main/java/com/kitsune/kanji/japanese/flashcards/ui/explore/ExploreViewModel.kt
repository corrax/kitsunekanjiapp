package com.kitsune.kanji.japanese.flashcards.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.DeckThemeOption
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val repository: KitsuneRepository,
    private val deckSelectionPreferences: DeckSelectionPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState = _uiState.asStateFlow()

    private val _openDeckEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openDeckEvents = _openDeckEvents.asSharedFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                repository.initialize()
                val selectedTrackId = deckSelectionPreferences.getSelectedTrackId("jlpt_n5_core")
                val topicTrackIds = deckSelectionPreferences.getSelectedTopicTrackIds()
                    .ifEmpty {
                        setOf(selectedTrackId).also { defaults ->
                            deckSelectionPreferences.setSelectedTopicTrackIds(defaults)
                        }
                    }
                val savedThemeId = deckSelectionPreferences.getSelectedThemeId(deckThemeCatalog.first().id)
                val selectedTheme = deckThemeCatalog.firstOrNull { it.id == savedThemeId }
                    ?: deckThemeCatalog.firstOrNull { it.contentTrackId == selectedTrackId }
                    ?: deckThemeCatalog.first()
                val previewTrackId = selectedTheme.contentTrackId ?: selectedTrackId
                val snapshot = repository.getHomeSnapshot(previewTrackId)
                Triple(selectedTheme.id, topicTrackIds, snapshot.packs)
            }.onSuccess { (themeId, topicTrackIds, packs) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedThemeId = themeId,
                        selectedTopicTrackIds = topicTrackIds,
                        packs = packs,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load Explore.")
                }
            }
        }
    }

    fun onTopicSelected(theme: DeckThemeOption) {
        val trackId = theme.contentTrackId ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedThemeId = theme.id,
                    errorMessage = null
                )
            }

            runCatching {
                repository.initialize()
                // Update selected theme ID preference so the UI state persists,
                // but do NOT modify the selected topic tracks.
                deckSelectionPreferences.setSelectedThemeId(theme.id)

                val snapshot = repository.getHomeSnapshot(trackId)
                snapshot.packs
            }.onSuccess { packs ->
                _uiState.update {
                    it.copy(
                        selectedThemeId = theme.id,
                        packs = packs,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to load topic.")
                }
            }
        }
    }

    fun onTopicSelectionToggled(theme: DeckThemeOption) {
        val trackId = theme.contentTrackId ?: return
        viewModelScope.launch {
            val nextTopicTrackIds = _uiState.value.selectedTopicTrackIds
                .toMutableSet()
                .ifEmpty { mutableSetOf(trackId) }

            if (trackId in nextTopicTrackIds && nextTopicTrackIds.size > 1) {
                nextTopicTrackIds.remove(trackId)
            } else if (trackId !in nextTopicTrackIds) {
                nextTopicTrackIds.add(trackId)
            } else {
                 // Cannot toggle off the last remaining topic
                 return@launch
            }

            _uiState.update {
                it.copy(
                    selectedTopicTrackIds = nextTopicTrackIds,
                    errorMessage = null
                )
            }

            runCatching {
                repository.initialize()
                deckSelectionPreferences.setSelectedTopicTrackIds(nextTopicTrackIds)

                // If we just selected a track that wasn't the focused one, we might want to ensure
                // the main deck logic knows about it.
                // However, for Explore screen purposes, we just update the set.
                // We keep the currently viewed theme selected.

                // If the user unselected the currently viewed theme, we don't necessarily need to change the view,
                // as they might just want to remove it from the daily deck but still look at it.
                // So we do nothing else here regarding theme selection.

                // We might want to update the "selected track" for the home tab if the current one was removed?
                val focusedTrack = deckSelectionPreferences.getSelectedTrackId("jlpt_n5_core")
                if (focusedTrack !in nextTopicTrackIds) {
                    val nextTrack = nextTopicTrackIds.first()
                    deckSelectionPreferences.setSelectedTrackId(nextTrack)
                }

            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to save topic preference.")
                }
            }
        }
    }

    fun startExamPack(packId: String) {
        viewModelScope.launch {
            runCatching {
                repository.createOrLoadExamDeck(packId)
            }.onSuccess { deck ->
                _openDeckEvents.emit(deck.deckRunId)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    companion object {
        fun factory(
            repository: KitsuneRepository,
            deckSelectionPreferences: DeckSelectionPreferences
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ExploreViewModel(
                    repository = repository,
                    deckSelectionPreferences = deckSelectionPreferences
                )
            }
        }
    }
}
