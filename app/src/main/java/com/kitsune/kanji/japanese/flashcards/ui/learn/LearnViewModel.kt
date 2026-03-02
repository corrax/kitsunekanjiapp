package com.kitsune.kanji.japanese.flashcards.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.model.ActiveDeckRunProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.PackProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.DeckThemeOption
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnHubUiState(
    val isLoading: Boolean = true,
    val isStartingDeck: Boolean = false,
    val startingPackId: String? = null,
    val currentStreak: Int = 0,
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
    val dailyActiveRun: ActiveDeckRunProgress? = null,
    val packs: List<PackProgress> = emptyList(),
    val availableThemes: List<DeckThemeOption> = deckThemeCatalog,
    val selectedThemeId: String = deckThemeCatalog.firstOrNull()?.id ?: "jlpt_n5",
    val selectedTrackId: String = "jlpt_n5_core",
    val errorMessage: String? = null
)

class LearnViewModel(
    private val repository: KitsuneRepository,
    private val deckSelectionPreferences: DeckSelectionPreferences,
    private val billingPreferences: BillingPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnHubUiState())
    val uiState = _uiState.asStateFlow()

    private val _openDeckEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openDeckEvents = _openDeckEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val defaultThemeId = deckThemeCatalog.firstOrNull()?.id ?: "jlpt_n5"
            val savedTheme = deckSelectionPreferences.getSelectedThemeId(defaultThemeId)
            val savedTrackId = deckSelectionPreferences.getSelectedTrackId("jlpt_n5_core")
            _uiState.update {
                it.copy(selectedThemeId = savedTheme, selectedTrackId = savedTrackId)
            }
            loadHome()
        }
    }

    fun startDailyDeck() {
        if (_uiState.value.isStartingDeck || _uiState.value.startingPackId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingDeck = true, errorMessage = null) }
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
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to start exam deck.") }
            }
            _uiState.update { it.copy(startingPackId = null) }
        }
    }

    fun onThemeSelected(theme: DeckThemeOption) {
        val trackId = theme.contentTrackId ?: _uiState.value.selectedTrackId
        viewModelScope.launch {
            deckSelectionPreferences.setSelectedThemeId(theme.id)
            if (!trackId.isNullOrBlank()) {
                deckSelectionPreferences.setSelectedTrackId(trackId)
            }
            _uiState.update {
                it.copy(selectedThemeId = theme.id, selectedTrackId = trackId)
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
                repository.getHomeSnapshot(trackId = _uiState.value.selectedTrackId)
            }.onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStreak = snapshot.currentStreak,
                        bestStreak = snapshot.bestStreak,
                        rankSummary = snapshot.rankSummary,
                        hasStartedDailyChallenge = snapshot.hasStartedDailyChallenge,
                        dailyActiveRun = snapshot.dailyActiveRun,
                        packs = snapshot.packs
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
            deckSelectionPreferences: DeckSelectionPreferences,
            billingPreferences: BillingPreferences
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LearnViewModel(
                    repository = repository,
                    deckSelectionPreferences = deckSelectionPreferences,
                    billingPreferences = billingPreferences
                )
            }
        }
    }
}
