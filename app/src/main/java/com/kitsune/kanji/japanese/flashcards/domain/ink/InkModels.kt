package com.kitsune.kanji.japanese.flashcards.domain.ink

data class InkPoint(
    val x: Float,
    val y: Float
)

data class InkStroke(
    val points: List<InkPoint>
)

data class InkSample(
    val strokes: List<InkStroke>,
    val canvasWidth: Float? = null,
    val canvasHeight: Float? = null
)

data class HandwritingScore(
    val score: Int,
    val feedback: String
)
