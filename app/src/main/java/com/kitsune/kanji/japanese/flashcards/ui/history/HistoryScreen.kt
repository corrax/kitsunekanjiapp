package com.kitsune.kanji.japanese.flashcards.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.scoring.requiresReinforcement
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.KanjiAttemptHistoryItem
import com.kitsune.kanji.japanese.flashcards.ui.common.GenkoyoshiInkPreview
import com.kitsune.kanji.japanese.flashcards.ui.common.scoreVisualFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onSelectTab: (HistoryTab) -> Unit,
    onOpenRunReport: (String) -> Unit,
    onRetest: (String) -> Unit
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFFF7F4EF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    text = "Study History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {}) { Text("") }
            }
        }

        if (state.errorMessage != null) {
            item {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                HistoryTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.selectedTab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(
                                if (tab == HistoryTab.CARDS) {
                                    "Cards"
                                } else {
                                    "Reports"
                                }
                            )
                        }
                    )
                }
            }
        }

        when (state.selectedTab) {
            HistoryTab.CARDS -> {
                if (state.attempts.isEmpty()) {
                    item {
                        Text(
                            text = "No kanji attempts yet. Start a deck to build your history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6C6258)
                        )
                    }
                } else {
                    items(state.attempts, key = { it.attemptId }) { attempt ->
                        HistoryCard(
                            item = attempt,
                            isRetesting = state.retestAttemptIdInProgress == attempt.attemptId,
                            onOpenRunReport = { onOpenRunReport(attempt.deckRunId) },
                            onRetest = { onRetest(attempt.attemptId) }
                        )
                    }
                }
            }
            HistoryTab.REPORTS -> {
                if (state.reports.isEmpty()) {
                    item {
                        Text(
                            text = "No submitted deck reports yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6C6258)
                        )
                    }
                } else {
                    items(state.reports, key = { it.deckRunId }) { report ->
                        ReportHistoryCard(
                            report = report,
                            onOpenRunReport = { onOpenRunReport(report.deckRunId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: KanjiAttemptHistoryItem,
    isRetesting: Boolean,
    onOpenRunReport: () -> Unit,
    onRetest: () -> Unit
) {
    val weakScore = requiresReinforcement(item.scoreTotal)
    val visual = scoreVisualFor(item.scoreTotal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenkoyoshiInkPreview(
                strokePathsRaw = item.strokePathsRaw,
                modifier = Modifier.size(92.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.canonicalAnswer} - ${item.prompt}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2018),
                        modifier = Modifier.weight(1f)
                    )
                    ScoreStamp(
                        label = visual.label,
                        background = visual.stampBackground,
                        textColor = visual.stampText
                    )
                }
                Text(
                    text = "Your answer: ${item.userAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5E4A3C)
                )
                Text(
                    text = "Deck: ${item.deckLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6E5544)
                )
                Text(
                    text = scoreLabel(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = visual.toneColor,
                    fontWeight = FontWeight.Medium
                )
                if (item.assistCount > 0) {
                    Text(
                        text = "Assist used x${item.assistCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A4E2C),
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Attempted: ${formatAttemptDate(item.attemptedAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5E4A3C)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onOpenRunReport) {
                Text("View Report")
            }
            if (weakScore) {
                Button(
                    onClick = onRetest,
                    enabled = !isRetesting
                ) {
                    Text(if (isRetesting) "Preparing..." else "Retest")
                }
            }
        }
    }
}

@Composable
private fun ReportHistoryCard(
    report: DeckRunHistoryItem,
    onOpenRunReport: () -> Unit
) {
    val visual = scoreVisualFor(report.totalScore)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenRunReport)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val deckLabel = when (report.deckType) {
            DeckType.DAILY -> "Daily"
            DeckType.EXAM -> "Exam"
            DeckType.REMEDIAL -> "Remedial"
        }
        Text(
            text = "$deckLabel - ${formatAttemptDate(report.submittedAtEpochMillis)}",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF7A5A47)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score ${report.totalScore} (${report.grade})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = visual.toneColor,
                modifier = Modifier.weight(1f)
            )
            ScoreStamp(
                label = visual.label,
                background = visual.stampBackground,
                textColor = visual.stampText
            )
        }
        Text(
            text = "Reviewed ${report.cardsReviewed}/${report.totalCards}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5E4A3C)
        )
        TextButton(onClick = onOpenRunReport) {
            Text("Open Report")
        }
    }
}

@Composable
private fun ScoreStamp(label: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun scoreLabel(item: KanjiAttemptHistoryItem): String {
    return "Score: ${item.scoreTotal}/100"
}

private fun formatAttemptDate(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
