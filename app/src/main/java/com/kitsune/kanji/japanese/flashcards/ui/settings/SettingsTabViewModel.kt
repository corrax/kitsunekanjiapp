package com.kitsune.kanji.japanese.flashcards.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.DailySchedulePreferences
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import com.kitsune.kanji.japanese.flashcards.data.local.OnboardingPreferences
import com.kitsune.kanji.japanese.flashcards.domain.time.DailySchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsTabViewModel(
    private val dailySchedulePreferences: DailySchedulePreferences,
    private val onboardingPreferences: OnboardingPreferences,
    private val deckSelectionPreferences: DeckSelectionPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsTabUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val schedule = dailySchedulePreferences.getSchedule()
            val learnerLevel = onboardingPreferences.getLearnerLevel()
            val selectedThemeId = deckSelectionPreferences.getSelectedThemeId("jlpt_n5")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedThemeId = selectedThemeId,
                    learnerLevel = learnerLevel,
                    resetTimeText = schedule.resetTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    reminderTimeText = schedule.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                )
            }
        }
    }

    fun updateLearnerLevel(level: LearnerLevel) {
        viewModelScope.launch {
            onboardingPreferences.setLearnerLevel(level)
            _uiState.update { it.copy(learnerLevel = level) }
        }
    }

    fun updateResetTime(text: String) {
        _uiState.update { it.copy(resetTimeText = text, errorMessage = null, savedMessage = null) }
    }

    fun updateReminderTime(text: String) {
        _uiState.update { it.copy(reminderTimeText = text, errorMessage = null, savedMessage = null) }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val resetTime = parseTime(_uiState.value.resetTimeText)
            val reminderTime = parseTime(_uiState.value.reminderTimeText)
            if (resetTime == null || reminderTime == null) {
                _uiState.update { it.copy(errorMessage = "Invalid time format. Use HH:mm.") }
                return@launch
            }
            dailySchedulePreferences.setSchedule(
                DailySchedule(resetTime = resetTime, reminderTime = reminderTime)
            )
            _uiState.update { it.copy(savedMessage = "Settings saved.", errorMessage = null) }
            onSaved()
        }
    }

    fun resetToLocaleDefaults(onSaved: () -> Unit) {
        viewModelScope.launch {
            dailySchedulePreferences.resetToLocaleDefaults()
            val schedule = dailySchedulePreferences.getSchedule()
            _uiState.update {
                it.copy(
                    resetTimeText = schedule.resetTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    reminderTimeText = schedule.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    savedMessage = "Reset to defaults.",
                    errorMessage = null
                )
            }
            onSaved()
        }
    }

    private fun parseTime(text: String): LocalTime? {
        return runCatching {
            LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }

    companion object {
        fun factory(
            dailySchedulePreferences: DailySchedulePreferences,
            onboardingPreferences: OnboardingPreferences,
            deckSelectionPreferences: DeckSelectionPreferences
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsTabViewModel(
                    dailySchedulePreferences = dailySchedulePreferences,
                    onboardingPreferences = onboardingPreferences,
                    deckSelectionPreferences = deckSelectionPreferences
                )
            }
        }
    }
}
