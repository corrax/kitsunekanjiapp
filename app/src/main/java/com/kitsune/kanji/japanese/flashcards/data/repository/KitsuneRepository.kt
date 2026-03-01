package com.kitsune.kanji.japanese.flashcards.data.repository

import androidx.room.withTransaction
import com.kitsune.kanji.japanese.flashcards.data.local.DailySchedulePreferences
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.EducationalGoal
import com.kitsune.kanji.japanese.flashcards.data.local.KitsuneDatabase
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import com.kitsune.kanji.japanese.flashcards.data.local.OnboardingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.PowerUpPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.dao.KitsuneDao
import com.kitsune.kanji.japanese.flashcards.data.local.dao.RetestAttemptContextRow
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardAttemptEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckRunCardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckRunEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.data.local.entity.SrsStateEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.StreakStateEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.TrackAbilityEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.UserPackProgressEntity
import com.kitsune.kanji.japanese.flashcards.data.seed.GoalAlignedSeedContent
import com.kitsune.kanji.japanese.flashcards.data.seed.N5SeedContent
import com.kitsune.kanji.japanese.flashcards.data.seed.SeedBundle
import com.kitsune.kanji.japanese.flashcards.domain.model.CardSubmission
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunCardReport
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunReport
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckResult
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckSession
import com.kitsune.kanji.japanese.flashcards.domain.model.HomeSnapshot
import com.kitsune.kanji.japanese.flashcards.domain.model.JlptLevelProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.KanjiAttemptHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.ActiveDeckRunProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.PackProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.StrokeTemplate
import com.kitsune.kanji.japanese.flashcards.domain.model.TemplatePoint
import com.kitsune.kanji.japanese.flashcards.domain.model.TemplateStroke
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import com.kitsune.kanji.japanese.flashcards.domain.scoring.ScoreBand
import com.kitsune.kanji.japanese.flashcards.domain.scoring.SCORE_REINFORCEMENT_CUTOFF
import com.kitsune.kanji.japanese.flashcards.domain.scoring.applyAssistPenalty
import com.kitsune.kanji.japanese.flashcards.domain.scoring.scoreBandFor
import com.kitsune.kanji.japanese.flashcards.domain.time.DailyChallengeClock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

interface KitsuneRepository {
    suspend fun initialize()
    suspend fun getHomeSnapshot(trackId: String): HomeSnapshot
    suspend fun getJlptLevelProgress(): List<JlptLevelProgress>
    suspend fun dismissDailyReminder()
    suspend fun createOrLoadDailyDeck(trackId: String): DeckSession
    suspend fun createOrLoadExamDeck(packId: String): DeckSession
    suspend fun loadDeck(deckRunId: String): DeckSession?
    suspend fun loadTemplate(templateId: String): StrokeTemplate?
    suspend fun loadTemplateForTarget(target: String): StrokeTemplate?
    suspend fun reorderDeckCards(deckRunId: String, cardIdsInOrder: List<String>)
    suspend fun submitCard(deckRunId: String, submission: CardSubmission)
    suspend fun submitDeck(deckRunId: String): DeckResult?
    suspend fun getDeckRunReport(deckRunId: String): DeckRunReport?
    suspend fun getKanjiAttemptHistory(limit: Int): List<KanjiAttemptHistoryItem>
    suspend fun getDeckRunHistory(limit: Int): List<DeckRunHistoryItem>
    suspend fun createRetestDeckForAttempt(attemptId: String): DeckSession
}

