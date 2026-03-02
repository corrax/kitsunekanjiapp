package com.kitsune.kanji.japanese.flashcards.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import com.kitsune.kanji.japanese.flashcards.ui.common.deckThemeDrawnVisuals

private val WarmSurface = Color(0xFFFFF8F1)
private val AccentOrange = Color(0xFFFF5A00)
private val TextDark = Color(0xFF2D1E14)

data class SettingsTabUiState(
    val isLoading: Boolean = true,
    val selectedThemeId: String? = null,
    val learnerLevel: LearnerLevel = LearnerLevel.BEGINNER_N5,
    val resetTimeText: String = "",
    val reminderTimeText: String = "",
    val errorMessage: String? = null,
    val savedMessage: String? = null
)

@Composable
fun SettingsTabScreen(
    state: SettingsTabUiState,
    onLearnerLevelChange: (LearnerLevel) -> Unit,
    onResetTimeChange: (String) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    onSave: () -> Unit,
    onResetDefaults: () -> Unit,
    onOpenUpgrade: () -> Unit
) {
    val visuals = deckThemeDrawnVisuals(state.selectedThemeId)
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(visuals.baseColor)
    ) {
        Image(
            painter = painterResource(id = visuals.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.11f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(visuals.overlayTop, visuals.overlayBottom)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentOrange)
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Learner Level",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(Modifier.height(8.dp))
                        LearnerLevelPicker(
                            selected = state.learnerLevel,
                            onSelect = onLearnerLevelChange
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(Modifier.height(12.dp))
                        TimePickerRow(
                            label = "Daily Reset Time",
                            timeText = state.resetTimeText,
                            onTimeChange = onResetTimeChange
                        )
                        Spacer(Modifier.height(8.dp))
                        TimePickerRow(
                            label = "Reminder Time",
                            timeText = state.reminderTimeText,
                            onTimeChange = onReminderTimeChange
                        )
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.savedMessage != null) {
                item {
                    Text(
                        text = state.savedMessage,
                        color = Color(0xFF2E8B57),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onResetDefaults,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset Defaults")
                    }
                }
            }

            // Upgrade / Remove Ads card hidden for now
            // item {
            //     Card(
            //         modifier = Modifier.fillMaxWidth(),
            //         shape = RoundedCornerShape(12.dp),
            //         colors = CardDefaults.cardColors(containerColor = WarmSurface),
            //         elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            //         onClick = onOpenUpgrade
            //     ) {
            //         Row(
            //             modifier = Modifier
            //                 .fillMaxWidth()
            //                 .padding(14.dp),
            //             verticalAlignment = Alignment.CenterVertically,
            //             horizontalArrangement = Arrangement.Center
            //         ) {
            //             Icon(
            //                 imageVector = Icons.Outlined.Star,
            //                 contentDescription = null,
            //                 tint = AccentOrange,
            //                 modifier = Modifier.size(18.dp)
            //             )
            //             Spacer(Modifier.padding(start = 6.dp))
            //             Text(
            //                 text = "Upgrade / Remove Ads",
            //                 style = MaterialTheme.typography.labelLarge,
            //                 color = AccentOrange,
            //                 fontWeight = FontWeight.SemiBold
            //             )
            //         }
            //     }
            // }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun LearnerLevelPicker(
    selected: LearnerLevel,
    onSelect: (LearnerLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        LearnerLevel.PRE_N5 to "Complete Beginner (Pre-N5)",
        LearnerLevel.BEGINNER_N5 to "Beginner (N5)",
        LearnerLevel.BEGINNER_PLUS_N4 to "Beginner+ (N4)",
        LearnerLevel.INTERMEDIATE_N3 to "Intermediate (N3)",
        LearnerLevel.ADVANCED_N2 to "Advanced (N2)",
        LearnerLevel.UNSURE to "Not Sure"
    )

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(labels[selected] ?: selected.name)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LearnerLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(labels[level] ?: level.name) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TimePickerRow(
    label: String,
    timeText: String,
    onTimeChange: (String) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark
        )
        OutlinedButton(onClick = {
            val parts = timeText.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 18
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(context, { _, h, m ->
                onTimeChange(String.format("%02d:%02d", h, m))
            }, hour, minute, true).show()
        }) {
            Text(timeText.ifBlank { "18:00" })
        }
    }
}
