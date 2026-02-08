package com.kitsune.kanji.japanese.flashcards.domain.ink

import com.kitsune.kanji.japanese.flashcards.domain.model.StrokeTemplate
import com.kitsune.kanji.japanese.flashcards.domain.model.TemplatePoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

interface HandwritingScorer {
    fun score(sample: InkSample, template: StrokeTemplate): HandwritingScore
}

class DeterministicHandwritingScorer : HandwritingScorer {
    override fun score(sample: InkSample, template: StrokeTemplate): HandwritingScore {
        if (sample.strokes.isEmpty()) {
            return HandwritingScore(
                score = 0,
                feedback = "No ink captured. Write the character before submitting."
            )
        }

        val normalizedInk = normalizeSample(sample)
        val normalizedTemplate = normalizeTemplate(template)
        if (normalizedInk.isEmpty() || normalizedTemplate.isEmpty()) {
            return HandwritingScore(
                score = 0,
                feedback = "Unable to compare strokes. Please clear and try again."
            )
        }

        val matchedCount = min(normalizedInk.size, normalizedTemplate.size)
        val strokeScores = mutableListOf<Float>()
        var lowestShapeScore = 100f
        var worstStrokeIndex = 0

        for (index in 0 until matchedCount) {
            val inkStroke = normalizedInk[index]
            val templateStroke = normalizedTemplate[index]
            val sampledInk = resample(inkStroke, 24)
            val sampledTemplate = resample(templateStroke, 24)

            val shapeDistance = averageDistance(sampledInk, sampledTemplate)
            val shapeScale = template.tolerance + 0.22f
            val shapeScore = (1f - (shapeDistance / shapeScale)).coerceIn(0f, 1f) * 100f

            val directionScore = directionScore(sampledInk, sampledTemplate)
            val lengthScore = lengthScore(sampledInk, sampledTemplate)
            val placementScore = placementScore(sampledInk, sampledTemplate)

            if (shapeScore < lowestShapeScore) {
                lowestShapeScore = shapeScore
                worstStrokeIndex = index + 1
            }

            val combined = (shapeScore * 0.55f) +
                (directionScore * 0.20f) +
                (lengthScore * 0.15f) +
                (placementScore * 0.10f)
            strokeScores += combined
        }

        val countPenalty = abs(normalizedInk.size - normalizedTemplate.size) * 11
        val smoothnessPenalty = normalizedInk.count { it.size < 3 } * 5
        val geometryScore = if (strokeScores.isEmpty()) 0f else strokeScores.average().toFloat()
        val normalizedScore = (geometryScore - countPenalty - smoothnessPenalty)
            .roundToInt()
            .coerceIn(0, 100)

        val feedback = when {
            countPenalty >= 22 -> "Stroke count is far from expected. Match the expected stroke order."
            lowestShapeScore < 45f -> "Stroke $worstStrokeIndex shape is off. Focus on the line path and angle."
            smoothnessPenalty >= 10 -> "Some strokes are too short. Write full strokes in a single motion."
            normalizedScore >= 85 -> "Clean form. Keep this balance and control."
            normalizedScore >= 70 -> "Good attempt. Refine proportions for a higher score."
            else -> "Needs work. Slow down and match shape and spacing."
        }

        return HandwritingScore(score = normalizedScore, feedback = feedback)
    }

    private fun normalizeSample(sample: InkSample): List<List<TemplatePoint>> {
        val points = sample.strokes.flatMap { it.points }
        if (points.isEmpty()) return emptyList()
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        val width = max(1f, maxX - minX)
        val height = max(1f, maxY - minY)
        val scale = max(width, height)

        return sample.strokes.map { stroke ->
            stroke.points.map { point ->
                TemplatePoint(
                    x = ((point.x - minX) / scale).coerceIn(0f, 1f),
                    y = ((point.y - minY) / scale).coerceIn(0f, 1f)
                )
            }
        }
    }

