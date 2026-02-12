package com.kitsune.kanji.japanese.flashcards.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.PackProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.PowerUpInventory
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isStartingDeck: Boolean = false,
    val startingPackId: String? = null,
    val trackTitle: String = "",
    val currentStreak: Int = 0,
    val currentStreakScore: Int = 0,
    val bestStreak: Int = 0,
    val rankSummary: UserRankSummary = UserRankSummary(
        hiddenRating = 1000,
        level = 1,
        title = "Fox Cub",
        wordsCovered = 0,
        totalWords = 0,
        easyWordScore = null,
        hardWordScore = null
    ),
    val hasStartedDailyChallenge: Boolean = false,
    val shouldShowDailyReminder: Boolean = false,
    val powerUps: List<PowerUpInventory> = emptyList(),
    val packs: List<PackProgress> = emptyList(),
    val lifetimeScore: Int = 0,
    val lifetimeCardsReviewed: Int = 0,
    val recentRuns: List<DeckRunHistoryItem> = emptyList(),
    val selectedPackId: String? = null,
    val selectedDeckThemeId: String = deckThemeCatalog.first().id,
    val selectedTrackId: String = "jlpt_n5_core",
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: KitsuneRepository,
    private val deckSelectionPreferences: DeckSelectionPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _openDeckEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openDeckEvents = _openDeckEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val savedTheme = deckSelectionPreferences.getSelectedThemeId(deckThemeCatalog.first().id)
            val savedTrackId = deckSelectionPreferences.getSelectedTrackId(defaultTrackId = "jlpt_n5_core")
            _uiState.update { it.copy(selectedDeckThemeId = savedTheme, selectedTrackId = savedTrackId) }
            loadHome()
        }
    }

    fun startDailyDeck() {
        if (_uiState.value.isStartingDeck || _uiState.value.startingPackId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingDeck = true, errorMessage = null, startingPackId = null) }
            runCatching {
                repository.createOrLoadDailyDeck(trackId = _uiState.value.selectedTrackId)
            }.onSuccess { deck ->
                _openDeckEvents.emit(deck.deckRunId)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to start daily deck.") }
            }
            _uiState.update { it.copy(isStartingDeck = false) }
        }
    }

    fun startExamPack(packId: String) {
        if (_uiState.value.isStartingDeck || _uiState.value.startingPackId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(startingPackId = packId, errorMessage = null) }
            runCatching {
                repository.createOrLoadExamDeck(packId)
            }.onSuccess { deck ->
                _openDeckEvents.emit(deck.deckRunId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to start exam deck.")
                }
            }
            _uiState.update { it.copy(startingPackId = null) }
        }
    }

    fun selectPack(packId: String) {
        _uiState.update { it.copy(selectedPackId = packId) }
    }

    fun dismissDailyReminder() {
        viewModelScope.launch {
            repository.dismissDailyReminder()
            _uiState.update { it.copy(shouldShowDailyReminder = false) }
        }
    }

    fun selectDeckTheme(themeId: String) {
        viewModelScope.launch {
            deckSelectionPreferences.setSelectedThemeId(themeId)
            _uiState.update { it.copy(selectedDeckThemeId = themeId) }
        }
    }

    fun selectDeck(themeId: String, trackId: String?) {
        viewModelScope.launch {
            deckSelectionPreferences.setSelectedThemeId(themeId)
            if (!trackId.isNullOrBlank()) {
                deckSelectionPreferences.setSelectedTrackId(trackId)
            }
            _uiState.update {
                it.copy(
                    selectedDeckThemeId = themeId,
                    selectedTrackId = trackId ?: it.selectedTrackId
                )
            }
            loadHome()
        }
    }

    fun refreshHome() {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.initialize()
                repository.getHomeSnapshot(trackId = _uiState.value.selectedTrackId)
            }.onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trackTitle = snapshot.trackTitle,
                        currentStreak = snapshot.currentStreak,
                        currentStreakScore = snapshot.currentStreakScore,
                        bestStreak = snapshot.bestStreak,
                        rankSummary = snapshot.rankSummary,
                        hasStartedDailyChallenge = snapshot.hasStartedDailyChallenge,
                        shouldShowDailyReminder = snapshot.shouldShowDailyReminder,
                        powerUps = snapshot.powerUps,
                        packs = snapshot.packs,
                        lifetimeScore = snapshot.lifetimeScore,
                        lifetimeCardsReviewed = snapshot.lifetimeCardsReviewed,
                        recentRuns = snapshot.recentRuns,
                        selectedPackId = it.selectedPackId ?: snapshot.packs.firstOrNull()?.packId
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load content."
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
                HomeViewModel(
                    repository = repository,
                    deckSelectionPreferences = deckSelectionPreferences
                )
            }
        }
    }
}
