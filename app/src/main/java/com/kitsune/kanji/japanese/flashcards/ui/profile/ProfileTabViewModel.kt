package com.kitsune.kanji.japanese.flashcards.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.JlptLevelProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileTabViewModel(
    private val repository: KitsuneRepository,
    private val deckSelectionPreferences: DeckSelectionPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileTabUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            runCatching {
                repository.initialize()
                val selectedThemeId = deckSelectionPreferences.getSelectedThemeId("jlpt_n5")
                val trackId = deckSelectionPreferences.getSelectedTrackId("jlpt_n5_core")
                val snapshot = repository.getHomeSnapshot(trackId)
                val recentRuns = repository.getDeckRunHistory(limit = 30)
                val jlptProgress = repository.getJlptLevelProgress()
                ProfilePayload(
                    selectedThemeId = selectedThemeId,
                    lifetimeScore = snapshot.lifetimeScore,
                    lifetimeCardsReviewed = snapshot.lifetimeCardsReviewed,
                    rankSummary = snapshot.rankSummary,
                    recentRuns = recentRuns,
                    jlptProgress = jlptProgress
                )
            }.onSuccess { payload ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedThemeId = payload.selectedThemeId,
                        rankSummary = payload.rankSummary,
                        lifetimeScore = payload.lifetimeScore,
                        lifetimeCardsReviewed = payload.lifetimeCardsReviewed,
                        recentRuns = payload.recentRuns,
                        jlptProgress = payload.jlptProgress,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load Profile."
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: KitsuneRepository,
            deckSelectionPreferences: DeckSelectionPreferences
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileTabViewModel(
                    repository = repository,
                    deckSelectionPreferences = deckSelectionPreferences
                )
            }
        }
    }
}

private data class ProfilePayload(
    val selectedThemeId: String,
    val rankSummary: UserRankSummary,
    val lifetimeScore: Int,
    val lifetimeCardsReviewed: Int,
    val recentRuns: List<DeckRunHistoryItem>,
    val jlptProgress: List<JlptLevelProgress>
)
