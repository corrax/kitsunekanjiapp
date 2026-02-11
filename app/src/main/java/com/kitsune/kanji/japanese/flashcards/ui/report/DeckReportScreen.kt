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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunCardReport
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunReport
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
                TextButton(onClick = onBack) { Text("Back") }
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
            text = "Total Score ${report.totalScore} (${report.grade})",
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
    val scoreColor = when {
        score >= 90 -> Color(0xFF1E8D53)
        score >= 70 -> Color(0xFFBF7A17)
        else -> Color(0xFFD24A3D)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0C6AE), RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "#${card.position} ${card.prompt}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2F221A)
        )
        Text(
            text = "Your answer: ${card.userAnswer ?: "(no answer submitted)"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Canonical answer: ${card.canonicalAnswer}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6C4E37)
        )
        Text(
            text = buildString {
                append("Score: ${card.score ?: 0}")
                card.effectiveScore?.let { effective ->
                    if (card.score != effective) {
                        append(" (learning $effective)")
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scoreColor,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "App note: ${card.comment ?: "No comment"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF654D3D)
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
        appendLine("Total score: ${report.totalScore} (${report.grade})")
        appendLine("Cards reviewed: ${report.cardsReviewed}/${report.totalCards}")
        appendLine()
        appendLine("Card details")
        report.cards.forEach { card ->
            appendLine("#${card.position} ${card.prompt}")
            appendLine("  Your answer: ${card.userAnswer ?: "(no answer submitted)"}")
            appendLine("  Canonical answer: ${card.canonicalAnswer}")
            appendLine("  Score: ${card.score ?: 0}${card.effectiveScore?.let { if (it != card.score) " (learning $it)" else "" } ?: ""}")
            appendLine("  App note: ${card.comment ?: "No comment"}")
        }
    }
}
