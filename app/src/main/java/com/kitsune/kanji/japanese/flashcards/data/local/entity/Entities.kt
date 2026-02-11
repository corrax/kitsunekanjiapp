package com.kitsune.kanji.japanese.flashcards.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CardType {
    KANJI_WRITE,
    VOCAB_READING,
    GRAMMAR_CHOICE,
    GRAMMAR_CLOZE_WRITE,
    SENTENCE_COMPREHENSION,
    SENTENCE_BUILD
}

enum class PackProgressStatus {
    LOCKED,
    UNLOCKED,
    PASSED
}

enum class DeckType {
    DAILY,
    EXAM,
    REMEDIAL
}

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val description: String,
    val accentColor: String,
    val displayOrder: Int
)

@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey val packId: String,
    val trackId: String,
    val level: Int,
    val title: String,
    val minTotalScore: Int,
    val minHandwritingScore: Int,
    val cardCount: Int,
    val displayOrder: Int
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val cardId: String,
    val type: CardType,
    val prompt: String,
    val canonicalAnswer: String,
    val acceptedAnswersRaw: String,
    val reading: String?,
    val meaning: String?,
    val promptFurigana: String?,
    val choicesRaw: String?,
    val difficulty: Int,
    val templateId: String
)

@Entity(tableName = "writing_templates")
data class WritingTemplateEntity(
    @PrimaryKey val templateId: String,
    val target: String,
    val expectedStrokeCount: Int,
    val tolerance: Float,
    val strokePaths: String
)

@Entity(tableName = "pack_cards", primaryKeys = ["packId", "cardId"])
data class PackCardCrossRef(
    val packId: String,
    val cardId: String,
    val position: Int
)

@Entity(tableName = "user_pack_progress")
data class UserPackProgressEntity(
    @PrimaryKey val packId: String,
    val status: PackProgressStatus,
    val bestExamScore: Int,
    val bestHandwritingScore: Int,
    val attemptCount: Int,
    val lastAttemptEpochMillis: Long?
)

@Entity(tableName = "deck_runs")
data class DeckRunEntity(
    @PrimaryKey val deckRunId: String,
    val deckType: DeckType,
    val sourceId: String,
    val startedAtEpochMillis: Long,
    val submittedAtEpochMillis: Long?,
    val totalScore: Int?
)

@Entity(tableName = "deck_run_cards", primaryKeys = ["deckRunId", "cardId"])
data class DeckRunCardEntity(
    val deckRunId: String,
    val cardId: String,
    val position: Int,
    val resultScore: Int?,
    val isRetryQueued: Boolean
)

@Entity(tableName = "card_attempts")
data class CardAttemptEntity(
    @PrimaryKey val attemptId: String,
    val deckRunId: String,
    val cardId: String,
    val createdAtEpochMillis: Long,
    val strokeCount: Int,
    val scoreTotal: Int,
    val scoreEffective: Int,
    val scoreHandwriting: Int,
    val scoreKnowledge: Int,
    val assistCount: Int,
    val assistsRaw: String,
    val matchedAnswer: String,
    val canonicalAnswer: String,
    val isCanonicalMatch: Boolean,
    val feedback: String
)

@Entity(tableName = "srs_state")
data class SrsStateEntity(
    @PrimaryKey val cardId: String,
    val strength: Int,
    val dueDateIso: String,
    val lastScore: Int,
    val lapseCount: Int
)

@Entity(tableName = "streak_state")
data class StreakStateEntity(
    @PrimaryKey val id: Int = 0,
    val currentStreak: Int,
    val bestStreak: Int,
    val lastCompletedDateIso: String?,
    val todayClaimedReward: Boolean
)

@Entity(tableName = "track_ability")
data class TrackAbilityEntity(
    @PrimaryKey val trackId: String,
    val abilityLevel: Float,
    val rollingScore: Float,
    val sampleCount: Int,
    val lastUpdatedEpochMillis: Long
)
