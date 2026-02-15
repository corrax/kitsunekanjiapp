package com.kitsune.kanji.japanese.flashcards.domain.ink

import com.kitsune.kanji.japanese.flashcards.domain.model.StrokeTemplate
import com.kitsune.kanji.japanese.flashcards.domain.model.TemplatePoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

interface HandwritingScorer {
    suspend fun score(sample: InkSample, template: StrokeTemplate): HandwritingScore
}

class DeterministicHandwritingScorer : HandwritingScorer {
    override suspend fun score(sample: InkSample, template: StrokeTemplate): HandwritingScore {
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
        val intersectionScore = intersectionDensityScore(inkDescriptors)
        val glyphDiffScore = glyphDiffScore(
            ink = inkDescriptors,
            template = templateDescriptors
        )

        val rawScore = (
            (geometryScore * 0.24f) +
                (countScore * 0.12f) +
                (orderScore * 0.07f) +
                (flowScore * 0.11f) +
                (straightnessScore * 0.10f) +
                (coverageScore * 0.09f) +
                (traceScore * 0.09f) +
                (glyphDiffScore * 0.18f)
            )
        val antiScribbleCap = minOf(
            if (coverageScore < 45f) 50 else 100,
            if (traceScore < 42f) 48 else 100,
            if (geometryScore < 38f) 50 else 100,
            if (intersectionScore < 56f) 44 else 100,
            if (glyphDiffScore < 38f && geometryScore < 60f) 42 else 100,
            if (glyphDiffScore < 46f && coverageScore < 55f) 50 else 100
        )
        val normalizedScore = rawScore.roundToInt()
            .coerceAtMost(antiScribbleCap)
            .coerceIn(0, 100)
        val weakestMatch = matchedStrokes.minByOrNull { it.shapeScore }

        val feedback = when {
            coverageScore < 45f || traceScore < 42f || intersectionScore < 56f ->
                "This looks more like a scribble than stroke structure. Slow down and draw distinct strokes."
            glyphDiffScore < 45f && (geometryScore < 65f || coverageScore < 60f) ->
                "The overall character form is off. Keep proportion and placement closer to the target kanji."
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

    private fun intersectionDensityScore(strokes: List<StrokeDescriptor>): Float {
        if (strokes.isEmpty()) return 0f
        val segments = mutableListOf<LineSegment>()
        strokes.forEach { stroke ->
            if (stroke.sampledPoints.size < 2) return@forEach
            for (index in 1 until stroke.sampledPoints.size) {
                segments += LineSegment(
                    strokeIndex = stroke.index,
                    segmentIndex = index - 1,
                    start = stroke.sampledPoints[index - 1],
                    end = stroke.sampledPoints[index]
                )
            }
        }
        if (segments.size < 2) return 100f

        var comparisons = 0
        var intersections = 0
        for (left in 0 until segments.lastIndex) {
            val a = segments[left]
            for (right in (left + 1) until segments.size) {
                val b = segments[right]
                if (a.strokeIndex == b.strokeIndex && abs(a.segmentIndex - b.segmentIndex) <= 1) {
                    continue
                }
                comparisons += 1
                if (properIntersection(a.start, a.end, b.start, b.end)) {
                    intersections += 1
                }
            }
        }
        if (comparisons <= 0) return 100f
        val density = intersections.toFloat() / comparisons.toFloat()
        return ((1f - (density / 0.22f)) * 100f).coerceIn(0f, 100f)
    }

    private fun glyphDiffScore(
        ink: List<StrokeDescriptor>,
        template: List<StrokeDescriptor>
    ): Float {
        if (ink.isEmpty() || template.isEmpty()) return 0f
        val templateStrokes = template.map { it.sampledPoints }
        val templatePoints = templateStrokes.flatten()
        if (templatePoints.isEmpty()) return 0f
        val templateCenter = centerOf(templatePoints)
        var best = 0f
        val scales = listOf(0.90f, 1.0f, 1.10f)
        val shears = listOf(-0.08f, 0f, 0.08f)

        for (scaleX in scales) {
            for (scaleY in scales) {
                for (shearX in shears) {
                    val affineOnly = ink.map { stroke ->
                        stroke.sampledPoints.map { point ->
                            applyAffine(point, scaleX = scaleX, scaleY = scaleY, shearX = shearX)
                        }
                    }
                    val affinePoints = affineOnly.flatten()
                    if (affinePoints.isEmpty()) continue
                    val affineCenter = centerOf(affinePoints)
                    val translation = TemplatePoint(
                        x = (templateCenter.x - affineCenter.x).coerceIn(-0.12f, 0.12f),
                        y = (templateCenter.y - affineCenter.y).coerceIn(-0.12f, 0.12f)
                    )
                    val aligned = affineOnly.map { stroke ->
                        stroke.map { point ->
                            TemplatePoint(
                                x = (point.x + translation.x).coerceIn(-0.2f, 1.2f),
                                y = (point.y + translation.y).coerceIn(-0.2f, 1.2f)
                            )
                        }
                    }
                    val candidate = glyphSimilarity(
                        inkStrokes = aligned,
                        templateStrokes = templateStrokes
                    )
                    if (candidate > best) {
                        best = candidate
                    }
                }
            }
        }
        return best
    }

    private fun applyAffine(
        point: TemplatePoint,
        scaleX: Float,
        scaleY: Float,
        shearX: Float
    ): TemplatePoint {
        val centeredX = point.x - 0.5f
        val centeredY = point.y - 0.5f
        val shearedX = centeredX + (shearX * centeredY)
        val scaledX = shearedX * scaleX
        val scaledY = centeredY * scaleY
        return TemplatePoint(
            x = scaledX + 0.5f,
            y = scaledY + 0.5f
        )
    }

    private fun glyphSimilarity(
        inkStrokes: List<List<TemplatePoint>>,
        templateStrokes: List<List<TemplatePoint>>
    ): Float {
        val gridSize = 32
        val inkMask = rasterizeMask(inkStrokes, gridSize)
        val templateMask = rasterizeMask(templateStrokes, gridSize)
        if (inkMask.isEmpty() || templateMask.isEmpty()) return 0f

        val overlap = overlapCount(inkMask, templateMask).toFloat()
        val precision = ((overlap / inkMask.size.toFloat()) * 100f).coerceIn(0f, 100f)
        val recall = ((overlap / templateMask.size.toFloat()) * 100f).coerceIn(0f, 100f)
        val dice = if (inkMask.isEmpty() && templateMask.isEmpty()) {
            100f
        } else {
            ((2f * overlap) / (inkMask.size + templateMask.size).toFloat() * 100f).coerceIn(0f, 100f)
        }
        val chamferDistance = bidirectionalChamferDistance(
            from = inkStrokes.flatten(),
            to = templateStrokes.flatten()
        )
        val chamferScore = ((1f - (chamferDistance / 0.20f)) * 100f).coerceIn(0f, 100f)

        return (precision * 0.46f) +
            (recall * 0.20f) +
            (dice * 0.20f) +
            (chamferScore * 0.14f)
    }

    private fun rasterizeMask(
        strokes: List<List<TemplatePoint>>,
        gridSize: Int
    ): Set<Int> {
        if (gridSize < 2) return emptySet()
        val cells = mutableSetOf<Int>()
        strokes.forEach { stroke ->
            if (stroke.isEmpty()) return@forEach
            if (stroke.size == 1) {
                addPointToMask(stroke.first(), gridSize, cells)
                return@forEach
            }
            for (index in 1 until stroke.size) {
                val start = stroke[index - 1]
                val end = stroke[index]
                val steps = (((max(abs(end.x - start.x), abs(end.y - start.y))) * gridSize * 2f).roundToInt())
                    .coerceAtLeast(1)
                for (step in 0..steps) {
                    val t = step.toFloat() / steps.toFloat()
                    val point = TemplatePoint(
                        x = start.x + ((end.x - start.x) * t),
                        y = start.y + ((end.y - start.y) * t)
                    )
                    addPointToMask(point, gridSize, cells)
                }
            }
        }
        return cells
    }

    private fun addPointToMask(point: TemplatePoint, gridSize: Int, cells: MutableSet<Int>) {
        val x = (point.x.coerceIn(0f, 1f) * (gridSize - 1)).roundToInt().coerceIn(0, gridSize - 1)
        val y = (point.y.coerceIn(0f, 1f) * (gridSize - 1)).roundToInt().coerceIn(0, gridSize - 1)
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = (x + dx).coerceIn(0, gridSize - 1)
                val ny = (y + dy).coerceIn(0, gridSize - 1)
                cells += (ny * gridSize) + nx
            }
        }
    }

