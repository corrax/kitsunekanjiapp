package com.kitsune.kanji.japanese.flashcards.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary

@Composable
fun ProfileScreen(
    rankSummary: UserRankSummary,
    onBack: () -> Unit,
    onOpenUpgrade: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = {}) { Text("") }
        }

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
                    "Rank Lv ${rankSummary.level} · ${rankSummary.title}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Words covered: ${rankSummary.wordsCovered}/${rankSummary.totalWords}",
                    style = MaterialTheme.typography.bodyMedium
                )
                val easy = rankSummary.easyWordScore?.toString() ?: "--"
                val hard = rankSummary.hardWordScore?.toString() ?: "--"
                Text("Easy $easy · Hard $hard", style = MaterialTheme.typography.bodyMedium)
                Text("Current goal: Build kanji writing consistency.", style = MaterialTheme.typography.bodyMedium)
            }
        }

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
}
