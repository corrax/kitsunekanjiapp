package com.kitsune.kanji.japanese.flashcards.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeckReportUiState(
    val isLoading: Boolean = true,
    val deckRunId: String = "",
    val report: DeckRunReport? = null,
    val errorMessage: String? = null
)

class DeckReportViewModel(
    private val repository: KitsuneRepository
) : ViewModel() {
    private var activeDeckRunId: String? = null

    private val _uiState = MutableStateFlow(DeckReportUiState())
    val uiState = _uiState.asStateFlow()

    fun initialize(deckRunId: String) {
        if (activeDeckRunId == deckRunId && _uiState.value.report != null) {
            return
        }
        activeDeckRunId = deckRunId
        _uiState.update {
            it.copy(
                isLoading = true,
                deckRunId = deckRunId,
                report = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.getDeckRunReport(deckRunId) ?: error("Deck report not found.")
            }.onSuccess { report ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = report,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load deck report."
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: KitsuneRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeckReportViewModel(repository = repository)
            }
        }
    }
}
