package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.data.seed.N5SeedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class N5SeedContentTest {
    @Test
    fun build_returnsExpectedMvpShape() {
        val bundle = N5SeedContent.build()

        assertEquals(4, bundle.tracks.size)
        assertTrue(bundle.tracks.any { it.trackId == "jlpt_n5_core" })
        assertTrue(bundle.tracks.any { it.trackId == "school" })
        assertTrue(bundle.tracks.any { it.trackId == "work" })
        assertTrue(bundle.tracks.any { it.trackId == "conversation" })
        assertTrue(bundle.packs.size >= 20)
        assertTrue(bundle.cards.size >= 500)
        assertEquals(bundle.cards.size, bundle.packCards.size)
        assertEquals(bundle.packs.size, bundle.progress.size)
        assertEquals(bundle.tracks.size, bundle.progress.count { it.status.name == "UNLOCKED" })
    }

    @Test
    fun build_cardsAreUnique() {
        val bundle = N5SeedContent.build()
        val uniqueCardIds = bundle.cards.map { it.cardId }.toSet()

        assertEquals(bundle.cards.size, uniqueCardIds.size)
        assertTrue(bundle.cards.all { it.prompt.isNotBlank() })
        assertTrue(bundle.cards.none { it.prompt.startsWith("Core meaning") })
        assertEquals("one", bundle.cards.first().prompt)
        assertTrue(
            bundle.cards.all { card ->
                card.acceptedAnswersRaw
                    .split("|")
                    .map { it.trim() }
                    .contains(card.canonicalAnswer)
            }
        )
        val cardIds = uniqueCardIds
        val packIds = bundle.packs.map { it.packId }.toSet()
        assertTrue(bundle.packCards.all { it.cardId in cardIds })
        assertTrue(bundle.packCards.all { it.packId in packIds })
    }

    @Test
    fun build_templatesContainStrokeGeometry() {
        val bundle = N5SeedContent.build()
        val kanjiWriteCards = bundle.cards.count { it.type.name == "KANJI_WRITE" }
        assertEquals(kanjiWriteCards, bundle.templates.size)
        assertTrue(bundle.templates.all { it.strokePaths.isNotBlank() })
        assertTrue(
            bundle.templates.all { template ->
                template.strokePaths.split('|').size == template.expectedStrokeCount
            }
        )
    }
}
