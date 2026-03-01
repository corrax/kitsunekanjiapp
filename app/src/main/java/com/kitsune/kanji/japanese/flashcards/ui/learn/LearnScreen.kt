package com.kitsune.kanji.japanese.flashcards.ui.learn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.ui.deck.DeckScreen
import com.kitsune.kanji.japanese.flashcards.ui.deck.DeckUiState

@Composable
fun LearnScreen(
    state: DeckUiState,
    trackId: String,
    onAutoInitialize: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmitCard: (InkSample, String?, String?, List<String>) -> Unit,
    onDeckSubmitted: (String) -> Unit,
    onSubmitDeck: () -> Unit,
    onDismissGestureOverlay: (Boolean) -> Unit
) {
    LaunchedEffect(trackId) {
        onAutoInitialize(trackId)
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        state.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Could not load your daily deck",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp)
                    )
                }
            }
        }
        else -> {
            DeckScreen(
                state = state,
                onBack = {},
                onPrevious = onPrevious,
                onNext = onNext,
                onSubmitCard = onSubmitCard,
                onDeckSubmitted = onDeckSubmitted,
                onSubmitDeck = onSubmitDeck,
                onDismissGestureOverlay = onDismissGestureOverlay
            )
        }
    }
}