    private fun overlapCount(first: Set<Int>, second: Set<Int>): Int {
        if (first.isEmpty() || second.isEmpty()) return 0
        val smaller = if (first.size <= second.size) first else second
        val larger = if (first.size <= second.size) second else first
        var overlap = 0
        for (cell in smaller) {
            if (cell in larger) {
                overlap += 1
            }
        }
        return overlap
    }

    private fun bidirectionalChamferDistance(
        from: List<TemplatePoint>,
        to: List<TemplatePoint>
    ): Float {
        if (from.isEmpty() || to.isEmpty()) return 1f
        val forward = averageNearestDistance(from = from, to = to)
        val backward = averageNearestDistance(from = to, to = from)
        return (forward + backward) / 2f
    }

    private fun averageNearestDistance(
        from: List<TemplatePoint>,
        to: List<TemplatePoint>
    ): Float {
        if (from.isEmpty() || to.isEmpty()) return 1f
        var total = 0f
        for (point in from) {
            var nearest = Float.MAX_VALUE
            for (candidate in to) {
                val distance = pointDistance(point, candidate)
                if (distance < nearest) {
                    nearest = distance
                }
            }
            total += nearest
        }
        return total / from.size.toFloat()
    }

    private fun properIntersection(
        aStart: TemplatePoint,
        aEnd: TemplatePoint,
        bStart: TemplatePoint,
        bEnd: TemplatePoint
    ): Boolean {
        val o1 = orientation(aStart, aEnd, bStart)
        val o2 = orientation(aStart, aEnd, bEnd)
        val o3 = orientation(bStart, bEnd, aStart)
        val o4 = orientation(bStart, bEnd, aEnd)
        val strictCross = o1 * o2 < 0f && o3 * o4 < 0f
        return strictCross
    }

    private fun orientation(a: TemplatePoint, b: TemplatePoint, c: TemplatePoint): Float {
        return ((b.x - a.x) * (c.y - a.y)) - ((b.y - a.y) * (c.x - a.x))
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

    private data class LineSegment(
        val strokeIndex: Int,
        val segmentIndex: Int,
        val start: TemplatePoint,
        val end: TemplatePoint
    )
}