    private fun normalizeTemplate(template: StrokeTemplate): List<List<TemplatePoint>> {
        val points = template.strokes.flatMap { it.points }
        if (points.isEmpty()) return emptyList()
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        val width = max(0.01f, maxX - minX)
        val height = max(0.01f, maxY - minY)
        val scale = max(width, height)

        return template.strokes.map { stroke ->
            stroke.points.map { point ->
                TemplatePoint(
                    x = ((point.x - minX) / scale).coerceIn(0f, 1f),
                    y = ((point.y - minY) / scale).coerceIn(0f, 1f)
                )
            }
        }
    }

    private fun resample(points: List<TemplatePoint>, targetCount: Int): List<TemplatePoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1 || targetCount <= 1) return List(targetCount) { points.first() }

        val distances = MutableList(points.size) { 0f }
        for (index in 1 until points.size) {
            val prev = points[index - 1]
            val current = points[index]
            distances[index] = distances[index - 1] + pointDistance(prev, current)
        }
        val totalLength = max(distances.last(), 0.0001f)

        return (0 until targetCount).map { targetIndex ->
            val targetDistance = (targetIndex.toFloat() / (targetCount - 1).toFloat()) * totalLength
            interpolateAtDistance(points, distances, targetDistance)
        }
    }

    private fun interpolateAtDistance(
        points: List<TemplatePoint>,
        distances: List<Float>,
        targetDistance: Float
    ): TemplatePoint {
        var right = 1
        while (right < distances.size && distances[right] < targetDistance) {
            right += 1
        }
        if (right >= distances.size) return points.last()
        val left = right - 1
        val segmentLength = max(0.0001f, distances[right] - distances[left])
        val ratio = ((targetDistance - distances[left]) / segmentLength).coerceIn(0f, 1f)
        val a = points[left]
        val b = points[right]
        return TemplatePoint(
            x = a.x + (b.x - a.x) * ratio,
            y = a.y + (b.y - a.y) * ratio
        )
    }

    private fun averageDistance(a: List<TemplatePoint>, b: List<TemplatePoint>): Float {
        val count = min(a.size, b.size)
        if (count == 0) return 1f
        var total = 0f
        for (index in 0 until count) {
            total += pointDistance(a[index], b[index])
        }
        return total / count.toFloat()
    }

    private fun directionScore(a: List<TemplatePoint>, b: List<TemplatePoint>): Float {
        if (a.size < 2 || b.size < 2) return 0f
        val aStart = a.first()
        val aEnd = a.last()
        val bStart = b.first()
        val bEnd = b.last()

        val avx = aEnd.x - aStart.x
        val avy = aEnd.y - aStart.y
        val bvx = bEnd.x - bStart.x
        val bvy = bEnd.y - bStart.y

        val aLen = max(0.0001f, hypot(avx, avy))
        val bLen = max(0.0001f, hypot(bvx, bvy))
        val dot = ((avx / aLen) * (bvx / bLen)) + ((avy / aLen) * (bvy / bLen))
        return (((dot + 1f) / 2f) * 100f).coerceIn(0f, 100f)
    }

    private fun lengthScore(a: List<TemplatePoint>, b: List<TemplatePoint>): Float {
        val aLength = polylineLength(a)
        val bLength = polylineLength(b)
        if (aLength <= 0f || bLength <= 0f) return 0f
        val ratio = min(aLength, bLength) / max(aLength, bLength)
        return (ratio * 100f).coerceIn(0f, 100f)
    }

    private fun placementScore(a: List<TemplatePoint>, b: List<TemplatePoint>): Float {
        val centerA = centerOf(a)
        val centerB = centerOf(b)
        val distance = pointDistance(centerA, centerB)
        return ((1f - distance / 0.8f) * 100f).coerceIn(0f, 100f)
    }

    private fun centerOf(points: List<TemplatePoint>): TemplatePoint {
        if (points.isEmpty()) return TemplatePoint(0f, 0f)
        val x = points.sumOf { it.x.toDouble() } / points.size.toDouble()
        val y = points.sumOf { it.y.toDouble() } / points.size.toDouble()
        return TemplatePoint(x = x.toFloat(), y = y.toFloat())
    }

    private fun polylineLength(points: List<TemplatePoint>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (index in 1 until points.size) {
            total += pointDistance(points[index - 1], points[index])
        }
        return total
    }

    private fun pointDistance(a: TemplatePoint, b: TemplatePoint): Float {
        return hypot(a.x - b.x, a.y - b.y)
    }
}
