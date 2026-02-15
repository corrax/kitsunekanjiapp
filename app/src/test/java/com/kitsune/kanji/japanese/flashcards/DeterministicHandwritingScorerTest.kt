package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.domain.ink.DeterministicHandwritingScorer
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkPoint
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkStroke
import com.kitsune.kanji.japanese.flashcards.domain.model.StrokeTemplate
import com.kitsune.kanji.japanese.flashcards.domain.model.TemplatePoint
import com.kitsune.kanji.japanese.flashcards.domain.model.TemplateStroke
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class DeterministicHandwritingScorerTest {
    private val scorer = DeterministicHandwritingScorer()
    private val template = StrokeTemplate(
        templateId = "tmpl_test",
        target = "日",
        expectedStrokeCount = 4,
        tolerance = 0.24f,
        strokes = listOf(
            templateStroke(0.15f, 0.15f, 0.15f, 0.85f),
            templateStroke(0.15f, 0.15f, 0.85f, 0.15f),
            templateStroke(0.85f, 0.15f, 0.85f, 0.85f),
            templateStroke(0.15f, 0.85f, 0.85f, 0.85f)
        )
    )

    @Test
    fun score_emptyInk_returnsZero() = runTest {
        val result = scorer.score(InkSample(emptyList()), template)
        assertTrue(result.score == 0)
    }

    @Test
    fun score_balancedInk_returnsPassingScore() = runTest {
        val sample = InkSample(
            strokes = listOf(
                stroke(20f, 20f, 20f, 200f),
                stroke(20f, 20f, 180f, 20f),
                stroke(180f, 20f, 180f, 200f),
                stroke(20f, 200f, 180f, 200f)
            )
        )

        val result = scorer.score(sample, template)
        assertTrue(result.score >= 70)
    }

    @Test
    fun score_mismatchedGeometry_isLower() = runTest {
        val goodSample = InkSample(
            strokes = listOf(
                stroke(20f, 20f, 20f, 200f),
                stroke(20f, 20f, 180f, 20f),
                stroke(180f, 20f, 180f, 200f),
                stroke(20f, 200f, 180f, 200f)
            )
        )
        val badSample = InkSample(
            strokes = listOf(
                stroke(30f, 30f, 170f, 170f),
                stroke(170f, 30f, 30f, 170f),
                stroke(30f, 110f, 170f, 110f),
                stroke(100f, 30f, 100f, 170f)
            )
        )

        val good = scorer.score(goodSample, template).score
        val bad = scorer.score(badSample, template).score
        assertTrue(good > bad)
    }

    @Test
    fun score_fragmentedButStructuredInk_isStillFair() = runTest {
        val fragmentedSample = InkSample(
            strokes = listOf(
                stroke(20f, 20f, 20f, 110f),
                stroke(20f, 110f, 20f, 200f),
                stroke(20f, 20f, 100f, 20f),
                stroke(100f, 20f, 180f, 20f),
                stroke(180f, 20f, 180f, 110f),
                stroke(180f, 110f, 180f, 200f),
                stroke(20f, 200f, 100f, 200f),
                stroke(100f, 200f, 180f, 200f)
            )
        )

        val result = scorer.score(fragmentedSample, template)
        assertTrue(result.score >= 55)
    }

    @Test
    fun score_affineSkewedButSameForm_staysPassable() = runTest {
        val skewedSample = InkSample(
            strokes = listOf(
                stroke(30f, 22f, 48f, 205f),
                stroke(30f, 22f, 192f, 32f),
                stroke(176f, 28f, 194f, 210f),
                stroke(42f, 198f, 206f, 210f)
            )
        )

        val result = scorer.score(skewedSample, template)
        assertTrue("skewed score=${result.score}", result.score >= 62)
    }

    @Test
    fun score_wrongGlyphShape_isPenalized() = runTest {
        val balancedSample = InkSample(
            strokes = listOf(
                stroke(20f, 20f, 20f, 200f),
                stroke(20f, 20f, 180f, 20f),
                stroke(180f, 20f, 180f, 200f),
                stroke(20f, 200f, 180f, 200f)
            )
        )
        val wrongGlyphSample = InkSample(
            strokes = listOf(
                stroke(30f, 30f, 170f, 170f),
                stroke(170f, 30f, 30f, 170f),
                stroke(100f, 20f, 100f, 200f),
                stroke(20f, 100f, 180f, 100f)
            )
        )

        val balanced = scorer.score(balancedSample, template).score
        val wrong = scorer.score(wrongGlyphSample, template).score
        assertTrue("wrong score=$wrong balanced=$balanced", wrong <= 58)
        assertTrue("wrong score=$wrong balanced=$balanced", balanced >= wrong + 12)
    }

    @Test
    fun score_denseScribble_isRejectedAsLow() = runTest {
        val balancedSample = InkSample(
            strokes = listOf(
                stroke(20f, 20f, 20f, 200f),
                stroke(20f, 20f, 180f, 20f),
                stroke(180f, 20f, 180f, 200f),
                stroke(20f, 200f, 180f, 200f)
            )
        )
        val scribble = InkSample(
            canvasWidth = 280f,
            canvasHeight = 280f,
            strokes = listOf(
                stroke(70f, 170f, 190f, 130f),
                stroke(80f, 120f, 200f, 180f),
                stroke(90f, 180f, 210f, 140f),
                stroke(70f, 145f, 210f, 150f),
                stroke(105f, 100f, 165f, 220f),
                stroke(80f, 190f, 190f, 95f),
                stroke(115f, 115f, 195f, 195f),
                stroke(95f, 205f, 215f, 125f)
            )
        )

        val balanced = scorer.score(balancedSample, template).score
        val scribbleScore = scorer.score(scribble, template).score
        assertTrue("scribble score=$scribbleScore", scribbleScore <= 60)
        assertTrue("scribble score=$scribbleScore balanced=$balanced", balanced >= scribbleScore + 12)
    }

    private fun stroke(x1: Float, y1: Float, x2: Float, y2: Float): InkStroke {
        return InkStroke(
            points = listOf(
                InkPoint(x = x1, y = y1),
                InkPoint(x = x2, y = y2)
            )
        )
    }

    private fun templateStroke(x1: Float, y1: Float, x2: Float, y2: Float): TemplateStroke {
        return TemplateStroke(
            points = listOf(
                TemplatePoint(x = x1, y = y1),
                TemplatePoint(x = x2, y = y2)
            )
        )
    }
}
