package com.kitsune.kanji.japanese.flashcards.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    rankSummary: UserRankSummary,
    lifetimeScore: Int,
    lifetimeCardsReviewed: Int,
    recentRuns: List<DeckRunHistoryItem>,
    onBack: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onOpenRunReport: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
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
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Kitsune Learner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Rank Lv ${rankSummary.level} - ${rankSummary.title}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Words covered: ${rankSummary.wordsCovered}/${rankSummary.totalWords}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val easy = rankSummary.easyWordScore?.toString() ?: "--"
                    val hard = rankSummary.hardWordScore?.toString() ?: "--"
                    Text("Easy $easy - Hard $hard", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Total score earned: $lifetimeScore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2D6C3F)
                    )
                    Text(
                        "Cards reviewed: $lifetimeCardsReviewed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Current goal: Build kanji writing consistency.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Free plan active", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onOpenUpgrade, modifier = Modifier.fillMaxWidth()) {
                        Text("Upgrade")
                    }
                }
            }
        }
        item {
            Text(
                text = "Past Runs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (recentRuns.isEmpty()) {
            item {
                Text(
                    text = "No completed runs yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B5F56)
                )
            }
        } else {
            items(recentRuns, key = { run -> run.deckRunId }) { run ->
                RunHistoryItem(
                    run = run,
                    onClick = { onOpenRunReport(run.deckRunId) }
                )
            }
        }
    }
}

@Composable
private fun RunHistoryItem(
    run: DeckRunHistoryItem,
    onClick: () -> Unit
) {
    val deckLabel = when (run.deckType) {
        DeckType.DAILY -> "Daily"
        DeckType.EXAM -> "Exam"
        DeckType.REMEDIAL -> "Remedial"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0D4C8), RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$deckLabel - ${formatRunDate(run.submittedAtEpochMillis)}",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF7A5D4A)
        )
        Text(
            text = "Score ${run.totalScore} (${run.grade})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E231B)
        )
        Text(
            text = "Reviewed ${run.cardsReviewed}/${run.totalCards} cards",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5E4D40)
        )
    }
}

private fun formatRunDate(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
