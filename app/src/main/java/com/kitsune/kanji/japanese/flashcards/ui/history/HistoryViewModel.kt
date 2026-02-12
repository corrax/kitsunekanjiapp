package com.kitsune.kanji.japanese.flashcards.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.KanjiAttemptHistoryItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HistoryTab {
    CARDS,
    REPORTS
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val selectedTab: HistoryTab = HistoryTab.CARDS,
    val attempts: List<KanjiAttemptHistoryItem> = emptyList(),
    val reports: List<DeckRunHistoryItem> = emptyList(),
    val retestAttemptIdInProgress: String? = null,
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val repository: KitsuneRepository
) : ViewModel() {
    private var hasLoaded = false

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()
    private val _openDeckEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openDeckEvents = _openDeckEvents.asSharedFlow()

    fun load() {
        if (hasLoaded) return
        hasLoaded = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val attempts = repository.getKanjiAttemptHistory(limit = 250)
                val reports = repository.getDeckRunHistory(limit = 250)
                attempts to reports
            }.onSuccess { (attempts, reports) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        attempts = attempts,
                        reports = reports,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        attempts = emptyList(),
                        reports = emptyList(),
                        errorMessage = error.message ?: "Failed to load card history."
                    )
                }
            }
        }
    }

    fun selectTab(tab: HistoryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun startRetest(attemptId: String) {
        if (_uiState.value.retestAttemptIdInProgress != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(retestAttemptIdInProgress = attemptId, errorMessage = null) }
            runCatching {
                repository.createRetestDeckForAttempt(attemptId)
            }.onSuccess { deck ->
                _openDeckEvents.emit(deck.deckRunId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to create retest deck.")
                }
            }
            _uiState.update { it.copy(retestAttemptIdInProgress = null) }
        }
    }

    companion object {
        fun factory(repository: KitsuneRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(repository)
            }
        }
    }
}
