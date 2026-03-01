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

    fun onTopicPillPressed(theme: DeckThemeOption) {
        val trackId = theme.contentTrackId ?: return
        viewModelScope.launch {
            val nextTopicTrackIds = _uiState.value.selectedTopicTrackIds
                .toMutableSet()
                .ifEmpty { mutableSetOf(trackId) }
            if (trackId in nextTopicTrackIds && nextTopicTrackIds.size > 1) {
                nextTopicTrackIds.remove(trackId)
            } else {
                nextTopicTrackIds.add(trackId)
            }

            _uiState.update {
                it.copy(
                    selectedThemeId = theme.id,
                    selectedTopicTrackIds = nextTopicTrackIds,
                    errorMessage = null
                )
            }

            runCatching {
                repository.initialize()
                deckSelectionPreferences.setSelectedTopicTrackIds(nextTopicTrackIds)
                deckSelectionPreferences.setSelectedThemeId(theme.id)

                val focusedTrack = deckSelectionPreferences.getSelectedTrackId("jlpt_n5_core")
                val shouldFocusTrack = trackId in nextTopicTrackIds || focusedTrack !in nextTopicTrackIds
                if (shouldFocusTrack) {
                    val nextTrack = if (trackId in nextTopicTrackIds) trackId else nextTopicTrackIds.first()
                    val nextThemeId = deckThemeCatalog.firstOrNull { it.contentTrackId == nextTrack }?.id
                        ?: theme.id
                    deckSelectionPreferences.setSelectedTrackId(nextTrack)
                    deckSelectionPreferences.setSelectedThemeId(nextThemeId)
                }

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
