package com.kitsune.kanji.japanese.flashcards.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.DailySchedulePreferences
import com.kitsune.kanji.japanese.flashcards.domain.time.DailySchedule
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val resetTimeText: String = "",
    val reminderTimeText: String = "",
    val errorMessage: String? = null,
    val savedMessage: String? = null
)

class SettingsViewModel(
    private val dailySchedulePreferences: DailySchedulePreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun updateResetTime(text: String) {
        _uiState.update {
            it.copy(resetTimeText = text, errorMessage = null, savedMessage = null)
        }
    }

    fun updateReminderTime(text: String) {
        _uiState.update {
            it.copy(reminderTimeText = text, errorMessage = null, savedMessage = null)
        }
    }

    fun save(onSaved: () -> Unit) {
        val reset = parseTime(_uiState.value.resetTimeText)
        val reminder = parseTime(_uiState.value.reminderTimeText)
        if (reset == null || reminder == null) {
            _uiState.update {
                it.copy(errorMessage = "Use 24-hour format HH:mm, e.g. 18:00.")
            }
            return
        }
        viewModelScope.launch {
            dailySchedulePreferences.setSchedule(
                DailySchedule(
                    resetTime = reset,
                    reminderTime = reminder
                )
            )
            _uiState.update {
                it.copy(errorMessage = null, savedMessage = "Schedule saved.")
            }
            onSaved()
        }
    }

    fun resetToLocaleDefaults(onSaved: () -> Unit) {
        viewModelScope.launch {
            dailySchedulePreferences.resetToLocaleDefaults()
            val schedule = dailySchedulePreferences.getSchedule()
            _uiState.update {
                it.copy(
                    resetTimeText = formatTime(schedule.resetTime),
                    reminderTimeText = formatTime(schedule.reminderTime),
                    errorMessage = null,
                    savedMessage = "Locale defaults restored."
                )
            }
            onSaved()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val schedule = dailySchedulePreferences.getSchedule()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    resetTimeText = formatTime(schedule.resetTime),
                    reminderTimeText = formatTime(schedule.reminderTime)
                )
            }
        }
    }

    private fun parseTime(value: String): LocalTime? {
        val trimmed = value.trim()
        if (!Regex("^\\d{2}:\\d{2}$").matches(trimmed)) return null
        val parts = trimmed.split(":")
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    private fun formatTime(time: LocalTime): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    companion object {
        fun factory(dailySchedulePreferences: DailySchedulePreferences): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(dailySchedulePreferences = dailySchedulePreferences)
            }
        }
    }
}