class KitsuneRepositoryImpl(
    private val database: KitsuneDatabase,
    private val dao: KitsuneDao,
    private val powerUpPreferences: PowerUpPreferences,
    private val dailySchedulePreferences: DailySchedulePreferences,
    private val onboardingPreferences: OnboardingPreferences,
    private val deckSelectionPreferences: DeckSelectionPreferences
) : KitsuneRepository {
    override suspend fun initialize() {
        val seed = mergeSeedBundles(
            primary = N5SeedContent.build(),
            secondary = GoalAlignedSeedContent.build()
        )
        if (dao.countCards() > 0) {
            database.withTransaction {
                dao.insertTracks(seed.tracks)
                dao.insertPacks(seed.packs)
                dao.insertCards(seed.cards)
                dao.insertTemplates(seed.templates)
                dao.insertPackCards(seed.packCards)

                // Only add progress rows for newly introduced packs.
                val existingProgress = dao.getPackProgress(seed.packs.map { it.packId })
                    .associateBy { it.packId }
                val missing = seed.progress.filter { it.packId !in existingProgress }
                if (missing.isNotEmpty()) {
                    dao.insertUserPackProgress(missing)
                }
            }
            ensureStreakState()
            ensurePlacementAndAbility()
            return
        }

        database.withTransaction {
            dao.insertTracks(seed.tracks)
            dao.insertPacks(seed.packs)
            dao.insertCards(seed.cards)
            dao.insertTemplates(seed.templates)
            dao.insertPackCards(seed.packCards)
            dao.insertUserPackProgress(seed.progress)
            dao.upsertStreakState(
                StreakStateEntity(
                    currentStreak = 0,
                    currentStreakScore = 0,
                    bestStreak = 0,
                    lastCompletedDateIso = null,
                    todayClaimedReward = false
                )
            )
        }
        ensurePlacementAndAbility()
    }

    override suspend fun getHomeSnapshot(trackId: String): HomeSnapshot {
        val today = cycleDate()
        val track = dao.getTracks().firstOrNull { it.trackId == trackId }
            ?: dao.getTracks().first()
        val packs = dao.getPacks(track.trackId)
        val progressMap = dao.getPackProgress(packs.map { it.packId }).associateBy { it.packId }
        val dailySource = dailySourceId(track.trackId, today)
        val hasStartedDailyChallenge = dao.getDeckRunBySource(
            deckType = DeckType.DAILY,
            sourceId = dailySource
        ) != null
        val dailyActiveRun = dao.getActiveDeckRunProgressBySources(
            deckType = DeckType.DAILY,
            sourceIds = listOf(dailySource)
        ).firstOrNull()?.toDomain()
        val activeExamRunsByPackId = dao.getActiveDeckRunProgressBySources(
            deckType = DeckType.EXAM,
            sourceIds = packs.map { it.packId }
        ).associateBy { it.sourceId }
        val shouldShowDailyReminder = !hasStartedDailyChallenge &&
            powerUpPreferences.shouldShowDailyReminder(today)
        val streak = dao.getStreakState() ?: StreakStateEntity(
            currentStreak = 0,
            currentStreakScore = 0,
            bestStreak = 0,
            lastCompletedDateIso = null,
            todayClaimedReward = false
        )

        val normalizedStreak = if (streak.lastCompletedDateIso != today.toString()) {
            streak.copy(todayClaimedReward = false)
        } else {
            streak
        }

        if (normalizedStreak != streak) {
            dao.upsertStreakState(normalizedStreak)
        }

        val packProgress = packs.map { pack ->
            val progress = progressMap[pack.packId]
            PackProgress(
                packId = pack.packId,
                level = pack.level,
                title = pack.title,
                status = progress?.status ?: PackProgressStatus.LOCKED,
                bestExamScore = progress?.bestExamScore ?: 0,
                activeRun = activeExamRunsByPackId[pack.packId]?.toDomain()
            )
        }
        val rankSummary = computeRankSummary(trackId = track.trackId)
        val accomplishment = dao.getScoreAccomplishmentSummary()
        val recentRuns = loadDeckRunHistory(limit = 20)

        return HomeSnapshot(
            trackId = track.trackId,
            trackTitle = track.title,
            currentStreak = normalizedStreak.currentStreak,
            currentStreakScore = normalizedStreak.currentStreakScore,
            bestStreak = normalizedStreak.bestStreak,
            hasStartedDailyChallenge = hasStartedDailyChallenge,
            dailyActiveRun = dailyActiveRun,
            shouldShowDailyReminder = shouldShowDailyReminder,
            rankSummary = rankSummary,
            packs = packProgress,
            lifetimeScore = accomplishment.lifetimeScore,
            lifetimeCardsReviewed = accomplishment.reviewedCards,
            recentRuns = recentRuns
        )
    }

    override suspend fun getJlptLevelProgress(): List<JlptLevelProgress> {
        return jlptTrackMapping.map { (level, trackId) ->
            val totalCount = dao.getTrackCardCount(trackId)
            val answeredCount = dao.getAttemptedCardCountForTrack(trackId)
                .coerceAtMost(totalCount)
            JlptLevelProgress(
                level = level,
                trackId = trackId,
                answeredCount = answeredCount,
                totalCount = totalCount
            )
        }
    }

    override suspend fun dismissDailyReminder() {
        val today = cycleDate()
        powerUpPreferences.markDailyReminderDismissed(today)
    }

    override suspend fun createOrLoadDailyDeck(trackId: String): DeckSession {
        val today = cycleDate()
        val sourceId = dailySourceId(trackId, today)
        val existing = dao.getActiveDeckRunBySource(DeckType.DAILY, sourceId)
        if (existing != null) {
            return loadDeck(existing.deckRunId) ?: error("Failed to load existing daily deck")
        }

        val track = dao.getTracks().firstOrNull { it.trackId == trackId }
            ?: dao.getTracks().first()
        val abilityState = loadOrCreateTrackAbility(track.trackId)
        val unlockedCards = dao.getUnlockedCardsForTrack(track.trackId)
        if (unlockedCards.isEmpty()) error("No unlocked cards available.")
        val availableTrackIds = dao.getTracks().map { it.trackId }.toSet()
        val relatedTracks = resolveDailyRelatedTracks(
            primaryTrackId = track.trackId,
            availableTrackIds = availableTrackIds
        )
        val preferredTopicCardsByTrack = relatedTracks.preferredTopicTrackIds.associateWith { topicTrackId ->
            dao.getUnlockedCardsForTrack(topicTrackId)
        }
        val crossTrackCards = relatedTracks.crossTrackIds
            .filterNot { it in relatedTracks.preferredTopicTrackIds }
            .flatMap { relatedTrackId ->
            dao.getUnlockedCardsForTrack(relatedTrackId)
        }
        val dailySeedCards = relatedTracks.dailySeedTrackIds.flatMap { dailyTrackId ->
            dao.getUnlockedCardsForTrack(dailyTrackId)
        }
        val retryCardIds = relatedTracks.reinforcementTrackIds
            .flatMap { reinforcementTrackId ->
                dao.getRetryCardIdsForTrack(
                    trackId = reinforcementTrackId,
                    retryBelowScore = SCORE_REINFORCEMENT_CUTOFF,
                    limit = 6
                )
            }
            .distinct()
            .take(14)
        val retryCards = dao.getCardsByIds(retryCardIds)
        val dueCount = dao.getDueCardCountForTrack(todayIso = today.toString(), trackId = track.trackId)
        val dueCards = dao.getCardsByIds(
            dao.getDueCardIdsForTrack(todayIso = today.toString(), trackId = track.trackId, limit = 24)
        )
        val attemptedIds = dao.getAttemptedCardIdsForTrack(trackId = track.trackId).toSet()
        val newCards = unlockedCards.filter { it.cardId !in attemptedIds }
        val chosenCards = pickDailyCards(
            today = today,
            unlockedCards = unlockedCards,
            retryCards = retryCards,
            dueCards = dueCards,
            newCards = newCards,
            crossTrackCards = crossTrackCards,
            preferredTopicCardsByTrack = preferredTopicCardsByTrack,
            dailySeedCards = dailySeedCards,
            abilityState = abilityState,
            dueCount = dueCount
        )

        val sequencedCards = applyCardTypeSequencing(chosenCards)

        return createDeckSession(
            deckType = DeckType.DAILY,
            sourceId = sourceId,
            cards = sequencedCards,
            retryCardIds = retryCardIds.toSet()
        )
    }

    override suspend fun createOrLoadExamDeck(packId: String): DeckSession {
        val pack = dao.getPackById(packId) ?: error("Pack not found.")
        val progress = dao.getSinglePackProgress(packId)
            ?: error("Pack progress not found.")
        if (progress.status == PackProgressStatus.LOCKED) {
            error("Pack is locked. Pass previous pack to unlock.")
        }

        val existing = dao.getActiveDeckRunBySource(DeckType.EXAM, sourceId = packId)
        if (existing != null) {
            return loadDeck(existing.deckRunId) ?: error("Failed to load existing exam deck.")
        }

        val cards = dao.getCardsForPack(pack.packId)
        if (cards.isEmpty()) {
            error("Pack has no cards.")
        }

        return createDeckSession(
            deckType = DeckType.EXAM,
            sourceId = packId,
            cards = cards
        )
    }

    override suspend fun loadDeck(deckRunId: String): DeckSession? {
        val deck = dao.getDeckRun(deckRunId) ?: return null
        val cards = dao.getDeckCards(deckRunId).map { row ->
            val acceptedAnswers = parseAcceptedAnswers(
                raw = row.acceptedAnswersRaw,
                fallback = row.canonicalAnswer
            )
            DeckCard(
                cardId = row.cardId,
                position = row.position,
                type = row.type,
                prompt = row.prompt,
                canonicalAnswer = row.canonicalAnswer,
                acceptedAnswers = acceptedAnswers,
                reading = row.reading,
                meaning = row.meaning,
                promptFurigana = row.promptFurigana,
                choices = parseChoices(row.choicesRaw),
                difficulty = row.difficulty,
                templateId = row.templateId,
                resultScore = row.resultScore,
                isRetryQueued = row.isRetryQueued
            )
        }
        return DeckSession(
            deckRunId = deckRunId,
            deckType = deck.deckType,
            sourceId = deck.sourceId,
            cards = cards,
            submitted = deck.submittedAtEpochMillis != null
        )
    }

    override suspend fun loadTemplate(templateId: String): StrokeTemplate? {
        val template = dao.getTemplateById(templateId) ?: return null
        return mapTemplate(template)
    }

    override suspend fun loadTemplateForTarget(target: String): StrokeTemplate? {
        val template = dao.getTemplateByTarget(target) ?: return null
        return mapTemplate(template)
    }

    override suspend fun reorderDeckCards(deckRunId: String, cardIdsInOrder: List<String>) {
        database.withTransaction {
            cardIdsInOrder.forEachIndexed { index, cardId ->
                dao.updateDeckCardPosition(deckRunId = deckRunId, cardId = cardId, position = index + 1)
            }
        }
    }

    private fun mapTemplate(
        template: com.kitsune.kanji.japanese.flashcards.data.local.entity.WritingTemplateEntity
    ): StrokeTemplate {
        return StrokeTemplate(
            templateId = template.templateId,
            target = template.target,
            expectedStrokeCount = template.expectedStrokeCount,
            tolerance = template.tolerance,
            strokes = parseStrokeTemplate(template.strokePaths)
        )
    }

    override suspend fun submitCard(deckRunId: String, submission: CardSubmission) {
        val today = cycleDate()
        val now = Instant.now().toEpochMilli()
        val deck = dao.getDeckRun(deckRunId) ?: error("Deck run missing")
        val dailyTrackId = resolveDailyTrackId(deck)
        val trackAbility = dailyTrackId?.let { loadOrCreateTrackAbility(it) }
        val appliedAssists = submission.requestedAssists
            .distinct()
            .filter { it.isNotBlank() }
        val adjustedScore = applyAssistPenalty(
            score = submission.score,
            assistCount = appliedAssists.size,
            cardDifficulty = submission.cardDifficulty,
            abilityLevel = trackAbility?.abilityLevel ?: 1.5f
        )
        val quality = qualityFor(adjustedScore)
        val current = dao.getSrsState(submission.cardId)
        val baseStrength = current?.strength ?: 0
        val nextStrength = (baseStrength + quality.strengthDelta).coerceIn(0, MAX_STRENGTH)
        val lapseCount = if (quality.isFailure) {
            (current?.lapseCount ?: 0) + 1
        } else {
            current?.lapseCount ?: 0
        }
        val nextDue = today.plusDays(
            intervalDaysFor(nextStrength = nextStrength, scoreQuality = quality)
        )
        val nextAbility = trackAbility?.let { currentAbility ->
            evolveAbility(
                current = currentAbility,
                effectiveScore = adjustedScore,
                cardDifficulty = submission.cardDifficulty,
                updatedAt = now
            )
        }
        val attempt = CardAttemptEntity(
            attemptId = UUID.randomUUID().toString(),
            deckRunId = deckRunId,
            cardId = submission.cardId,
            createdAtEpochMillis = now,
            strokeCount = submission.strokeCount,
            scoreTotal = adjustedScore,
            scoreEffective = adjustedScore,
            scoreHandwriting = submission.handwritingScore,
            scoreKnowledge = submission.knowledgeScore,
            assistCount = appliedAssists.size,
            assistsRaw = appliedAssists.joinToString("|"),
            matchedAnswer = submission.matchedAnswer,
            strokePathsRaw = submission.strokePathsRaw,
            canonicalAnswer = submission.canonicalAnswer,
            isCanonicalMatch = submission.isCanonicalMatch,
            feedback = submission.feedback
        )

        database.withTransaction {
            dao.updateDeckCardScore(deckRunId, submission.cardId, adjustedScore)
            dao.insertAttempt(attempt)
            dao.upsertSrsState(
                SrsStateEntity(
                    cardId = submission.cardId,
                    strength = nextStrength,
                    dueDateIso = nextDue.toString(),
                    lastScore = adjustedScore,
                    lapseCount = lapseCount
                )
            )
            if (deck.deckType == DeckType.DAILY && nextAbility != null) {
                dao.upsertTrackAbility(nextAbility)
            }
        }
    }

    override suspend fun submitDeck(deckRunId: String): DeckResult? {
        val today = cycleDate()
        val deck = dao.getDeckRun(deckRunId) ?: return null
        if (deck.submittedAtEpochMillis != null && deck.totalScore != null) {
            return buildDeckResult(
                deck = deck,
                totalScore = deck.totalScore,
                unlockedPackId = null
            )
        }

        val average = (dao.getDeckAverageScore(deckRunId) ?: 0.0).roundToInt()
        val now = Instant.now().toEpochMilli()
        var unlockedPackId: String? = null

        database.withTransaction {
            dao.submitDeck(deckRunId, now, average)
            if (deck.deckType == DeckType.DAILY) {
                applyDailyStreak(today, average)
            } else if (deck.deckType == DeckType.EXAM) {
                unlockedPackId = updateExamProgress(
                    packId = deck.sourceId,
                    score = average,
                    handwritingScore = average,
                    submittedAtEpochMillis = now
                )
            }
        }

        return buildDeckResult(
            deck = deck,
            totalScore = average,
            unlockedPackId = unlockedPackId
        )
    }

    override suspend fun getDeckRunReport(deckRunId: String): DeckRunReport? {
        val deck = dao.getDeckRun(deckRunId) ?: return null
        val cards = dao.getDeckRunReportCards(deckRunId)
        val totalScore = deck.totalScore ?: (dao.getDeckAverageScore(deckRunId) ?: 0.0).roundToInt()
        val totalCards = cards.size
        val reviewedCards = cards.count { it.resultScore != null }

        return DeckRunReport(
            deckRunId = deck.deckRunId,
            deckType = deck.deckType,
            sourceId = deck.sourceId,
            startedAtEpochMillis = deck.startedAtEpochMillis,
            submittedAtEpochMillis = deck.submittedAtEpochMillis,
            totalScore = totalScore,
            grade = gradeFor(totalScore),
            cardsReviewed = reviewedCards,
            totalCards = totalCards,
            cards = cards.map { row ->
                DeckRunCardReport(
                    cardId = row.cardId,
                    position = row.position,
                    type = row.type,
                    prompt = row.prompt,
                    canonicalAnswer = row.canonicalAnswer,
                    userAnswer = row.matchedAnswer,
                    score = row.resultScore,
                    effectiveScore = row.effectiveScore,
                    strokePathsRaw = row.strokePathsRaw,
                    comment = row.feedback
                )
            }
        )
    }

    override suspend fun getKanjiAttemptHistory(limit: Int): List<KanjiAttemptHistoryItem> {
        return dao.getCardAttemptHistory(
            cardType = CardType.KANJI_WRITE,
            limit = limit
        ).map { row ->
            KanjiAttemptHistoryItem(
                attemptId = row.attemptId,
                deckRunId = row.deckRunId,
                deckSourceId = row.sourceId,
                cardId = row.cardId,
                prompt = row.prompt,
                canonicalAnswer = row.canonicalAnswer,
                userAnswer = row.matchedAnswer,
                strokePathsRaw = row.strokePathsRaw,
                scoreTotal = row.scoreTotal,
                scoreEffective = row.scoreEffective,
                assistCount = row.assistCount,
                deckType = row.deckType,
                deckLabel = deckLabelForAttempt(row.deckType, row.sourceId, row.packTitle),
                attemptedAtEpochMillis = row.attemptedAtEpochMillis
            )
        }
    }

    override suspend fun getDeckRunHistory(limit: Int): List<DeckRunHistoryItem> {
        return loadDeckRunHistory(limit)
    }

    override suspend fun createRetestDeckForAttempt(attemptId: String): DeckSession {
        val context = dao.getRetestAttemptContext(attemptId)
            ?: error("Attempt not found.")
        return when (context.deckType) {
            DeckType.EXAM -> createOrLoadExamDeck(context.sourceId)
            DeckType.DAILY,
            DeckType.REMEDIAL -> createAdaptiveRetestDeck(context)
        }
    }

    private suspend fun loadDeckRunHistory(limit: Int): List<DeckRunHistoryItem> {
        return dao.getRecentDeckRunHistory(limit = limit)
            .map { row ->
                DeckRunHistoryItem(
                    deckRunId = row.deckRunId,
                    deckType = row.deckType,
                    sourceId = row.sourceId,
                    submittedAtEpochMillis = row.submittedAtEpochMillis,
                    totalScore = row.totalScore,
                    grade = gradeFor(row.totalScore),
                    cardsReviewed = row.cardsReviewed,
                    totalCards = row.totalCards
                )
            }
    }

    private suspend fun createAdaptiveRetestDeck(context: RetestAttemptContextRow): DeckSession {
        val sourceDeckCards = dao.getDeckCards(context.deckRunId)
        if (sourceDeckCards.isEmpty()) {
            error("Source deck has no cards.")
        }

        val sourceCardById = sourceDeckCards.associateBy { it.cardId }
        val sourceCards = dao.getCardsByIds(sourceDeckCards.map { it.cardId }).associateBy { it.cardId }
        val anchorSource = sourceCardById[context.cardId] ?: sourceDeckCards.first()
        val trackId = resolveRetestTrackId(
            deckType = context.deckType,
            sourceId = context.sourceId,
            fallbackCardId = anchorSource.cardId
        )
        val sourceId = remedialSourceId(trackId = trackId, attemptId = context.attemptId)
        val existing = dao.getActiveDeckRunBySource(deckType = DeckType.REMEDIAL, sourceId = sourceId)
        if (existing != null) {
            return loadDeck(existing.deckRunId) ?: error("Failed to load existing retest deck.")
        }

        val weakSourceRows = sourceDeckCards
            .filter { it.type == CardType.KANJI_WRITE }
            .sortedWith(
                compareBy<com.kitsune.kanji.japanese.flashcards.data.local.dao.DeckCardRow> {
                    it.resultScore ?: 0
                }.thenBy { it.position }
            )
            .filter { row ->
                row.cardId == context.cardId || (row.resultScore ?: 0) < RETEST_WEAK_SCORE_THRESHOLD
            }
        val weakCards = weakSourceRows
            .mapNotNull { row -> sourceCards[row.cardId] }
            .distinctBy { it.cardId }

        val unlockedCards = dao.getUnlockedCardsForTrack(trackId)
        val unlockedKanjiCards = unlockedCards.filter { it.type == CardType.KANJI_WRITE }
        val candidateCards = unlockedKanjiCards.ifEmpty { unlockedCards }
        if (candidateCards.isEmpty()) {
            error("No unlocked cards available for retest.")
        }
        val attemptedCardIds = dao.getAttemptedCardIdsForTrack(trackId).toSet()
        val weakCardIds = weakCards.map { it.cardId }.toSet()

        val referenceDifficulty = weakCards
            .map { it.difficulty.toFloat() }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
            ?: anchorSource.difficulty.toFloat()

        val targetDifficulty = (referenceDifficulty + 0.8f).coerceIn(1f, 12f)
        val newCandidatesPrimary = candidateCards
            .asSequence()
            .filter { it.cardId !in weakCardIds }
            .filter { it.cardId !in attemptedCardIds }
            .filter { it.difficulty >= referenceDifficulty.roundToInt() }
            .sortedBy { abs(it.difficulty - targetDifficulty) }
            .toList()
        val newCandidatesFallback = candidateCards
            .asSequence()
            .filter { it.cardId !in weakCardIds }
            .filter { it.cardId !in attemptedCardIds }
            .sortedBy { abs(it.difficulty - targetDifficulty) }
            .toList()
        val newCandidates = if (newCandidatesPrimary.isNotEmpty()) {
            newCandidatesPrimary
        } else {
            newCandidatesFallback
        }

        val practiceCandidates = candidateCards
            .asSequence()
            .filter { it.cardId !in weakCardIds }
            .sortedBy { abs(it.difficulty - targetDifficulty) }
            .toList()

        val random = Random(context.attemptId.hashCode())
        val selected = LinkedHashMap<String, CardEntity>()
        val targetSize = N5SeedContent.deckSizeDaily
        val weakQuota = min(8, weakCards.size.coerceAtLeast(1))
        val newQuota = 4

        appendCards(
            selected = selected,
            cards = weakCards.shuffled(random),
            quota = weakQuota,
            targetSize = targetSize
        )
        appendCards(
            selected = selected,
            cards = newCandidates.shuffled(random),
            quota = newQuota,
            targetSize = targetSize
        )
        appendCards(
            selected = selected,
            cards = practiceCandidates.shuffled(random),
            quota = targetSize,
            targetSize = targetSize
        )

        val selectedCards = selected.values.toList()
        if (selectedCards.isEmpty()) {
            error("Failed to build retest card set.")
        }
        return createDeckSession(
            deckType = DeckType.REMEDIAL,
            sourceId = sourceId,
            cards = selectedCards,
            retryCardIds = weakCardIds
        )
    }

    private suspend fun resolveRetestTrackId(
        deckType: DeckType,
        sourceId: String,
        fallbackCardId: String
    ): String {
        return when (deckType) {
            DeckType.DAILY -> sourceId.substringBefore("-daily-")
            DeckType.REMEDIAL -> sourceId.substringAfter("retest:", "").substringBefore(":", "")
            DeckType.EXAM -> ""
        }.ifBlank {
            dao.getTrackIdForCard(fallbackCardId) ?: error("Unable to resolve track for retest.")
        }
    }

    private fun remedialSourceId(trackId: String, attemptId: String): String {
        return "retest:$trackId:$attemptId"
    }

    private suspend fun applyDailyStreak(today: LocalDate, challengeScore: Int) {
        val state = dao.getStreakState() ?: StreakStateEntity(
            currentStreak = 0,
            currentStreakScore = 0,
            bestStreak = 0,
            lastCompletedDateIso = null,
            todayClaimedReward = false
        )

        val last = state.lastCompletedDateIso?.let { LocalDate.parse(it) }
        if (last == today) {
            return
        }

        val nextCurrent = if (last == today.minusDays(1)) state.currentStreak + 1 else 1
        val nextStreakScore = if (last == today.minusDays(1)) {
            state.currentStreakScore + challengeScore
        } else {
            challengeScore
        }
        val nextBest = max(state.bestStreak, nextCurrent)

        dao.upsertStreakState(
            state.copy(
                currentStreak = nextCurrent,
                currentStreakScore = nextStreakScore,
                bestStreak = nextBest,
                lastCompletedDateIso = today.toString(),
                todayClaimedReward = true
            )
        )
    }

    private suspend fun ensureStreakState() {
        if (dao.getStreakState() == null) {
            dao.upsertStreakState(
                StreakStateEntity(
                    currentStreak = 0,
                    currentStreakScore = 0,
                    bestStreak = 0,
                    lastCompletedDateIso = null,
                    todayClaimedReward = false
                )
            )
        }
    }

    private suspend fun createDeckSession(
        deckType: DeckType,
        sourceId: String,
        cards: List<com.kitsune.kanji.japanese.flashcards.data.local.entity.CardEntity>,
        retryCardIds: Set<String> = emptySet()
    ): DeckSession {
        val deckRunId = UUID.randomUUID().toString()
        val deckRun = DeckRunEntity(
            deckRunId = deckRunId,
            deckType = deckType,
            sourceId = sourceId,
            startedAtEpochMillis = Instant.now().toEpochMilli(),
            submittedAtEpochMillis = null,
            totalScore = null
        )
        val deckCards = cards.mapIndexed { index, card ->
            DeckRunCardEntity(
                deckRunId = deckRunId,
                cardId = card.cardId,
                position = index + 1,
                resultScore = null,
                isRetryQueued = card.cardId in retryCardIds
            )
        }

        database.withTransaction {
            dao.insertDeckRun(deckRun)
            dao.insertDeckRunCards(deckCards)
        }

        return loadDeck(deckRunId) ?: error("Failed to load generated deck.")
    }

    private suspend fun updateExamProgress(
        packId: String,
        score: Int,
        handwritingScore: Int,
        submittedAtEpochMillis: Long
    ): String? {
        if (dao.getPackById(packId) == null) return null
        val passed = didPassPack(totalScore = score)
        val current = dao.getSinglePackProgress(packId) ?: UserPackProgressEntity(
            packId = packId,
            status = PackProgressStatus.UNLOCKED,
            bestExamScore = 0,
            bestHandwritingScore = 0,
            attemptCount = 0,
            lastAttemptEpochMillis = null
        )

        dao.upsertPackProgress(
            current.copy(
                status = if (passed) PackProgressStatus.PASSED else PackProgressStatus.UNLOCKED,
                bestExamScore = max(current.bestExamScore, score),
                bestHandwritingScore = max(current.bestHandwritingScore, handwritingScore),
                attemptCount = current.attemptCount + 1,
                lastAttemptEpochMillis = submittedAtEpochMillis
            )
        )

        if (!passed) return null

        val nextPack = dao.getNextPack(packId) ?: return null
        val nextProgress = dao.getSinglePackProgress(nextPack.packId) ?: UserPackProgressEntity(
            packId = nextPack.packId,
            status = PackProgressStatus.LOCKED,
            bestExamScore = 0,
            bestHandwritingScore = 0,
            attemptCount = 0,
            lastAttemptEpochMillis = null
        )
        if (nextProgress.status == PackProgressStatus.LOCKED) {
            dao.upsertPackProgress(nextProgress.copy(status = PackProgressStatus.UNLOCKED))
            return nextPack.packId
        }

        return null
    }

    private suspend fun buildDeckResult(
        deck: DeckRunEntity,
        totalScore: Int,
        unlockedPackId: String?
    ): DeckResult {
        val passedThreshold = if (deck.deckType == DeckType.EXAM) {
            didPassPack(totalScore = totalScore)
        } else {
            true
        }

        return DeckResult(
            deckRunId = deck.deckRunId,
            deckType = deck.deckType,
            totalScore = totalScore,
            grade = gradeFor(totalScore),
            passedThreshold = passedThreshold,
            unlockedPackId = unlockedPackId
        )
    }

    private fun didPassPack(totalScore: Int): Boolean {
        return when (scoreBandFor(totalScore)) {
            ScoreBand.EXCELLENT,
            ScoreBand.GOOD -> true
            ScoreBand.OK,
            ScoreBand.INCORRECT -> false
        }
    }

    private fun pickDailyCards(
        today: LocalDate,
        unlockedCards: List<CardEntity>,
        retryCards: List<CardEntity>,
        dueCards: List<CardEntity>,
        newCards: List<CardEntity>,
        crossTrackCards: List<CardEntity>,
        preferredTopicCardsByTrack: Map<String, List<CardEntity>>,
        dailySeedCards: List<CardEntity>,
        abilityState: TrackAbilityEntity,
        dueCount: Int
    ): List<CardEntity> {
        val random = Random(today.toEpochDay())
        val rollingScore = abilityState.rollingScore
        val targetSize = resolveDailyDeckSize(
            base = N5SeedContent.deckSizeDaily,
            rollingScore = rollingScore,
            dueCount = dueCount
        )
        val selected = LinkedHashMap<String, CardEntity>()
        // Smooth quota curves: lerp between extremes based on rolling score
        val t = ((rollingScore - 50f) / 40f).coerceIn(0f, 1f) // 0 at score 50, 1 at score 90
        val retryQuota = (7f - 3f * t).toInt().coerceIn(3, 7)  // 7→4
        val minNew = (1f + 3f * t).toInt().coerceIn(1, 4)       // 1→4
        val dueQuota = when {
            dueCount >= 20 -> 8
            dueCount >= 12 -> 7
            else -> 6
        }
        val preferredTopicTrackCount = preferredTopicCardsByTrack.size
        val guaranteedPreferredTopicQuota = preferredTopicTrackCount.coerceAtMost(3)
        val preferredTopicQuota = if (preferredTopicTrackCount > 0) {
            (1f + 1f * t).toInt().coerceIn(1, 2)
        } else {
            0
        }
        val crossTrackQuota = (1f + 1f * t).toInt().coerceIn(1, 2)
        val dailySeedQuota = (2f + 1f * t).toInt().coerceIn(2, 3)
        val tChallenge = ((rollingScore - 70f) / 25f).coerceIn(0f, 1f) // 0 at 70, 1 at 95
        val challengeQuota = (0f + 3f * tChallenge).toInt().coerceIn(0, 3)
        val abilityLevel = abilityState.abilityLevel

        val challengeCandidates = unlockedCards
            .asSequence()
            .filter { it.difficulty >= (abilityLevel + 1.5f).roundToInt() }
            .sortedBy { abs(it.difficulty - (abilityLevel + 2f)) }
            .toList()

        val practiceCandidates = unlockedCards
            .sortedBy { abs(it.difficulty - abilityLevel) }
        val preferredTopicCandidatesByTrack = preferredTopicCardsByTrack.mapValues { (_, cards) ->
            cards.sortedBy { abs(it.difficulty - abilityLevel) }
        }
        val preferredTopicCandidates = preferredTopicCandidatesByTrack.values
            .flatten()
            .distinctBy { it.cardId }
        val crossTrackCandidates = crossTrackCards
            .sortedBy { abs(it.difficulty - abilityLevel) }
        val dailyCandidates = dailySeedCards
            .sortedBy { abs(it.difficulty - abilityLevel) }
        val blendedPracticeCandidates = (
            practiceCandidates +
                preferredTopicCandidates +
                crossTrackCandidates +
                dailyCandidates
            )
            .distinctBy { it.cardId }

        appendCards(selected, retryCards.shuffled(random), retryQuota, targetSize)
        appendCards(selected, dueCards.shuffled(random), dueQuota, targetSize)
        appendCards(
            selected = selected,
            cards = newCards.sortedBy { abs(it.difficulty - abilityLevel) }.shuffled(random),
            quota = minNew,
            targetSize = targetSize
        )
        preferredTopicCandidatesByTrack.keys
            .shuffled(random)
            .take(guaranteedPreferredTopicQuota)
            .forEach { trackId ->
                appendCards(
                    selected = selected,
                    cards = preferredTopicCandidatesByTrack[trackId].orEmpty().shuffled(random),
                    quota = 1,
                    targetSize = targetSize
                )
            }
        appendCards(
            selected = selected,
            cards = preferredTopicCandidates.shuffled(random),
            quota = preferredTopicQuota,
            targetSize = targetSize
        )
        appendCards(
            selected = selected,
            cards = dailyCandidates.shuffled(random),
            quota = dailySeedQuota,
            targetSize = targetSize
        )
        appendCards(
            selected = selected,
            cards = crossTrackCandidates.shuffled(random),
            quota = crossTrackQuota,
            targetSize = targetSize
        )
        appendCards(
            selected = selected,
            cards = challengeCandidates.shuffled(random),
            quota = challengeQuota,
            targetSize = targetSize
        )
        appendCards(selected, blendedPracticeCandidates.shuffled(random), targetSize, targetSize)
        return selected.values.toList()
    }

    /**
     * Reorder daily deck cards so that when multiple card types reference the
     * same kanji/concept, they follow a pedagogical progression:
     * recognition → reading → writing → grammar → sentence.
     * Cards without related siblings keep their original relative order.
     */
    private fun applyCardTypeSequencing(cards: List<CardEntity>): List<CardEntity> {
        val typeOrder = mapOf(
            CardType.VOCAB_READING to 0,
            CardType.KANJI_READING to 1,
            CardType.KANJI_WRITE to 2,
            CardType.GRAMMAR_CHOICE to 3,
            CardType.GRAMMAR_CLOZE_WRITE to 4,
            CardType.SENTENCE_COMPREHENSION to 5,
            CardType.SENTENCE_BUILD to 6
        )
        // Card IDs follow the pattern "{trackId}_{type}_{level}_{index}" where
        // type is v/r/g/c/s/b. Track IDs can contain underscores (e.g. jlpt_n5_core).
        // Cards sharing the same track+level+index are related concepts.
        // We strip the type code (3rd-from-last segment) to group them.
        val typeCodePattern = Regex("""^(.+)_([vrgcsb])_(\d+)_(\d+)$""")
        fun conceptKey(card: CardEntity): String {
            val match = typeCodePattern.matchEntire(card.cardId)
            return if (match != null) {
                // e.g. "jlpt_n5_core_v_1_1" → "jlpt_n5_core_1_1"
                "${match.groupValues[1]}_${match.groupValues[3]}_${match.groupValues[4]}"
            } else {
                card.cardId
            }
        }

        val groups = cards.groupBy(::conceptKey)
        // For groups with multiple cards, sort by type order; singles keep position
        val result = mutableListOf<CardEntity>()
        val visited = mutableSetOf<String>()
        for (card in cards) {
            if (card.cardId in visited) continue
            val key = conceptKey(card)
            val group = groups[key] ?: listOf(card)
            if (group.size > 1) {
                group.sortedBy { typeOrder[it.type] ?: 99 }.forEach {
                    if (it.cardId !in visited) {
                        result.add(it)
                        visited.add(it.cardId)
                    }
                }
            } else {
                result.add(card)
                visited.add(card.cardId)
            }
        }
        return result
    }

    private suspend fun resolveDailyRelatedTracks(
        primaryTrackId: String,
        availableTrackIds: Set<String>
    ): DailyTrackSelection {
        val goal = onboardingPreferences.getEducationalGoal()
        val learnerLevel = onboardingPreferences.getLearnerLevel()
        val preferredTopicTracks = deckSelectionPreferences.getSelectedTopicTrackIds()
            .filter { trackId ->
                trackId in availableTrackIds &&
                    trackId != primaryTrackId &&
                    trackId != GoalAlignedSeedContent.dailyChallengeTrackId
            }
        val goalTracks = when (goal) {
            EducationalGoal.CASUAL -> when (learnerLevel) {
                LearnerLevel.PRE_N5 -> listOf("foundations", "conversation")
                else -> listOf("conversation", "school", "work")
            }
            EducationalGoal.EVERYDAY_USE -> when (learnerLevel) {
                LearnerLevel.PRE_N5 -> listOf("foundations", "conversation", "daily_life_core")
                else -> listOf("conversation", "school", "work", N5SeedContent.trackId)
            }
            EducationalGoal.SCHOOL_OR_WORK -> when (learnerLevel) {
                LearnerLevel.PRE_N5 -> listOf("foundations", "school", "conversation")
                LearnerLevel.INTERMEDIATE_N3,
                LearnerLevel.ADVANCED_N2 -> listOf("work", "school", "conversation")

                LearnerLevel.BEGINNER_N5,
                LearnerLevel.BEGINNER_PLUS_N4,
                LearnerLevel.UNSURE -> listOf("school", "work", "conversation")
            }

            EducationalGoal.JLPT_OR_CLASSES -> when (learnerLevel) {
                LearnerLevel.PRE_N5 -> listOf("foundations", "jlpt_n5_core")

                LearnerLevel.BEGINNER_N5,
                LearnerLevel.UNSURE -> listOf("jlpt_n5_core", "jlpt_n4_core")

                LearnerLevel.BEGINNER_PLUS_N4 -> listOf("jlpt_n4_core", "jlpt_n5_core", "jlpt_n3_core")
                LearnerLevel.INTERMEDIATE_N3,
                LearnerLevel.ADVANCED_N2 -> listOf("jlpt_n3_core", "jlpt_n4_core", "jlpt_n5_core")
            }
        }

        val requestedCrossTrackIds = (preferredTopicTracks + goalTracks)
            .distinct()
            .filter { it != primaryTrackId }
            .take(5)
        val requested = (
            listOf(primaryTrackId) +
                requestedCrossTrackIds +
                GoalAlignedSeedContent.dailyChallengeTrackId
            )
            .distinct()
            .filter { it in availableTrackIds }
        val crossTrackIds = requested.filter { it != primaryTrackId && it != GoalAlignedSeedContent.dailyChallengeTrackId }
        val dailySeedTrackIds = requested.filter { it == GoalAlignedSeedContent.dailyChallengeTrackId }
        val reinforcementTrackIds = (listOf(primaryTrackId) + crossTrackIds + dailySeedTrackIds).distinct()
        return DailyTrackSelection(
            preferredTopicTrackIds = preferredTopicTracks,
            crossTrackIds = crossTrackIds,
            dailySeedTrackIds = dailySeedTrackIds,
            reinforcementTrackIds = reinforcementTrackIds
        )
    }

    private fun appendCards(
        selected: LinkedHashMap<String, CardEntity>,
        cards: List<CardEntity>,
        quota: Int,
        targetSize: Int
    ) {
        var taken = 0
        cards.forEach { card ->
            if (selected.size >= targetSize || taken >= quota) return@forEach
            if (selected.put(card.cardId, card) == null) {
                taken += 1
            }
        }
    }

    private fun resolveDailyDeckSize(base: Int, rollingScore: Float, dueCount: Int): Int {
        var extra = 0
        if (dueCount >= 12) extra = max(extra, 2)
        if (dueCount >= 20) extra = max(extra, 3)
        if (dueCount < 12 && rollingScore >= 90f) extra = max(extra, 1)
        return (base + extra).coerceIn(base, base + 3)
    }

    private fun gradeFor(score: Int): String {
        return when {
            score >= 95 -> "S"
            score >= 85 -> "A"
            score >= 75 -> "B"
            score >= 65 -> "C"
            else -> "D"
        }
    }

    private fun deckLabelForAttempt(deckType: DeckType, sourceId: String, packTitle: String?): String {
        return when (deckType) {
            DeckType.DAILY -> "Daily Challenge"
            DeckType.EXAM -> packTitle ?: "Exam Pack ($sourceId)"
            DeckType.REMEDIAL -> "Remedial ($sourceId)"
        }
    }

    private fun parseStrokeTemplate(encoded: String): List<TemplateStroke> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split('|')
            .mapNotNull { strokeChunk ->
                val points = strokeChunk.split(";")
                    .mapNotNull { pointChunk ->
                        val values = pointChunk.split(",")
                        if (values.size != 2) {
                            null
                        } else {
                            val x = values[0].toFloatOrNull()
                            val y = values[1].toFloatOrNull()
                            if (x == null || y == null) null else TemplatePoint(x = x, y = y)
                        }
                    }
                if (points.isEmpty()) null else TemplateStroke(points = points)
            }
    }

    private fun parseAcceptedAnswers(raw: String, fallback: String): List<String> {
        val parsed = raw.split('|')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val withCanonical = listOf(fallback) + parsed
        return withCanonical.distinct()
    }

    private fun parseChoices(raw: String?): List<String> {
        return raw.orEmpty()
            .split('|')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun com.kitsune.kanji.japanese.flashcards.data.local.dao.ActiveDeckRunProgressRow.toDomain(): ActiveDeckRunProgress {
        return ActiveDeckRunProgress(
            deckRunId = deckRunId,
            cardsReviewed = cardsReviewed,
            totalCards = totalCards
        )
    }

    private suspend fun computeRankSummary(trackId: String): UserRankSummary {
        val ability = loadOrCreateTrackAbility(trackId)
        val trackTotalWords = dao.getTrackCardCount(trackId).coerceAtLeast(1)
        val trackCoveredWords = dao.getAttemptedCardCountForTrack(trackId).coerceAtLeast(0)
        val coverageRatio = (trackCoveredWords.toFloat() / trackTotalWords.toFloat()).coerceIn(0f, 1f)
        val totalWords = dao.countCards().coerceAtLeast(1)
        val coveredWords = dao.getAttemptedCardCount().coerceAtLeast(0)
        val difficultySnapshot = dao.getDifficultyScoreSnapshotForTrack(
            trackId = trackId,
            easyMax = EASY_DIFFICULTY_MAX,
            hardMin = HARD_DIFFICULTY_MIN
        )
        val easyScore = difficultySnapshot?.easyAverage?.roundToInt()
        val hardScore = difficultySnapshot?.hardAverage?.roundToInt()
        val hardDelta = if (easyScore != null && hardScore != null) {
            hardScore - easyScore
        } else {
            0
        }

        val rating = (
            900f +
                (ability.abilityLevel * 72f) +
                (ability.rollingScore * 2.2f) +
                (coverageRatio * 240f) +
                (hardDelta * 4.5f)
            ).roundToInt()
            .coerceIn(800, 2600)

        val level = ((rating - 800) / 80 + 1).coerceIn(1, 30)
        return UserRankSummary(
            hiddenRating = rating,
            level = level,
            title = titleForLevel(level),
            wordsCovered = coveredWords,
            totalWords = totalWords,
            easyWordScore = easyScore,
            hardWordScore = hardScore
        )
    }

    private fun titleForLevel(level: Int): String {
        return when {
            level <= 2 -> "Fox Cub"
            level <= 4 -> "Ink Scout"
            level <= 6 -> "Stroke Initiate"
            level <= 9 -> "Kana Ranger"
            level <= 12 -> "Kanji Tracker"
            level <= 15 -> "Script Pathfinder"
            level <= 18 -> "Glyph Hunter"
            level <= 21 -> "Radical Tactician"
            level <= 24 -> "Kitsune Scholar"
            level <= 27 -> "Joyo Sentinel"
            else -> "Zen Scribe"
        }
    }

    private suspend fun ensurePlacementAndAbility() {
        val alreadyApplied = onboardingPreferences.isPlacementApplied()
        val tracks = dao.getTracks()
        if (tracks.isEmpty()) return
        val level = onboardingPreferences.getLearnerLevel()
        tracks.forEach { track ->
            reconcileTrackPackProgress(track.trackId)
            if (dao.getTrackAbility(track.trackId) == null) {
                val initialAbility = when (level) {
                    LearnerLevel.PRE_N5 -> 0.5f
                    LearnerLevel.BEGINNER_N5 -> 1.5f
                    LearnerLevel.BEGINNER_PLUS_N4 -> 3.0f
                    LearnerLevel.INTERMEDIATE_N3 -> 6.0f
                    LearnerLevel.ADVANCED_N2 -> 9.0f
                    LearnerLevel.UNSURE -> 2.0f
                }
                dao.upsertTrackAbility(
                    TrackAbilityEntity(
                        trackId = track.trackId,
                        abilityLevel = initialAbility,
                        rollingScore = 76f,
                        sampleCount = 0,
                        lastUpdatedEpochMillis = Instant.now().toEpochMilli()
                    )
                )
            }
        }

        if (!alreadyApplied) {
            onboardingPreferences.setPlacementApplied(true)
        }
    }

    private suspend fun reconcileTrackPackProgress(trackId: String) {
        val packs = dao.getPacks(trackId)
        var previousPackPassedGoodOrBetter = true
        packs.forEach { pack ->
            val current = dao.getSinglePackProgress(pack.packId) ?: UserPackProgressEntity(
                packId = pack.packId,
                status = PackProgressStatus.LOCKED,
                bestExamScore = 0,
                bestHandwritingScore = 0,
                attemptCount = 0,
                lastAttemptEpochMillis = null
            )
            val hasGoodOrBetter = current.bestExamScore > 0 && didPassPack(totalScore = current.bestExamScore)
            val nextStatus = when {
                !previousPackPassedGoodOrBetter -> PackProgressStatus.LOCKED
                hasGoodOrBetter -> PackProgressStatus.PASSED
                else -> PackProgressStatus.UNLOCKED
            }
            if (nextStatus != current.status) {
                dao.upsertPackProgress(current.copy(status = nextStatus))
            }
            previousPackPassedGoodOrBetter = nextStatus == PackProgressStatus.PASSED
        }
    }

    private suspend fun loadOrCreateTrackAbility(trackId: String): TrackAbilityEntity {
        return dao.getTrackAbility(trackId) ?: TrackAbilityEntity(
            trackId = trackId,
            abilityLevel = 1.5f,
            rollingScore = 76f,
            sampleCount = 0,
            lastUpdatedEpochMillis = Instant.now().toEpochMilli()
        ).also { dao.upsertTrackAbility(it) }
    }

    private fun qualityFor(effectiveScore: Int): ScoreQuality {
        return when {
            effectiveScore >= 80 -> ScoreQuality(strengthDelta = 2, intervalMultiplier = 1.3f, isFailure = false)
            effectiveScore >= 65 -> ScoreQuality(strengthDelta = 1, intervalMultiplier = 1f, isFailure = false)
            effectiveScore >= 55 -> ScoreQuality(strengthDelta = 0, intervalMultiplier = 0.8f, isFailure = false)
            effectiveScore >= 45 -> ScoreQuality(strengthDelta = -1, intervalMultiplier = 0.55f, isFailure = false)
            else -> ScoreQuality(strengthDelta = -2, intervalMultiplier = 0.32f, isFailure = true)
        }
    }

    private fun intervalDaysFor(nextStrength: Int, scoreQuality: ScoreQuality): Long {
        val baseDays = when (nextStrength) {
            0 -> 1f
            1 -> 2f
            2 -> 4f
            3 -> 7f
            4 -> 12f
            5 -> 18f
            6 -> 24f
            7 -> 32f
            else -> 45f
        }
        val days = (baseDays * scoreQuality.intervalMultiplier).roundToInt().coerceAtLeast(1)
        return days.toLong()
    }

    private fun evolveAbility(
        current: TrackAbilityEntity,
        effectiveScore: Int,
        cardDifficulty: Int,
        updatedAt: Long
    ): TrackAbilityEntity {
        val expected = (68f + ((current.abilityLevel - cardDifficulty) * 6f)).coerceIn(20f, 95f)
        val error = effectiveScore - expected
        val nextAbility = (current.abilityLevel + (error / 42f)).coerceIn(1f, 12f)
        val nextRolling = if (current.sampleCount <= 0) {
            effectiveScore.toFloat()
        } else {
            (current.rollingScore * 0.82f) + (effectiveScore * 0.18f)
        }.coerceIn(0f, 100f)
        return current.copy(
            abilityLevel = nextAbility,
            rollingScore = nextRolling,
            sampleCount = current.sampleCount + 1,
            lastUpdatedEpochMillis = updatedAt
        )
    }

    private suspend fun cycleDate(): LocalDate {
        val schedule = dailySchedulePreferences.getSchedule()
        return DailyChallengeClock.currentCycleDate(schedule = schedule)
    }

    private fun dailySourceId(trackId: String, today: LocalDate): String {
        return "$trackId-daily-$today"
    }

    private fun resolveDailyTrackId(deck: DeckRunEntity): String? {
        if (deck.deckType != DeckType.DAILY) return null
        return deck.sourceId.substringBefore("-daily-").takeIf { it.isNotBlank() }
    }

    private fun mergeSeedBundles(primary: SeedBundle, secondary: SeedBundle): SeedBundle {
        return SeedBundle(
            tracks = primary.tracks + secondary.tracks,
            packs = primary.packs + secondary.packs,
            cards = primary.cards + secondary.cards,
            templates = primary.templates + secondary.templates,
            packCards = primary.packCards + secondary.packCards,
            progress = primary.progress + secondary.progress
        )
    }

    private data class ScoreQuality(
        val strengthDelta: Int,
        val intervalMultiplier: Float,
        val isFailure: Boolean
    )

    private data class DailyTrackSelection(
        val preferredTopicTrackIds: List<String>,
        val crossTrackIds: List<String>,
        val dailySeedTrackIds: List<String>,
        val reinforcementTrackIds: List<String>
    )

    companion object {
        private const val MAX_STRENGTH = 8
        private const val EASY_DIFFICULTY_MAX = 4
        private const val HARD_DIFFICULTY_MIN = 8
        private const val RETEST_WEAK_SCORE_THRESHOLD = SCORE_REINFORCEMENT_CUTOFF
        private val jlptTrackMapping = listOf(
            "N5" to "jlpt_n5_core",
            "N4" to "jlpt_n4_core",
            "N3" to "jlpt_n3_core",
            "N2" to "jlpt_n2_core",
            "N1" to "jlpt_n1_core"
        )
    }
}
