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
    fun score_emptyInk_returnsZero() {
        val result = scorer.score(InkSample(emptyList()), template)
        assertTrue(result.score == 0)
    }

    @Test
    fun score_balancedInk_returnsPassingScore() {
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
    fun score_mismatchedGeometry_isLower() {
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
    fun score_fragmentedButStructuredInk_isStillFair() {
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
    fun score_extremeMismatch_remainsLowerThanBalanced() {
        val balancedSample = InkSample(
            strokes = listOf(
                stroke(20f, 20f, 20f, 200f),
                stroke(20f, 20f, 180f, 20f),
                stroke(180f, 20f, 180f, 200f),
                stroke(20f, 200f, 180f, 200f)
            )
        )
        val extremeSample = InkSample(
            strokes = listOf(
                stroke(20f, 100f, 180f, 100f),
                stroke(50f, 40f, 150f, 160f),
                stroke(150f, 40f, 50f, 160f),
                stroke(30f, 30f, 30f, 180f),
                stroke(170f, 30f, 170f, 180f),
                stroke(20f, 20f, 180f, 180f),
                stroke(180f, 20f, 20f, 180f)
            )
        )

        val balanced = scorer.score(balancedSample, template).score
        val extreme = scorer.score(extremeSample, template).score
        assertTrue(balanced >= extreme + 10)
    }

    @Test
    fun score_denseScribble_isRejectedAsLow() {
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

        val result = scorer.score(scribble, template)
        assertTrue(result.score <= 50)
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
