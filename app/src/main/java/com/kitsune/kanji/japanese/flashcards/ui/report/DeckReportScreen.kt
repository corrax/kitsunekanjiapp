package com.kitsune.kanji.japanese.flashcards.ui.report

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunCardReport
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunReport
import com.kitsune.kanji.japanese.flashcards.ui.common.GenkoyoshiInkPreview
import com.kitsune.kanji.japanese.flashcards.ui.common.scoreVisualFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DeckReportScreen(
    state: DeckReportUiState,
    onBack: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val report = state.report
    if (report == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(state.errorMessage ?: "Report unavailable.")
        }
        return
    }
    val shareText = remember(report) { buildShareReportText(report) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFFF9F3EC))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Deck Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share performance report"))
                    }
                ) {
                    Text("Share")
                }
            }
        }
        item {
            ReportSummaryCard(report = report)
        }
        items(report.cards, key = { card -> card.cardId }) { card ->
            ReportCardItem(card = card)
        }
        item {
            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
        }
        item {
            Box(modifier = Modifier.size(1.dp))
        }
    }
}

@Composable
private fun ReportSummaryCard(report: DeckRunReport) {
    val finishedAt = report.submittedAtEpochMillis ?: report.startedAtEpochMillis
    val subtitle = when (report.deckType) {
        DeckType.DAILY -> "Daily Deck"
        DeckType.EXAM -> "Exam Deck"
        DeckType.REMEDIAL -> "Remedial Deck"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFDCC3A8), RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "$subtitle - ${formatEpochMillis(finishedAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6A4E3B)
        )
        Text(
            text = "Total Score ${report.totalScore}/100 (${report.grade})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF372418)
        )
        Text(
            text = "Cards reviewed ${report.cardsReviewed}/${report.totalCards}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReportCardItem(card: DeckRunCardReport) {
    val score = card.score ?: 0
    val visual = scoreVisualFor(score)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0C6AE), RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${card.position} ${card.prompt}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2F221A),
                modifier = Modifier.weight(1f)
            )
            ScoreStamp(label = visual.label, background = visual.stampBackground, textColor = visual.stampText)
        }
        if (card.type == CardType.KANJI_WRITE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GenkoyoshiInkPreview(
                    strokePathsRaw = card.strokePathsRaw,
                    modifier = Modifier.size(120.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Your answer: ${card.userAnswer ?: "(no answer submitted)"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Rendered writing sample",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C4E37)
                    )
                }
            }
        } else {
            Text(
                text = "Your answer: ${card.userAnswer ?: "(no answer submitted)"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Canonical answer: ${card.canonicalAnswer}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6C4E37)
        )
        Text(
            text = "Score: ${card.score ?: 0}/100",
            style = MaterialTheme.typography.bodyMedium,
            color = visual.toneColor,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Comment: ${card.comment ?: "No comment"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF654D3D)
        )
    }
}

@Composable
private fun ScoreStamp(label: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, textColor.copy(alpha = 0.38f), RoundedCornerShape(999.dp))
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

private fun formatEpochMillis(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

private fun buildShareReportText(report: DeckRunReport): String {
    return buildString {
        appendLine("Kitsune Deck Report")
        appendLine("Deck type: ${report.deckType}")
        appendLine("Run ID: ${report.deckRunId}")
        appendLine("Finished: ${formatEpochMillis(report.submittedAtEpochMillis ?: report.startedAtEpochMillis)}")
        appendLine("Total score: ${report.totalScore}/100 (${report.grade})")
        appendLine("Cards reviewed: ${report.cardsReviewed}/${report.totalCards}")
        appendLine()
        appendLine("Card details")
        report.cards.forEach { card ->
            appendLine("#${card.position} ${card.prompt}")
            appendLine("  Your answer: ${card.userAnswer ?: "(no answer submitted)"}")
            appendLine("  Canonical answer: ${card.canonicalAnswer}")
            appendLine("  Score: ${card.score ?: 0}/100")
            appendLine("  Comment: ${card.comment ?: "No comment"}")
        }
    }
}
