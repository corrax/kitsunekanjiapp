package com.kitsune.kanji.japanese.flashcards.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun GenkoyoshiInkPreview(
    strokePathsRaw: String?,
    modifier: Modifier = Modifier
) {
    val strokes = remember(strokePathsRaw) { parseStrokePathsRaw(strokePathsRaw) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFFFFFCF8), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFBFA489), RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDimension = min(size.width, size.height)
            val guideStroke = (minDimension * 0.012f).coerceAtLeast(1f)
            val guideColor = Color(0xFFB9A58F)
            val dash = PathEffect.dashPathEffect(
                intervals = floatArrayOf(minDimension * 0.05f, minDimension * 0.03f)
            )
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            drawLine(
                color = guideColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = guideStroke,
                pathEffect = dash
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = guideStroke,
                pathEffect = dash
            )

            val allPoints = strokes.flatten()
            if (allPoints.isEmpty()) return@Canvas

            val minX = allPoints.minOf { it.x }
            val maxX = allPoints.maxOf { it.x }
            val minY = allPoints.minOf { it.y }
            val maxY = allPoints.maxOf { it.y }
            val rawWidth = max(maxX - minX, 1f)
            val rawHeight = max(maxY - minY, 1f)
            val drawArea = minDimension * 0.74f
            val scale = drawArea / max(rawWidth, rawHeight)
            val areaLeft = (size.width - drawArea) / 2f
            val areaTop = (size.height - drawArea) / 2f
            val offsetX = areaLeft + ((drawArea - (rawWidth * scale)) / 2f) - (minX * scale)
            val offsetY = areaTop + ((drawArea - (rawHeight * scale)) / 2f) - (minY * scale)
            val strokeWidth = (minDimension * 0.08f).coerceAtLeast(2f)

            strokes.forEach { stroke ->
                if (stroke.isEmpty()) return@forEach
                if (stroke.size == 1) {
                    val point = stroke.first()
                    drawCircle(
                        color = Color(0xFF2B1E17),
                        radius = strokeWidth / 2f,
                        center = Offset(
                            x = (point.x * scale) + offsetX,
                            y = (point.y * scale) + offsetY
                        )
                    )
                    return@forEach
                }
                val path = Path().apply {
                    val first = stroke.first()
                    moveTo((first.x * scale) + offsetX, (first.y * scale) + offsetY)
                    stroke.drop(1).forEach { point ->
                        lineTo((point.x * scale) + offsetX, (point.y * scale) + offsetY)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFF2B1E17),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

private fun parseStrokePathsRaw(raw: String?): List<List<Offset>> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split("|")
        .mapNotNull { strokeChunk ->
            val points = strokeChunk.split(";")
                .mapNotNull { pointChunk ->
                    val values = pointChunk.split(",")
                    if (values.size != 2) return@mapNotNull null
                    val x = values[0].toFloatOrNull() ?: return@mapNotNull null
                    val y = values[1].toFloatOrNull() ?: return@mapNotNull null
                    Offset(x = x, y = y)
                }
            points.takeIf { it.isNotEmpty() }
        }
}
