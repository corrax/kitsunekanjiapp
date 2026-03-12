package com.kitsune.kanji.japanese.flashcards.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.CaptureQuotaPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.OnboardingPreferences
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
import kotlinx.coroutines.flow.combine
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
    val packsCache: Map<String, List<PackProgress>> = emptyMap(),
    val availableThemes: List<DeckThemeOption> = deckThemeCatalog,
    val selectedThemeId: String = deckThemeCatalog.firstOrNull()?.id ?: "jlpt_n5",
    val selectedTrackId: String = "jlpt_n5_core",
    val errorMessage: String? = null,
    /** True when free capture quota is exceeded; open paywall instead of capture when user taps capture. */
    val captureNeedsUpgrade: Boolean = false,
    val showFirstCapturePrompt: Boolean = false,
    /** Number of captured words queued for the next daily deck. */
    val pendingCapturedCount: Int = 0
)

class LearnViewModel(
    private val repository: KitsuneRepository,
    private val deckSelectionPreferences: DeckSelectionPreferences,
    private val billingPreferences: BillingPreferences,
    private val captureQuotaPreferences: CaptureQuotaPreferences,
    private val onboardingPreferences: OnboardingPreferences
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
            // loadHome() is triggered when Learn screen is composed (see KitsuneRoot) so profile stays in sync with Profile tab
        }
        viewModelScope.launch {
            if (onboardingPreferences.shouldShowFirstCapturePrompt()) {
                _uiState.update { it.copy(showFirstCapturePrompt = true) }
            }
        }
        viewModelScope.launch {
            combine(
                billingPreferences.entitlementFlow,
                captureQuotaPreferences.weeklyUsedFlow
            ) { entitlement, used ->
                !entitlement.isPlusEntitled && used >= CaptureQuotaPreferences.FREE_WEEKLY_LIMIT
            }.collect { needsUpgrade ->
                _uiState.update { it.copy(captureNeedsUpgrade = needsUpgrade) }
            }
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
            // Show cached packs immediately (if available) — no rank/daily reload on swipe
            _uiState.update {
                val cached = it.packsCache[theme.id]
                it.copy(
                    selectedThemeId = theme.id,
                    selectedTrackId = trackId,
                    packs = cached ?: it.packs
                )
            }
            // Only fetch packs for this theme if not already cached
            if (_uiState.value.packsCache[theme.id] == null) {
                loadPacksForTheme(trackId, theme.id)
            }
        }
    }

    fun dismissFirstCapturePrompt() {
        _uiState.update { it.copy(showFirstCapturePrompt = false) }
        viewModelScope.launch {
            onboardingPreferences.setFirstCapturePromptDismissed()
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
                        packs = snapshot.packs,
                        packsCache = it.packsCache + (it.selectedThemeId to snapshot.packs)
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
            // Load pending captured word count
            runCatching { repository.getDailyCapturedCards() }
                .onSuccess { cards ->
                    _uiState.update { it.copy(pendingCapturedCount = cards.size) }
                }
        }
    }

    private fun loadPacksForTheme(trackId: String, themeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getHomeSnapshot(trackId)
            }.onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packs = snapshot.packs,
                        packsCache = it.packsCache + (themeId to snapshot.packs)
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load content.")
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: KitsuneRepository,
            deckSelectionPreferences: DeckSelectionPreferences,
            billingPreferences: BillingPreferences,
            captureQuotaPreferences: CaptureQuotaPreferences,
            onboardingPreferences: OnboardingPreferences
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LearnViewModel(
                    repository = repository,
                    deckSelectionPreferences = deckSelectionPreferences,
                    billingPreferences = billingPreferences,
                    captureQuotaPreferences = captureQuotaPreferences,
                    onboardingPreferences = onboardingPreferences
                )
            }
        }
    }
}
