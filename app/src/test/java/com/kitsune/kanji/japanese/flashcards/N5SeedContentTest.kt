package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.data.seed.N5SeedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class N5SeedContentTest {
    @Test
    fun build_returnsExpectedMvpShape() {
        val bundle = N5SeedContent.build()

        assertEquals(1, bundle.tracks.size)
        assertEquals(10, bundle.packs.size)
        assertEquals(200, bundle.cards.size)
        assertEquals(200, bundle.templates.size)
        assertEquals(200, bundle.packCards.size)
        assertEquals(10, bundle.progress.size)
        assertEquals(1, bundle.progress.count { it.status.name == "UNLOCKED" })
    }

    @Test
    fun build_cardsAreUnique() {
        val bundle = N5SeedContent.build()
        val uniqueCardIds = bundle.cards.map { it.cardId }.toSet()
        val uniqueTargets = bundle.cards.map { it.canonicalAnswer }.toSet()

        assertEquals(200, uniqueCardIds.size)
        assertEquals(200, uniqueTargets.size)
        assertTrue(bundle.cards.all { it.prompt.isNotBlank() })
        assertTrue(bundle.cards.none { it.prompt.startsWith("Core meaning") })
        assertEquals("one", bundle.cards.first().prompt)
        assertTrue(bundle.cards.all { it.acceptedAnswersRaw == it.canonicalAnswer })
    }

    @Test
    fun build_templatesContainStrokeGeometry() {
        val bundle = N5SeedContent.build()
        assertTrue(bundle.templates.all { it.strokePaths.isNotBlank() })
        assertTrue(
            bundle.templates.all { template ->
                template.strokePaths.split('|').size == template.expectedStrokeCount
            }
        )
    }
}
