package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.seed.GoalAlignedSeedContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalAlignedSeedContentTest {
    @Test
    fun build_avoidsDuplicateMeaningPromptsWhenVocabAlreadyCoversThem() {
        val bundle = GoalAlignedSeedContent.build()

        val levelMeaningsByTrack = bundle.cards
            .groupBy { card ->
                val match = Regex("""^(.+)_([a-z])_(\d+)_(\d+)$""").matchEntire(card.cardId)
                checkNotNull(match) { "Unexpected card id: ${card.cardId}" }
                "${match.groupValues[1]}_${match.groupValues[3]}"
            }

        levelMeaningsByTrack.values.forEach { cards ->
            val vocabMeanings = cards
                .filter { it.type == CardType.VOCAB_READING }
                .map { it.meaning.trim() }
                .toSet()

            val duplicateKanjiMeaning = cards.any { card ->
                card.type == CardType.KANJI_MEANING && card.meaning.trim() in vocabMeanings
            }

            assertFalse("Found duplicate kanji meaning card in ${cards.first().cardId}", duplicateKanjiMeaning)
        }
    }

    @Test
    fun build_keepsKanjiWritingCardsForOverlappingConcepts() {
        val bundle = GoalAlignedSeedContent.build()

        assertTrue(
            bundle.cards.any { card ->
                card.type == CardType.KANJI_WRITE &&
                    card.meaning == "push" &&
                    card.prompt == "Write the kanji: push"
            }
        )
        assertTrue(
            bundle.cards.any { card ->
                card.type == CardType.KANJI_WRITE &&
                    card.meaning == "pull" &&
                    card.prompt == "Write the kanji: pull"
            }
        )
    }
}
