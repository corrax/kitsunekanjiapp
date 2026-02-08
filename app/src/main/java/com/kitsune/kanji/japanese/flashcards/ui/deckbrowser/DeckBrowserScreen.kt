package com.kitsune.kanji.japanese.flashcards.ui.deckbrowser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DeckBrowserScreen(
    selectedThemeId: String?,
    onBack: () -> Unit,
    onSelectTheme: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF3E5D4), Color(0xFFE9D4BC))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deck Browser",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
        items(deckThemeCatalog, key = { it.id }) { theme ->
            DeckThemeCard(
                option = theme,
                isSelected = selectedThemeId == theme.id,
                onSelect = { onSelectTheme(theme.id) }
            )
        }
    }
}

@Composable
private fun DeckThemeCard(
    option: DeckThemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF8EE))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF8B3A2E) else Color(0xFFE2CDAF),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSelect)
            ) {
                Image(
                    painter = painterResource(option.heroRes),
                    contentDescription = option.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x22000000), Color(0x99000000))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = option.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = option.category,
                        color = Color(0xFFEBD7C3),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xD92C1E17))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = option.difficulty,
                        color = Color(0xFFFFE2C7),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = "Levels",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Column(
                modifier = Modifier.wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                option.levels.forEach { level ->
                    Text(
                        text = "L${level.level}: ${level.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF634A39)
                    )
                }
            }

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSelected) "Selected" else "Select Deck")
            }
        }
    }
}
