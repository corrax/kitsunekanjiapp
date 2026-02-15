package com.kitsune.kanji.japanese.flashcards.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardAttemptEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckRunCardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckRunEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackCardCrossRef
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.SrsStateEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.StreakStateEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.TrackAbilityEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.TrackEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.UserPackProgressEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.WritingTemplateEntity

@Dao
interface KitsuneDao {
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun countCards(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(items: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacks(items: List<PackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(items: List<CardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackCards(items: List<PackCardCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(items: List<WritingTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPackProgress(items: List<UserPackProgressEntity>)

    @Query("SELECT * FROM tracks ORDER BY displayOrder")
    suspend fun getTracks(): List<TrackEntity>

    @Query("SELECT * FROM packs WHERE trackId = :trackId ORDER BY displayOrder")
    suspend fun getPacks(trackId: String): List<PackEntity>

    @Query("SELECT * FROM packs WHERE packId = :packId LIMIT 1")
    suspend fun getPackById(packId: String): PackEntity?

    @Query(
        """
        SELECT p2.* FROM packs p1
        INNER JOIN packs p2 ON p1.trackId = p2.trackId
        WHERE p1.packId = :packId AND p2.displayOrder = p1.displayOrder + 1
        LIMIT 1
        """
    )
    suspend fun getNextPack(packId: String): PackEntity?

    @Query("SELECT * FROM user_pack_progress WHERE packId IN (:packIds)")
    suspend fun getPackProgress(packIds: List<String>): List<UserPackProgressEntity>

    @Query("SELECT * FROM user_pack_progress WHERE packId = :packId LIMIT 1")
    suspend fun getSinglePackProgress(packId: String): UserPackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPackProgress(item: UserPackProgressEntity)

    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN pack_cards pc ON c.cardId = pc.cardId
        INNER JOIN user_pack_progress upp ON upp.packId = pc.packId
        INNER JOIN packs p ON p.packId = upp.packId
        WHERE upp.status IN ('UNLOCKED', 'PASSED')
          AND p.trackId = :trackId
        ORDER BY pc.position
        """
    )
    suspend fun getUnlockedCardsForTrack(trackId: String): List<CardEntity>

    @Query(
        """
        SELECT s.cardId
        FROM srs_state s
        INNER JOIN pack_cards pc ON pc.cardId = s.cardId
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE s.dueDateIso <= :todayIso
          AND p.trackId = :trackId
        ORDER BY s.dueDateIso
        LIMIT :limit
        """
    )
    suspend fun getDueCardIdsForTrack(todayIso: String, trackId: String, limit: Int): List<String>

    @Query(
        """
        SELECT COUNT(*)
        FROM srs_state s
        INNER JOIN pack_cards pc ON pc.cardId = s.cardId
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE s.dueDateIso <= :todayIso
          AND p.trackId = :trackId
        """
    )
    suspend fun getDueCardCountForTrack(todayIso: String, trackId: String): Int

    @Query(
        """
        SELECT ca.cardId
        FROM card_attempts ca
        INNER JOIN pack_cards pc2 ON pc2.cardId = ca.cardId
        INNER JOIN packs p2 ON p2.packId = pc2.packId
        INNER JOIN (
            SELECT cardId, MAX(createdAtEpochMillis) AS latestAttempt
            FROM card_attempts
            GROUP BY cardId
        ) latest
            ON latest.cardId = ca.cardId
           AND latest.latestAttempt = ca.createdAtEpochMillis
        WHERE ca.scoreEffective < :retryBelowScore
          AND p2.trackId = :trackId
        ORDER BY ca.createdAtEpochMillis DESC
        LIMIT :limit
        """
    )
    suspend fun getRetryCardIdsForTrack(trackId: String, retryBelowScore: Int, limit: Int): List<String>

    @Query("SELECT * FROM cards WHERE cardId IN (:cardIds)")
    suspend fun getCardsByIds(cardIds: List<String>): List<CardEntity>

    @Query(
        """
        SELECT p.trackId
        FROM pack_cards pc
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE pc.cardId = :cardId
        LIMIT 1
        """
    )
    suspend fun getTrackIdForCard(cardId: String): String?

    @Query(
        """
        SELECT DISTINCT ca.cardId
        FROM card_attempts ca
        INNER JOIN pack_cards pc ON pc.cardId = ca.cardId
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE p.trackId = :trackId
        """
    )
    suspend fun getAttemptedCardIdsForTrack(trackId: String): List<String>

    @Query(
        """
        SELECT COUNT(DISTINCT pc.cardId)
        FROM pack_cards pc
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE p.trackId = :trackId
        """
    )
    suspend fun getTrackCardCount(trackId: String): Int

    @Query(
        """
        SELECT COUNT(DISTINCT ca.cardId)
        FROM card_attempts ca
        INNER JOIN pack_cards pc ON pc.cardId = ca.cardId
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE p.trackId = :trackId
        """
    )
    suspend fun getAttemptedCardCountForTrack(trackId: String): Int

    @Query(
        """
        SELECT AVG(CASE WHEN c.difficulty <= :easyMax THEN ca.scoreEffective END) AS easyAverage,
               AVG(CASE WHEN c.difficulty >= :hardMin THEN ca.scoreEffective END) AS hardAverage
        FROM card_attempts ca
        INNER JOIN cards c ON c.cardId = ca.cardId
        INNER JOIN pack_cards pc ON pc.cardId = ca.cardId
        INNER JOIN packs p ON p.packId = pc.packId
        WHERE p.trackId = :trackId
        """
    )
    suspend fun getDifficultyScoreSnapshotForTrack(
        trackId: String,
        easyMax: Int,
        hardMin: Int
    ): DifficultyScoreSnapshotRow?

    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN pack_cards pc ON c.cardId = pc.cardId
        WHERE pc.packId = :packId
        ORDER BY pc.position
        """
    )
    suspend fun getCardsForPack(packId: String): List<CardEntity>

    @Query("SELECT * FROM writing_templates WHERE templateId = :templateId")
    suspend fun getTemplateById(templateId: String): WritingTemplateEntity?

    @Query("SELECT * FROM writing_templates WHERE target = :target LIMIT 1")
    suspend fun getTemplateByTarget(target: String): WritingTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckRun(item: DeckRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckRunCards(items: List<DeckRunCardEntity>)

    @Query("SELECT * FROM deck_runs WHERE deckType = :deckType AND sourceId = :sourceId LIMIT 1")
    suspend fun getDeckRunBySource(deckType: DeckType, sourceId: String): DeckRunEntity?

    @Query(
        """
        SELECT * FROM deck_runs
        WHERE deckType = :deckType
          AND sourceId = :sourceId
          AND submittedAtEpochMillis IS NULL
        LIMIT 1
        """
    )
    suspend fun getActiveDeckRunBySource(deckType: DeckType, sourceId: String): DeckRunEntity?

    @Query("SELECT * FROM deck_runs WHERE deckRunId = :deckRunId")
    suspend fun getDeckRun(deckRunId: String): DeckRunEntity?

    @Query(
        """
        SELECT drc.cardId AS cardId,
               drc.position AS position,
               drc.resultScore AS resultScore,
               drc.isRetryQueued AS isRetryQueued,
               c.type AS type,
               c.prompt AS prompt,
               c.canonicalAnswer AS canonicalAnswer,
               c.acceptedAnswersRaw AS acceptedAnswersRaw,
               c.reading AS reading,
               c.meaning AS meaning,
               c.promptFurigana AS promptFurigana,
               c.choicesRaw AS choicesRaw,
               c.difficulty AS difficulty,
               c.templateId AS templateId
        FROM deck_run_cards drc
        INNER JOIN cards c ON c.cardId = drc.cardId
        WHERE drc.deckRunId = :deckRunId
        ORDER BY drc.position
        """
    )
    suspend fun getDeckCards(deckRunId: String): List<DeckCardRow>

    @Query("UPDATE deck_run_cards SET resultScore = :resultScore WHERE deckRunId = :deckRunId AND cardId = :cardId")
    suspend fun updateDeckCardScore(deckRunId: String, cardId: String, resultScore: Int)

    @Query("UPDATE deck_run_cards SET position = :position WHERE deckRunId = :deckRunId AND cardId = :cardId")
    suspend fun updateDeckCardPosition(deckRunId: String, cardId: String, position: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(item: CardAttemptEntity)

    @Query(
        """
        SELECT COALESCE(SUM(scoreTotal), 0) AS lifetimeScore,
               COUNT(*) AS reviewedCards
        FROM card_attempts
        """
    )
    suspend fun getScoreAccomplishmentSummary(): ScoreAccomplishmentRow

    @Query(
        """
        SELECT dr.deckRunId AS deckRunId,
               dr.deckType AS deckType,
               dr.sourceId AS sourceId,
               dr.submittedAtEpochMillis AS submittedAtEpochMillis,
               COALESCE(dr.totalScore, 0) AS totalScore,
               COUNT(drc.cardId) AS totalCards,
               SUM(CASE WHEN drc.resultScore IS NOT NULL THEN 1 ELSE 0 END) AS cardsReviewed
        FROM deck_runs dr
        INNER JOIN deck_run_cards drc ON drc.deckRunId = dr.deckRunId
        WHERE dr.submittedAtEpochMillis IS NOT NULL
        GROUP BY dr.deckRunId
        ORDER BY dr.submittedAtEpochMillis DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentDeckRunHistory(limit: Int): List<DeckRunHistoryRow>

    @Query(
        """
        SELECT drc.cardId AS cardId,
               drc.position AS position,
               c.type AS type,
               c.prompt AS prompt,
               c.canonicalAnswer AS canonicalAnswer,
               drc.resultScore AS resultScore,
               ca.scoreEffective AS effectiveScore,
               ca.matchedAnswer AS matchedAnswer,
               ca.strokePathsRaw AS strokePathsRaw,
               ca.feedback AS feedback
        FROM deck_run_cards drc
        INNER JOIN cards c ON c.cardId = drc.cardId
        LEFT JOIN card_attempts ca
            ON ca.deckRunId = drc.deckRunId
            AND ca.cardId = drc.cardId
            AND ca.createdAtEpochMillis = (
                SELECT MAX(ca2.createdAtEpochMillis)
                FROM card_attempts ca2
                WHERE ca2.deckRunId = drc.deckRunId
                  AND ca2.cardId = drc.cardId
            )
        WHERE drc.deckRunId = :deckRunId
        ORDER BY drc.position
        """
    )
    suspend fun getDeckRunReportCards(deckRunId: String): List<DeckRunReportCardRow>

    @Query(
        """
        SELECT ca.attemptId AS attemptId,
               ca.deckRunId AS deckRunId,
               ca.cardId AS cardId,
               c.prompt AS prompt,
               c.canonicalAnswer AS canonicalAnswer,
               ca.matchedAnswer AS matchedAnswer,
               ca.strokePathsRaw AS strokePathsRaw,
               ca.scoreTotal AS scoreTotal,
               ca.scoreEffective AS scoreEffective,
               dr.deckType AS deckType,
               dr.sourceId AS sourceId,
               p.title AS packTitle,
               ca.createdAtEpochMillis AS attemptedAtEpochMillis
        FROM card_attempts ca
        INNER JOIN cards c ON c.cardId = ca.cardId
        INNER JOIN deck_runs dr ON dr.deckRunId = ca.deckRunId
        LEFT JOIN packs p
            ON dr.deckType = 'EXAM'
           AND p.packId = dr.sourceId
        WHERE c.type = :cardType
        ORDER BY ca.createdAtEpochMillis DESC
        LIMIT :limit
        """
    )
    suspend fun getCardAttemptHistory(
        cardType: CardType,
        limit: Int
    ): List<CardAttemptHistoryRow>

    @Query(
        """
        SELECT ca.attemptId AS attemptId,
               ca.deckRunId AS deckRunId,
               ca.cardId AS cardId,
               ca.scoreEffective AS scoreEffective,
               dr.deckType AS deckType,
               dr.sourceId AS sourceId
        FROM card_attempts ca
        INNER JOIN deck_runs dr ON dr.deckRunId = ca.deckRunId
        WHERE ca.attemptId = :attemptId
        LIMIT 1
        """
    )
    suspend fun getRetestAttemptContext(attemptId: String): RetestAttemptContextRow?

    @Query("SELECT * FROM srs_state WHERE cardId = :cardId")
    suspend fun getSrsState(cardId: String): SrsStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSrsState(item: SrsStateEntity)

    @Query("SELECT AVG(COALESCE(resultScore, 0)) FROM deck_run_cards WHERE deckRunId = :deckRunId")
    suspend fun getDeckAverageScore(deckRunId: String): Double?

    @Query("UPDATE deck_runs SET submittedAtEpochMillis = :submittedAtEpochMillis, totalScore = :totalScore WHERE deckRunId = :deckRunId")
    suspend fun submitDeck(deckRunId: String, submittedAtEpochMillis: Long, totalScore: Int)

    @Query("SELECT * FROM streak_state WHERE id = 0")
    suspend fun getStreakState(): StreakStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreakState(item: StreakStateEntity)

    @Query("SELECT * FROM track_ability WHERE trackId = :trackId LIMIT 1")
    suspend fun getTrackAbility(trackId: String): TrackAbilityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackAbility(item: TrackAbilityEntity)
}

data class DeckCardRow(
    val cardId: String,
    val position: Int,
    val resultScore: Int?,
    val isRetryQueued: Boolean,
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

data class DifficultyScoreSnapshotRow(
    val easyAverage: Double?,
    val hardAverage: Double?
)

data class ScoreAccomplishmentRow(
    val lifetimeScore: Int,
    val reviewedCards: Int
)

data class DeckRunHistoryRow(
    val deckRunId: String,
    val deckType: DeckType,
    val sourceId: String,
    val submittedAtEpochMillis: Long,
    val totalScore: Int,
    val cardsReviewed: Int,
    val totalCards: Int
)

data class DeckRunReportCardRow(
    val cardId: String,
    val position: Int,
    val type: CardType,
    val prompt: String,
    val canonicalAnswer: String,
    val resultScore: Int?,
    val effectiveScore: Int?,
    val matchedAnswer: String?,
    val strokePathsRaw: String?,
    val feedback: String?
)

data class CardAttemptHistoryRow(
    val attemptId: String,
    val deckRunId: String,
    val cardId: String,
    val prompt: String,
    val canonicalAnswer: String,
    val matchedAnswer: String,
    val strokePathsRaw: String?,
    val scoreTotal: Int,
    val scoreEffective: Int,
    val deckType: DeckType,
    val sourceId: String,
    val packTitle: String?,
    val attemptedAtEpochMillis: Long
)

data class RetestAttemptContextRow(
    val attemptId: String,
    val deckRunId: String,
    val cardId: String,
    val scoreEffective: Int,
    val deckType: DeckType,
    val sourceId: String
)
