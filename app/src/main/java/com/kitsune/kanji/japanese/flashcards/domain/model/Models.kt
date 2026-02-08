package com.kitsune.kanji.japanese.flashcards.domain.model

import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType

data class HomeSnapshot(
    val trackId: String,
    val trackTitle: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val hasStartedDailyChallenge: Boolean,
    val shouldShowDailyReminder: Boolean,
    val rankSummary: UserRankSummary,
    val powerUps: List<PowerUpInventory>,
    val packs: List<PackProgress>
)

data class UserRankSummary(
    val hiddenRating: Int,
    val level: Int,
    val title: String,
    val wordsCovered: Int,
    val totalWords: Int,
    val easyWordScore: Int?,
    val hardWordScore: Int?
)

data class PackProgress(
    val packId: String,
    val level: Int,
    val title: String,
    val status: PackProgressStatus,
    val bestExamScore: Int
)

data class PowerUpInventory(
    val id: String,
    val title: String,
    val count: Int,
    val description: String
)

data class DeckCard(
    val cardId: String,
    val position: Int,
    val prompt: String,
    val canonicalAnswer: String,
    val acceptedAnswers: List<String>,
    val reading: String?,
    val meaning: String?,
    val difficulty: Int,
    val templateId: String,
    val resultScore: Int?,
    val isRetryQueued: Boolean
)

data class DeckSession(
    val deckRunId: String,
    val deckType: DeckType,
    val sourceId: String,
    val cards: List<DeckCard>,
    val submitted: Boolean
)

data class StrokeTemplate(
    val templateId: String,
    val target: String,
    val expectedStrokeCount: Int,
    val tolerance: Float,
    val strokes: List<TemplateStroke>
)

data class TemplateStroke(
    val points: List<TemplatePoint>
)

data class TemplatePoint(
    val x: Float,
    val y: Float
)

data class CardSubmission(
    val cardId: String,
    val cardDifficulty: Int,
    val score: Int,
    val handwritingScore: Int,
    val knowledgeScore: Int,
    val matchedAnswer: String,
    val canonicalAnswer: String,
    val isCanonicalMatch: Boolean,
    val requestedAssists: List<String>,
    val strokeCount: Int,
    val feedback: String
)

data class DeckResult(
    val deckRunId: String,
    val deckType: DeckType,
    val totalScore: Int,
    val grade: String,
    val passedThreshold: Boolean,
    val unlockedPackId: String?
)
