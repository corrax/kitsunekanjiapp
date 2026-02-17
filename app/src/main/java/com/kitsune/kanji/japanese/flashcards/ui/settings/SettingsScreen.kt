package com.kitsune.kanji.japanese.flashcards.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onResetTimeChange: (String) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    onSave: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFFF6EBDD), Color(0xFFE4CDB0)))
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "Set daily challenge reset/reminder times in local device time.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        TimePickerRow(
            label = "Daily Reset Time",
            value = state.resetTimeText,
            onTimePicked = onResetTimeChange
        )
        TimePickerRow(
            label = "Reminder Time",
            value = state.reminderTimeText,
            onTimePicked = onReminderTimeChange
        )

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        state.savedMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Times")
        }
        OutlinedButton(
            onClick = onResetDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use Locale Defaults")
        }
    }
}

@Composable
private fun TimePickerRow(
    label: String,
    value: String,
    onTimePicked: (String) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val initial = parseOrDefault(value)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onTimePicked(String.format("%02d:%02d", hour, minute))
                    },
                    initial.hour,
                    initial.minute,
                    true
                ).show()
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = {
                val initial = parseOrDefault(value)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onTimePicked(String.format("%02d:%02d", hour, minute))
                    },
                    initial.hour,
                    initial.minute,
                    true
                ).show()
            },
            modifier = Modifier.width(130.dp)
        ) {
            Text(value)
        }
    }
}

private fun parseOrDefault(value: String): LocalTime {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()
    val minute = parts.getOrNull(1)?.toIntOrNull()
    return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
        LocalTime.of(hour, minute)
    } else {
        LocalTime.of(18, 0)
    }
}
