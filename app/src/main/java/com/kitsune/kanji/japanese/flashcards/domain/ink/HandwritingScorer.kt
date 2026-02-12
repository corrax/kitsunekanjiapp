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

        val normalizedInk = sanitizeStrokes(normalizeSample(sample))
        val normalizedTemplate = sanitizeStrokes(normalizeTemplate(template))
        if (normalizedInk.isEmpty() || normalizedTemplate.isEmpty()) {
            return HandwritingScore(
                score = 0,
                feedback = "Unable to compare strokes. Please clear and try again."
            )
        }

        val inkDescriptors = normalizedInk.mapIndexed { index, stroke ->
            StrokeDescriptor(
                index = index,
                rawPoints = stroke,
                sampledPoints = resample(stroke, 24),
                length = polylineLength(stroke)
            )
        }
        val templateDescriptors = normalizedTemplate.mapIndexed { index, stroke ->
            StrokeDescriptor(
                index = index,
                rawPoints = stroke,
                sampledPoints = resample(stroke, 24),
                length = polylineLength(stroke)
            )
        }
        val shapeScale = (template.tolerance + 0.26f).coerceAtLeast(0.31f)
        val matchedStrokes = matchStrokes(
            ink = inkDescriptors,
            template = templateDescriptors,
            shapeScale = shapeScale
        )
        val geometryScore = if (matchedStrokes.isEmpty()) {
            0f
        } else {
            matchedStrokes.map { it.combinedScore }.average().toFloat()
        }
        val countScore = strokeCountAlignmentScore(
            inkCount = inkDescriptors.size,
            templateCount = templateDescriptors.size
        )
        val orderScore = strokeOrderScore(matchedStrokes)
        val flowScore = flowQualityScore(inkDescriptors)
        val straightnessScore = if (matchedStrokes.isEmpty()) {
            0f
        } else {
            matchedStrokes.map { it.straightnessScore }.average().toFloat()
        }
        val coverageScore = strokeCoverageScore(
            matchedCount = matchedStrokes.size,
            inkCount = inkDescriptors.size,
            templateCount = templateDescriptors.size
        )
        val traceScore = traceEfficiencyScore(inkDescriptors)

        val rawScore = (
            (geometryScore * 0.32f) +
                (countScore * 0.15f) +
                (orderScore * 0.08f) +
                (flowScore * 0.12f) +
                (straightnessScore * 0.13f) +
                (coverageScore * 0.10f) +
                (traceScore * 0.10f)
            )
        val antiScribbleCap = minOf(
            if (coverageScore < 42f) 58 else 100,
            if (traceScore < 38f) 56 else 100,
            if (geometryScore < 34f) 54 else 100
        )
        val normalizedScore = rawScore.roundToInt()
            .coerceAtMost(antiScribbleCap)
            .coerceIn(0, 100)
        val weakestMatch = matchedStrokes.minByOrNull { it.shapeScore }

        val feedback = when {
            coverageScore < 42f || traceScore < 38f -> "This looks more like a scribble than stroke structure. Slow down and draw distinct strokes."
            countScore < 38f -> "Stroke count is quite different from the model. Focus on the main structure first."
            geometryScore < 40f -> {
                val strokeNumber = (weakestMatch?.templateIndex ?: 0) + 1
                "Stroke $strokeNumber shape is drifting. Focus on line path and angle."
            }
            orderScore < 60f -> "Stroke sequence is unstable. Keep a smoother stroke order."
            flowScore < 55f || straightnessScore < 52f -> "Input is fragmented. Use fewer pen lifts and finish each stroke."
            normalizedScore >= 90 -> "Clean form. Keep this balance and control."
            normalizedScore >= 74 -> "Good structure. Refine spacing and angle for a higher score."
            else -> "Solid attempt. Slow down and keep shape and spacing consistent."
        }

        return HandwritingScore(score = normalizedScore, feedback = feedback)
    }

    private fun normalizeSample(sample: InkSample): List<List<TemplatePoint>> {
        val points = sample.strokes.flatMap { it.points }
        if (points.isEmpty()) return emptyList()
        val canvasWidth = sample.canvasWidth?.takeIf { it > 1f }
        val canvasHeight = sample.canvasHeight?.takeIf { it > 1f }
        val minX = if (canvasWidth != null && canvasHeight != null) 0f else points.minOf { it.x }
        val minY = if (canvasWidth != null && canvasHeight != null) 0f else points.minOf { it.y }
        val maxX = canvasWidth ?: points.maxOf { it.x }
        val maxY = canvasHeight ?: points.maxOf { it.y }
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
        if (template.strokes.isEmpty()) return emptyList()
        return template.strokes.map { stroke ->
            stroke.points.map { point ->
                TemplatePoint(
                    x = point.x.coerceIn(0f, 1f),
                    y = point.y.coerceIn(0f, 1f)
                )
            }
        }
    }

    private fun sanitizeStrokes(strokes: List<List<TemplatePoint>>): List<List<TemplatePoint>> {
        val cleaned = strokes
            .map { dedupeSequentialPoints(it) }
            .filter { it.size >= 2 }
        if (cleaned.isEmpty()) return emptyList()

        val merged = mutableListOf<MutableList<TemplatePoint>>()
        for (stroke in cleaned) {
            if (merged.isEmpty()) {
                merged += stroke.toMutableList()
                continue
            }
            val previous = merged.last()
            val previousLength = polylineLength(previous)
            val currentLength = polylineLength(stroke)
            val gap = pointDistance(previous.last(), stroke.first())
            val shouldMerge = gap <= 0.08f && (previousLength <= 0.11f || currentLength <= 0.11f)
            if (shouldMerge) {
                previous += stroke
            } else {
                merged += stroke.toMutableList()
            }
        }

        val kept = merged
            .map { it.toList() }
            .filter { stroke -> stroke.size >= 2 && polylineLength(stroke) >= 0.035f }
        if (kept.isNotEmpty()) return kept
        return listOf(cleaned.maxBy { polylineLength(it) })
    }

    private fun dedupeSequentialPoints(points: List<TemplatePoint>): List<TemplatePoint> {
        if (points.isEmpty()) return points
        val deduped = mutableListOf(points.first())
        for (index in 1 until points.size) {
            val point = points[index]
            if (pointDistance(point, deduped.last()) > 0.002f) {
                deduped += point
            }
        }
        return deduped
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

    private fun matchStrokes(
        ink: List<StrokeDescriptor>,
        template: List<StrokeDescriptor>,
        shapeScale: Float
    ): List<MatchedStroke> {
        if (ink.isEmpty() || template.isEmpty()) return emptyList()

        val candidates = mutableListOf<MatchedStroke>()
        for (inkStroke in ink) {
            for (templateStroke in template) {
                val shapeDistanceForward = averageDistance(inkStroke.sampledPoints, templateStroke.sampledPoints)
                val shapeDistanceReverse = averageDistance(inkStroke.sampledPoints.asReversed(), templateStroke.sampledPoints)
                val shapeDistance = min(shapeDistanceForward, shapeDistanceReverse)
                val shapeScore = (1f - (shapeDistance / shapeScale)).coerceIn(0f, 1f) * 100f
                val direction = directionScore(inkStroke.sampledPoints, templateStroke.sampledPoints)
                val length = lengthScore(inkStroke.sampledPoints, templateStroke.sampledPoints)
                val placement = placementScore(inkStroke.sampledPoints, templateStroke.sampledPoints)
                val straightness = straightnessSimilarity(inkStroke.rawPoints, templateStroke.rawPoints)
                val combined = (shapeScore * 0.44f) +
                    (direction * 0.14f) +
                    (length * 0.14f) +
                    (placement * 0.12f) +
                    (straightness * 0.16f)
                candidates += MatchedStroke(
                    inkIndex = inkStroke.index,
                    templateIndex = templateStroke.index,
                    combinedScore = combined,
                    shapeScore = shapeScore,
                    straightnessScore = straightness
                )
            }
        }

        val selected = mutableListOf<MatchedStroke>()
        val usedInk = mutableSetOf<Int>()
        val usedTemplate = mutableSetOf<Int>()
        val targetMatchCount = min(ink.size, template.size)

        for (candidate in candidates.sortedByDescending { it.combinedScore }) {
            if (candidate.inkIndex in usedInk || candidate.templateIndex in usedTemplate) {
                continue
            }
            selected += candidate
            usedInk += candidate.inkIndex
            usedTemplate += candidate.templateIndex
            if (selected.size >= targetMatchCount) {
                break
            }
        }

        return selected
    }

    private fun strokeCoverageScore(matchedCount: Int, inkCount: Int, templateCount: Int): Float {
        if (inkCount <= 0 || templateCount <= 0) return 0f
        val templateCoverage = matchedCount.toFloat() / templateCount.toFloat()
        val inkUtilization = matchedCount.toFloat() / inkCount.toFloat()
        return ((templateCoverage * 0.7f) + (inkUtilization * 0.3f)) * 100f
            .coerceIn(0f, 100f)
    }

    private fun strokeCountAlignmentScore(inkCount: Int, templateCount: Int): Float {
        if (templateCount <= 0) return 0f
        val difference = abs(inkCount - templateCount).toFloat()
        val relativeDelta = difference / templateCount.toFloat()
        val absoluteBand = when {
            difference == 0f -> 100f
            difference <= 1f -> 92f
            difference <= 2f -> 82f
            difference <= 3f -> 70f
            else -> 58f - ((difference - 3f) * 7f)
        }
        val relativeBand = when {
            relativeDelta <= 0.25f -> 100f
            relativeDelta <= 0.5f -> 88f
            relativeDelta <= 0.8f -> 74f
            relativeDelta <= 1.1f -> 62f
            else -> 50f
        }
        return min(absoluteBand, relativeBand).coerceIn(24f, 100f)
    }

    private fun strokeOrderScore(matches: List<MatchedStroke>): Float {
        if (matches.size < 2) return 100f
        val sorted = matches.sortedBy { it.templateIndex }
        var orderedPairs = 0
        for (index in 1 until sorted.size) {
            if (sorted[index].inkIndex > sorted[index - 1].inkIndex) {
                orderedPairs += 1
            }
        }
        val ratio = orderedPairs.toFloat() / (sorted.size - 1).toFloat()
        return (55f + (ratio * 45f)).coerceIn(55f, 100f)
    }

    private fun flowQualityScore(strokes: List<StrokeDescriptor>): Float {
        if (strokes.isEmpty()) return 0f
        val shortStrokeRatio = strokes.count { it.length < 0.08f }.toFloat() / strokes.size.toFloat()
        val sparsePointPenalty = strokes.count { it.rawPoints.size < 3 } * 6f
        val score = 100f - (shortStrokeRatio * 35f) - sparsePointPenalty
        return score.coerceIn(35f, 100f)
    }

    private fun traceEfficiencyScore(strokes: List<StrokeDescriptor>): Float {
        if (strokes.isEmpty()) return 0f
        val gridSize = 18
        val visitedCells = mutableSetOf<Int>()
        var sampledPointCount = 0
        for (stroke in strokes) {
            val sampled = resample(stroke.rawPoints, 24)
            sampled.forEach { point ->
                val x = (point.x * (gridSize - 1)).roundToInt().coerceIn(0, gridSize - 1)
                val y = (point.y * (gridSize - 1)).roundToInt().coerceIn(0, gridSize - 1)
                visitedCells += (y * gridSize) + x
                sampledPointCount += 1
            }
        }
        if (sampledPointCount <= 0) return 0f
        val uniqueRatio = visitedCells.size.toFloat() / sampledPointCount.toFloat()
        return (((uniqueRatio - 0.12f) / 0.30f) * 100f).coerceIn(15f, 100f)
    }

    private fun straightnessSimilarity(
        inkStroke: List<TemplatePoint>,
        templateStroke: List<TemplatePoint>
    ): Float {
        val inkStraightness = strokeStraightness(inkStroke)
        val templateStraightness = strokeStraightness(templateStroke)
        if (inkStraightness <= 0f || templateStraightness <= 0f) return 0f
        return (min(inkStraightness, templateStraightness) / max(inkStraightness, templateStraightness) * 100f)
            .coerceIn(0f, 100f)
    }

    private fun strokeStraightness(points: List<TemplatePoint>): Float {
        if (points.size < 2) return 0f
        val endpointDistance = pointDistance(points.first(), points.last())
        val pathLength = polylineLength(points).coerceAtLeast(0.0001f)
        return (endpointDistance / pathLength).coerceIn(0f, 1f)
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

    private data class StrokeDescriptor(
        val index: Int,
        val rawPoints: List<TemplatePoint>,
        val sampledPoints: List<TemplatePoint>,
        val length: Float
    )

    private data class MatchedStroke(
        val inkIndex: Int,
        val templateIndex: Int,
        val combinedScore: Float,
        val shapeScore: Float,
        val straightnessScore: Float
    )
}
