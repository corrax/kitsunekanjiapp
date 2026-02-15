package com.kitsune.kanji.japanese.flashcards.data.seed

import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackCardCrossRef
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.data.local.entity.TrackEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.UserPackProgressEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.WritingTemplateEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

data class SeedBundle(
    val tracks: List<TrackEntity>,
    val packs: List<PackEntity>,
    val cards: List<CardEntity>,
    val templates: List<WritingTemplateEntity>,
    val packCards: List<PackCardCrossRef>,
    val progress: List<UserPackProgressEntity>
)

object N5SeedContent {
    const val trackId = "jlpt_n5_core"
    const val deckSizeDaily = 15
    const val totalKanji = 240
    private const val packSize = 20

    fun build(): SeedBundle {
        val n5 = buildN5Track()
        val school = buildSchoolTrack()
        val work = buildWorkTrack()
        val conversation = buildConversationTrack()

        return SeedBundle(
            tracks = listOf(n5.track, school.track, work.track, conversation.track),
            packs = n5.packs + school.packs + work.packs + conversation.packs,
            cards = n5.cards + school.cards + work.cards + conversation.cards,
            templates = n5.templates + school.templates + work.templates + conversation.templates,
            packCards = n5.packCards + school.packCards + work.packCards + conversation.packCards,
            progress = n5.progress + school.progress + work.progress + conversation.progress
        )
    }

    private data class TrackSeed(
        val track: TrackEntity,
        val packs: List<PackEntity>,
        val cards: List<CardEntity>,
        val templates: List<WritingTemplateEntity>,
        val packCards: List<PackCardCrossRef>,
        val progress: List<UserPackProgressEntity>
    )

    private fun buildN5Track(): TrackSeed {
        val kanji = rawKanji.take(totalKanji)

        require(kanji.size == totalKanji) {
            "Expected $totalKanji kanji for MVP seed, got ${kanji.size}"
        }
        require(kanji.distinct().size == totalKanji) {
            "Seed kanji list contains duplicates (expected $totalKanji unique items)."
        }
        require(n5EnglishPrompts.size >= totalKanji) {
            "Expected at least $totalKanji English prompts, got ${n5EnglishPrompts.size}."
        }

        val track = TrackEntity(
            trackId = trackId,
            title = "JLPT N5 Core",
            description = "Integrated vocab, kanji, grammar, and sentence training.",
            accentColor = "#C65D3B",
            displayOrder = 1
        )

        val baseKanjiCards = kanji.mapIndexed { index, symbol ->
            val id = "n5_${index + 1}"
            val englishPrompt = englishPromptFor(index)
            CardEntity(
                cardId = id,
                type = CardType.KANJI_WRITE,
                prompt = englishPrompt,
                canonicalAnswer = symbol,
                acceptedAnswersRaw = symbol,
                reading = null,
                meaning = englishPrompt,
                promptFurigana = null,
                choicesRaw = null,
                difficulty = (index / packSize) + 1,
                templateId = "tmpl_$id"
            )
        }

        val supplementalCards = buildSupplementalCards()
        val cards = baseKanjiCards + supplementalCards.map { it.card }

        val basePackCards = baseKanjiCards.mapIndexed { index, card ->
            val packNumber = (index / packSize) + 1
            PackCardCrossRef(
                packId = "n5_pack_$packNumber",
                cardId = card.cardId,
                position = (index % packSize) + 1
            )
        }
        val supplementalPackCards = supplementalCards.map { seed ->
            val positionOffset = seed.positionOffset.coerceAtLeast(1)
            PackCardCrossRef(
                packId = "n5_pack_${seed.packLevel}",
                cardId = seed.card.cardId,
                position = packSize + positionOffset
            )
        }
        val packCards = basePackCards + supplementalPackCards
        val packCardCountByPack = packCards
            .groupingBy { it.packId }
            .eachCount()

        val packs = (1..(totalKanji / packSize)).map { level ->
            val packId = "n5_pack_$level"
            PackEntity(
                packId = packId,
                trackId = trackId,
                level = level,
                title = "Pack $level",
                minTotalScore = 80,
                minHandwritingScore = 70,
                cardCount = packCardCountByPack[packId] ?: packSize,
                displayOrder = level
            )
        }

        val templates = cards
            .filter { it.type == CardType.KANJI_WRITE }
            .map { card ->
            val target = card.canonicalAnswer
            val expectedStrokeCount = strokeCountFor(target)
            WritingTemplateEntity(
                templateId = card.templateId,
                target = target,
                expectedStrokeCount = expectedStrokeCount,
                tolerance = 0.24f,
                strokePaths = buildTemplateStrokePaths(target, expectedStrokeCount)
            )
        }

        val progress = packs.map { pack ->
            UserPackProgressEntity(
                packId = pack.packId,
                status = if (pack.level == 1) PackProgressStatus.UNLOCKED else PackProgressStatus.LOCKED,
                bestExamScore = 0,
                bestHandwritingScore = 0,
                attemptCount = 0,
                lastAttemptEpochMillis = null
            )
        }

        return TrackSeed(
            track = track,
            packs = packs,
            cards = cards,
            templates = templates,
            packCards = packCards,
            progress = progress
        )
    }

    private data class ScenarioPack(
        val level: Int,
        val title: String,
        val cards: List<CardEntity>
    )

    private data class VocabSeed(
        val jp: String,
        val reading: String?,
        val meaning: String,
        val furigana: String? = null
    )

    private fun buildScenarioTrack(
        trackId: String,
        title: String,
        description: String,
        accentColor: String,
        displayOrder: Int,
        packs: List<ScenarioPack>
    ): TrackSeed {
        val enrichedPacks = packs.map { pack ->
            withScenarioKanjiReadingCard(trackId = trackId, pack = pack)
        }
        val track = TrackEntity(
            trackId = trackId,
            title = title,
            description = description,
            accentColor = accentColor,
            displayOrder = displayOrder
        )

        val packEntities = enrichedPacks.map { pack ->
            PackEntity(
                packId = packId(trackId, pack.level),
                trackId = trackId,
                level = pack.level,
                title = pack.title,
                minTotalScore = 78,
                minHandwritingScore = 0,
                cardCount = pack.cards.size,
                displayOrder = pack.level
            )
        }

        val cards = enrichedPacks.flatMap { it.cards }
        val packCards = enrichedPacks.flatMap { pack ->
            pack.cards.mapIndexed { index, card ->
                PackCardCrossRef(
                    packId = packId(trackId, pack.level),
                    cardId = card.cardId,
                    position = index + 1
                )
            }
        }

        val templates = cards
            .filter { it.type == CardType.KANJI_WRITE }
            .map { card ->
                val target = card.canonicalAnswer
                val expectedStrokeCount = strokeCountFor(target)
                WritingTemplateEntity(
                    templateId = card.templateId,
                    target = target,
                    expectedStrokeCount = expectedStrokeCount,
                    tolerance = 0.24f,
                    strokePaths = buildTemplateStrokePaths(target, expectedStrokeCount)
                )
            }

        val progress = packEntities.map { pack ->
            UserPackProgressEntity(
                packId = pack.packId,
                status = if (pack.level == 1) PackProgressStatus.UNLOCKED else PackProgressStatus.LOCKED,
                bestExamScore = 0,
                bestHandwritingScore = 0,
                attemptCount = 0,
                lastAttemptEpochMillis = null
            )
        }

        return TrackSeed(
            track = track,
            packs = packEntities,
            cards = cards,
            templates = templates,
            packCards = packCards,
            progress = progress
        )
    }

    private fun packId(trackId: String, level: Int): String = "${trackId}_pack_$level"

    private fun vocabCards(trackId: String, level: Int, items: List<VocabSeed>): List<CardEntity> {
        val pool = items.map { it.meaning }.distinct()
        return items.mapIndexed { index, item ->
            val cardId = "${trackId}_v_${level}_${index + 1}"
            CardEntity(
                cardId = cardId,
                type = CardType.VOCAB_READING,
                prompt = item.jp,
                canonicalAnswer = item.meaning,
                acceptedAnswersRaw = item.meaning,
                reading = item.reading,
                meaning = item.meaning,
                promptFurigana = item.furigana,
                choicesRaw = englishChoices(correct = item.meaning, pool = pool, key = cardId),
                difficulty = level,
                templateId = "tmpl_$cardId"
            )
        }
    }

    private fun withScenarioKanjiReadingCard(trackId: String, pack: ScenarioPack): ScenarioPack {
        val vocabulary = pack.cards.filter { card ->
            card.type == CardType.VOCAB_READING &&
                !card.reading.isNullOrBlank() &&
                containsKanji(card.prompt)
        }
        if (vocabulary.isEmpty()) return pack
        val source = vocabulary.first()
        val answer = source.reading.orEmpty().trim()
        if (answer.isBlank()) return pack
        val pool = (
            vocabulary.mapNotNull { it.reading?.trim() } +
                listOf("desu", "masu", "ka", "ni")
            )
            .filter { it.isNotBlank() }
            .distinct()
        val seed = (trackId.hashCode() xor pack.level).coerceAtLeast(1)
        val choices = (pool.shuffled(Random(seed)).take(3) + answer)
            .distinct()
            .shuffled(Random(seed xor 0x4D95A1F))
        val readingCard = CardEntity(
            cardId = "${trackId}_r_${pack.level}_1",
            type = CardType.KANJI_READING,
            prompt = source.prompt,
            canonicalAnswer = answer,
            acceptedAnswersRaw = listOf(answer, answer.lowercase(Locale.US)).distinct().joinToString("|"),
            reading = answer,
            meaning = "kanji reading",
            promptFurigana = source.promptFurigana,
            choicesRaw = choices.joinToString("|"),
            difficulty = pack.level,
            templateId = "tmpl_${trackId}_r_${pack.level}_1"
        )
        return pack.copy(cards = pack.cards + readingCard)
    }

    private fun containsKanji(text: String): Boolean {
        return text.any { character ->
            Character.UnicodeScript.of(character.code) == Character.UnicodeScript.HAN
        }
    }

    private fun englishChoices(correct: String, pool: List<String>, key: String): String {
        val normalizedPool = pool.distinct().filter { it.isNotBlank() }
        val others = normalizedPool.filter { it != correct }
        val seed = abs(key.hashCode()).coerceAtLeast(1)
        val random = Random(seed)
        val distractors = others.shuffled(random).take(3)
        val options = (distractors + correct)
            .distinct()
            .shuffled(Random(seed xor 0x5BD1E995.toInt()))
        return options.joinToString("|")
    }

    private fun grammarChoiceCard(
        trackId: String,
        level: Int,
        index: Int,
        prompt: String,
        promptFurigana: String?,
        answer: String,
        choices: List<String>,
        meaning: String
    ): CardEntity {
        val cardId = "${trackId}_g_${level}_${index + 1}"
        val normalizedChoices = (choices + answer).distinct()
        return CardEntity(
            cardId = cardId,
            type = CardType.GRAMMAR_CHOICE,
            prompt = prompt,
            canonicalAnswer = answer,
            acceptedAnswersRaw = answer,
            reading = null,
            meaning = meaning,
            promptFurigana = promptFurigana,
            choicesRaw = normalizedChoices.joinToString("|"),
            difficulty = level,
            templateId = "tmpl_$cardId"
        )
    }

    private fun grammarClozeCard(
        trackId: String,
        level: Int,
        index: Int,
        prompt: String,
        promptFurigana: String?,
        answer: String,
        accepted: List<String>,
        meaning: String
    ): CardEntity {
        val cardId = "${trackId}_c_${level}_${index + 1}"
        val acceptedRaw = (accepted + answer).distinct().joinToString("|")
        return CardEntity(
            cardId = cardId,
            type = CardType.GRAMMAR_CLOZE_WRITE,
            prompt = prompt,
            canonicalAnswer = answer,
            acceptedAnswersRaw = acceptedRaw,
            reading = null,
            meaning = meaning,
            promptFurigana = promptFurigana,
            choicesRaw = null,
            difficulty = level,
            templateId = "tmpl_$cardId"
        )
    }

    private fun sentenceComprehensionCard(
        trackId: String,
        level: Int,
        index: Int,
        prompt: String,
        promptFurigana: String?,
        answer: String,
        choices: List<String>,
        meaning: String
    ): CardEntity {
        val cardId = "${trackId}_s_${level}_${index + 1}"
        val normalizedChoices = (choices + answer).distinct()
        return CardEntity(
            cardId = cardId,
            type = CardType.SENTENCE_COMPREHENSION,
            prompt = prompt,
            canonicalAnswer = answer,
            acceptedAnswersRaw = answer,
            reading = null,
            meaning = meaning,
            promptFurigana = promptFurigana,
            choicesRaw = normalizedChoices.joinToString("|"),
            difficulty = level,
            templateId = "tmpl_$cardId"
        )
    }

    private fun sentenceBuildCard(
        trackId: String,
        level: Int,
        index: Int,
        prompt: String,
        answer: String,
        accepted: List<String>,
        meaning: String
    ): CardEntity {
        val cardId = "${trackId}_b_${level}_${index + 1}"
        val acceptedRaw = (accepted + answer).distinct().joinToString("|")
        return CardEntity(
            cardId = cardId,
            type = CardType.SENTENCE_BUILD,
            prompt = prompt,
            canonicalAnswer = answer,
            acceptedAnswersRaw = acceptedRaw,
            reading = null,
            meaning = meaning,
            promptFurigana = null,
            choicesRaw = null,
            difficulty = level,
            templateId = "tmpl_$cardId"
        )
    }

    private fun buildSchoolTrack(): TrackSeed {
        val packs = listOf(
            ScenarioPack(
                level = 1,
                title = "Classroom",
                cards = vocabCards(
                    trackId = "school",
                    level = 1,
                    items = listOf(
                        VocabSeed("学校", "がっこう", "school", "学校{がっこう}"),
                        VocabSeed("先生", "せんせい", "teacher", "先生{せんせい}"),
                        VocabSeed("学生", "がくせい", "student", "学生{がくせい}"),
                        VocabSeed("教室", "きょうしつ", "classroom", "教室{きょうしつ}"),
                        VocabSeed("机", "つくえ", "desk", "机{つくえ}"),
                        VocabSeed("椅子", "いす", "chair", "椅子{いす}"),
                        VocabSeed("ノート", null, "notebook", null),
                        VocabSeed("教科書", "きょうかしょ", "textbook", "教科書{きょうかしょ}"),
                        VocabSeed("宿題", "しゅくだい", "homework", "宿題{しゅくだい}"),
                        VocabSeed("授業", "じゅぎょう", "lesson", "授業{じゅぎょう}"),
                        VocabSeed("試験", "しけん", "exam", "試験{しけん}"),
                        VocabSeed("黒板", "こくばん", "blackboard", "黒板{こくばん}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "school",
                        level = 1,
                        index = 0,
                        prompt = "ここに名前を（  ）ください。",
                        promptFurigana = "ここに名前{なまえ}を（　）ください。",
                        answer = "書いて",
                        choices = listOf("書いて", "書く", "書かないで", "書いても"),
                        meaning = "te-form + ください (please do)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 1,
                        index = 1,
                        prompt = "一緒に勉強し（  ）。",
                        promptFurigana = "一緒{いっしょ}に勉強{べんきょう}し（　）。",
                        answer = "ましょう",
                        choices = listOf("ましょう", "ました", "ません", "たい"),
                        meaning = "ましょう (let's ...)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 1,
                        index = 2,
                        prompt = "これは私のペン（  ）。",
                        promptFurigana = "これは私{わたし}のペン（　）。",
                        answer = "です",
                        choices = listOf("です", "ます", "でした", "ません"),
                        meaning = "です (copula)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 1,
                        index = 0,
                        prompt = "先生、もう一度言って（      ）。",
                        promptFurigana = "先生{せんせい}、もう一度{いちど}言{い}って（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "te-form + ください (please do)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 1,
                        index = 1,
                        prompt = "明日、学校に行き（      ）。",
                        promptFurigana = "明日{あした}、学校{がっこう}に行{い}き（　　　）。",
                        answer = "ます",
                        accepted = listOf("ます", "ます。"),
                        meaning = "polite present/future"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 1,
                        index = 0,
                        prompt = "ここは教室です。",
                        promptFurigana = "ここは教室{きょうしつ}です。",
                        answer = "This is a classroom.",
                        choices = listOf(
                            "This is a school.",
                            "This is a teacher.",
                            "This is a classroom.",
                            "This is homework."
                        ),
                        meaning = "basic identification sentence"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 1,
                        index = 1,
                        prompt = "宿題を出してください。",
                        promptFurigana = "宿題{しゅくだい}を出{だ}してください。",
                        answer = "Please hand in your homework.",
                        choices = listOf(
                            "Please open the textbook.",
                            "Please hand in your homework.",
                            "Please write your name.",
                            "Please sit down."
                        ),
                        meaning = "request with ください"
                    ),
                    sentenceBuildCard(
                        trackId = "school",
                        level = 1,
                        index = 0,
                        prompt = "Say: I am a student.",
                        answer = "私は学生です",
                        accepted = listOf("私は学生です", "わたしは学生です"),
                        meaning = "self-introduction"
                    )
                )
            ),
            ScenarioPack(
                level = 2,
                title = "Homework",
                cards = vocabCards(
                    trackId = "school",
                    level = 2,
                    items = listOf(
                        VocabSeed("時間割", "じかんわり", "timetable", "時間割{じかんわり}"),
                        VocabSeed("休み時間", "やすみじかん", "break time", "休{やす}み時間{じかん}"),
                        VocabSeed("図書館", "としょかん", "library", "図書館{としょかん}"),
                        VocabSeed("部活", "ぶかつ", "club activities", "部活{ぶかつ}"),
                        VocabSeed("練習", "れんしゅう", "practice", "練習{れんしゅう}"),
                        VocabSeed("成績", "せいせき", "grades", "成績{せいせき}"),
                        VocabSeed("予定", "よてい", "plan", "予定{よてい}"),
                        VocabSeed("提出", "ていしゅつ", "submission", "提出{ていしゅつ}"),
                        VocabSeed("質問", "しつもん", "question", "質問{しつもん}"),
                        VocabSeed("答え", "こたえ", "answer", "答{こた}え"),
                        VocabSeed("例文", "れいぶん", "example sentence", "例文{れいぶん}"),
                        VocabSeed("連絡", "れんらく", "contact", "連絡{れんらく}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "school",
                        level = 2,
                        index = 0,
                        prompt = "ここで食べ（  ）いいですか。",
                        promptFurigana = "ここで食{た}べ（　）いいですか。",
                        answer = "ても",
                        choices = listOf("ても", "ては", "ないで", "から"),
                        meaning = "てもいい (may I ...?)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 2,
                        index = 1,
                        prompt = "宿題は今日（  ）明日までです。",
                        promptFurigana = "宿題{しゅくだい}は今日{きょう}（　）明日{あした}までです。",
                        answer = "から",
                        choices = listOf("から", "まで", "ので", "でも"),
                        meaning = "から (from)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 2,
                        index = 2,
                        prompt = "授業が終わった（  ）休みます。",
                        promptFurigana = "授業{じゅぎょう}が終{お}わった（　）休{やす}みます。",
                        answer = "ら",
                        choices = listOf("ら", "ので", "のに", "ながら"),
                        meaning = "たら (when/if)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 2,
                        index = 0,
                        prompt = "一緒に図書館に行き（      ）か。",
                        promptFurigana = "一緒{いっしょ}に図書館{としょかん}に行{い}き（　　　）か。",
                        answer = "ません",
                        accepted = listOf("ません"),
                        meaning = "ませんか (won't you ...?)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 2,
                        index = 1,
                        prompt = "宿題を忘れ（      ）ください。",
                        promptFurigana = "宿題{しゅくだい}を忘{わす}れ（　　　）ください。",
                        answer = "ないで",
                        accepted = listOf("ないで"),
                        meaning = "ないでください (please don't)"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 2,
                        index = 0,
                        prompt = "明日までに提出してください。",
                        promptFurigana = "明日{あした}までに提出{ていしゅつ}してください。",
                        answer = "Please submit it by tomorrow.",
                        choices = listOf(
                            "Please submit it by tomorrow.",
                            "Please submit it next week.",
                            "Please read it tomorrow.",
                            "Please write it tomorrow."
                        ),
                        meaning = "deadline with までに"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 2,
                        index = 1,
                        prompt = "質問があります。",
                        promptFurigana = "質問{しつもん}があります。",
                        answer = "I have a question.",
                        choices = listOf(
                            "I have a question.",
                            "I have an exam.",
                            "I have a textbook.",
                            "I have homework."
                        ),
                        meaning = "simple statement"
                    ),
                    sentenceBuildCard(
                        trackId = "school",
                        level = 2,
                        index = 0,
                        prompt = "Build: I will submit my homework today.",
                        answer = "今日宿題を提出します",
                        accepted = listOf("今日宿題を提出します", "今日、宿題を提出します"),
                        meaning = "submission sentence"
                    )
                )
            ),
            ScenarioPack(
                level = 3,
                title = "Rules & Requests",
                cards = vocabCards(
                    trackId = "school",
                    level = 3,
                    items = listOf(
                        VocabSeed("遅刻", "ちこく", "being late", "遅刻{ちこく}"),
                        VocabSeed("欠席", "けっせき", "absence", "欠席{けっせき}"),
                        VocabSeed("早退", "そうたい", "leaving early", "早退{そうたい}"),
                        VocabSeed("禁止", "きんし", "prohibition", "禁止{きんし}"),
                        VocabSeed("必要", "ひつよう", "necessary", "必要{ひつよう}"),
                        VocabSeed("大切", "たいせつ", "important", "大切{たいせつ}"),
                        VocabSeed("注意", "ちゅうい", "caution", "注意{ちゅうい}"),
                        VocabSeed("説明", "せつめい", "explanation", "説明{せつめい}"),
                        VocabSeed("理由", "りゆう", "reason", "理由{りゆう}"),
                        VocabSeed("許可", "きょか", "permission", "許可{きょか}"),
                        VocabSeed("校則", "こうそく", "school rules", "校則{こうそく}"),
                        VocabSeed("忘れ物", "わすれもの", "forgotten item", "忘{わす}れ物{もの}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "school",
                        level = 3,
                        index = 0,
                        prompt = "ここで携帯を使って（  ）。",
                        promptFurigana = "ここで携帯{けいたい}を使{つか}って（　）。",
                        answer = "はいけません",
                        choices = listOf("はいけません", "もいいです", "ください", "ませんか"),
                        meaning = "てはいけません (must not)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 3,
                        index = 1,
                        prompt = "毎日宿題をし（  ）なりません。",
                        promptFurigana = "毎日{まいにち}宿題{しゅくだい}をし（　）なりません。",
                        answer = "なければ",
                        choices = listOf("なければ", "ても", "ので", "ながら"),
                        meaning = "なければなりません (must)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 3,
                        index = 2,
                        prompt = "遅刻した（  ）、先生に言ってください。",
                        promptFurigana = "遅刻{ちこく}した（　）、先生{せんせい}に言{い}ってください。",
                        answer = "ら",
                        choices = listOf("ら", "ので", "ながら", "だけ"),
                        meaning = "たら (if/when)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 3,
                        index = 0,
                        prompt = "授業中は話しては（      ）。",
                        promptFurigana = "授業中{じゅぎょうちゅう}は話{はな}しては（　　　）。",
                        answer = "いけません",
                        accepted = listOf("いけません", "だめです"),
                        meaning = "prohibition"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 3,
                        index = 1,
                        prompt = "必要な本を持って（      ）。",
                        promptFurigana = "必要{ひつよう}な本{ほん}を持{も}って（　　　）。",
                        answer = "きてください",
                        accepted = listOf("きてください", "来てください"),
                        meaning = "please bring"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 3,
                        index = 0,
                        prompt = "ここでたばこを吸ってはいけません。",
                        promptFurigana = "ここでたばこを吸{す}ってはいけません。",
                        answer = "You must not smoke here.",
                        choices = listOf(
                            "You must not smoke here.",
                            "You may smoke here.",
                            "Please smoke here.",
                            "I don't smoke here."
                        ),
                        meaning = "てはいけません"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 3,
                        index = 1,
                        prompt = "遅刻しないでください。",
                        promptFurigana = "遅刻{ちこく}しないでください。",
                        answer = "Please don't be late.",
                        choices = listOf(
                            "Please don't be late.",
                            "Please be late.",
                            "Please don't speak.",
                            "Please don't run."
                        ),
                        meaning = "negative request"
                    ),
                    sentenceBuildCard(
                        trackId = "school",
                        level = 3,
                        index = 0,
                        prompt = "Build: I have to go to school tomorrow.",
                        answer = "明日学校に行かなければなりません",
                        accepted = listOf(
                            "明日学校に行かなければなりません",
                            "明日、学校に行かなければなりません",
                            "明日学校に行かなければならない"
                        ),
                        meaning = "must-do"
                    )
                )
            ),
            ScenarioPack(
                level = 4,
                title = "Clubs & Plans",
                cards = vocabCards(
                    trackId = "school",
                    level = 4,
                    items = listOf(
                        VocabSeed("体育館", "たいいくかん", "gym", "体育館{たいいくかん}"),
                        VocabSeed("運動", "うんどう", "exercise", "運動{うんどう}"),
                        VocabSeed("音楽", "おんがく", "music", "音楽{おんがく}"),
                        VocabSeed("映画", "えいが", "movie", "映画{えいが}"),
                        VocabSeed("旅行", "りょこう", "trip", "旅行{りょこう}"),
                        VocabSeed("参加", "さんか", "participation", "参加{さんか}"),
                        VocabSeed("用意", "ようい", "preparation", "用意{ようい}"),
                        VocabSeed("決める", "きめる", "decide", "決{き}める"),
                        VocabSeed("続ける", "つづける", "continue", "続{つづ}ける"),
                        VocabSeed("手伝う", "てつだう", "help", "手伝{てつだ}う"),
                        VocabSeed("練習", "れんしゅう", "practice", "練習{れんしゅう}"),
                        VocabSeed("準備", "じゅんび", "getting ready", "準備{じゅんび}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "school",
                        level = 4,
                        index = 0,
                        prompt = "音楽を聞き（  ）勉強します。",
                        promptFurigana = "音楽{おんがく}を聞{き}き（　）勉強{べんきょう}します。",
                        answer = "ながら",
                        choices = listOf("ながら", "ので", "だけ", "でも"),
                        meaning = "ながら (while ...)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 4,
                        index = 1,
                        prompt = "週末は映画を見（  ）本を読みます。",
                        promptFurigana = "週末{しゅうまつ}は映画{えいが}を見{み}（　）本{ほん}を読{よ}みます。",
                        answer = "たり",
                        choices = listOf("たり", "ながら", "から", "ので"),
                        meaning = "たり...たり (do things like ...)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 4,
                        index = 2,
                        prompt = "明日テストがある（  ）、勉強します。",
                        promptFurigana = "明日{あした}テストがある（　）、勉強{べんきょう}します。",
                        answer = "ので",
                        choices = listOf("ので", "でも", "だけ", "ながら"),
                        meaning = "ので (because)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 4,
                        index = 0,
                        prompt = "来週、図書館で勉強する（      ）です。",
                        promptFurigana = "来週{らいしゅう}、図書館{としょかん}で勉強{べんきょう}する（　　　）です。",
                        answer = "つもり",
                        accepted = listOf("つもり"),
                        meaning = "つもりです (intend to)"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 4,
                        index = 1,
                        prompt = "日本語が読める（      ）なりました。",
                        promptFurigana = "日本語{にほんご}が読{よ}める（　　　）なりました。",
                        answer = "ように",
                        accepted = listOf("ように"),
                        meaning = "ようになる (come to be able to)"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 4,
                        index = 0,
                        prompt = "部活が終わったら、帰ります。",
                        promptFurigana = "部活{ぶかつ}が終{お}わったら、帰{かえ}ります。",
                        answer = "After club ends, I go home.",
                        choices = listOf(
                            "After club ends, I go home.",
                            "While club ends, I go home.",
                            "I will not go home.",
                            "I go to club tomorrow."
                        ),
                        meaning = "たら (after/if)"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 4,
                        index = 1,
                        prompt = "明日は体育館で練習します。",
                        promptFurigana = "明日{あした}は体育館{たいいくかん}で練習{れんしゅう}します。",
                        answer = "Tomorrow I'll practice in the gym.",
                        choices = listOf(
                            "Tomorrow I'll practice in the gym.",
                            "Tomorrow I'll practice at home.",
                            "I practiced yesterday.",
                            "I will go to the library."
                        ),
                        meaning = "future plan"
                    ),
                    sentenceBuildCard(
                        trackId = "school",
                        level = 4,
                        index = 0,
                        prompt = "Build: I study while listening to music.",
                        answer = "音楽を聞きながら勉強します",
                        accepted = listOf("音楽を聞きながら勉強します"),
                        meaning = "ながら usage"
                    )
                )
            ),
            ScenarioPack(
                level = 5,
                title = "Dialogs",
                cards = vocabCards(
                    trackId = "school",
                    level = 5,
                    items = listOf(
                        VocabSeed("面接", "めんせつ", "interview", "面接{めんせつ}"),
                        VocabSeed("進学", "しんがく", "continuing education", "進学{しんがく}"),
                        VocabSeed("卒業", "そつぎょう", "graduation", "卒業{そつぎょう}"),
                        VocabSeed("入学", "にゅうがく", "enrollment", "入学{にゅうがく}"),
                        VocabSeed("相談", "そうだん", "consultation", "相談{そうだん}"),
                        VocabSeed("連絡先", "れんらくさき", "contact info", "連絡先{れんらくさき}"),
                        VocabSeed("場合", "ばあい", "case", "場合{ばあい}"),
                        VocabSeed("忘れ物", "わすれもの", "lost item", "忘{わす}れ物{もの}"),
                        VocabSeed("受付", "うけつけ", "reception", "受付{うけつけ}"),
                        VocabSeed("資料", "しりょう", "materials", "資料{しりょう}"),
                        VocabSeed("安心", "あんしん", "relief", "安心{あんしん}"),
                        VocabSeed("本当", "ほんとう", "really", "本当{ほんとう}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "school",
                        level = 5,
                        index = 0,
                        prompt = "この問題は難しい（  ）思います。",
                        promptFurigana = "この問題{もんだい}は難{むずか}しい（　）思{おも}います。",
                        answer = "と",
                        choices = listOf("と", "に", "で", "を"),
                        meaning = "と思います (I think that)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 5,
                        index = 1,
                        prompt = "わからない（  ）、質問してください。",
                        promptFurigana = "わからない（　）、質問{しつもん}してください。",
                        answer = "ときは",
                        choices = listOf("ときは", "ので", "だけ", "ながら"),
                        meaning = "とき (when)"
                    ),
                    grammarChoiceCard(
                        trackId = "school",
                        level = 5,
                        index = 2,
                        prompt = "宿題を忘れてしまって（  ）。",
                        promptFurigana = "宿題{しゅくだい}を忘{わす}れてしまって（　）。",
                        answer = "すみません",
                        choices = listOf("すみません", "ましょう", "ください", "でした"),
                        meaning = "apology"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 5,
                        index = 0,
                        prompt = "明日来られない（      ）、メールしてください。",
                        promptFurigana = "明日{あした}来{こ}られない（　　　）、メールしてください。",
                        answer = "ときは",
                        accepted = listOf("とき", "ときは"),
                        meaning = "when/if"
                    ),
                    grammarClozeCard(
                        trackId = "school",
                        level = 5,
                        index = 1,
                        prompt = "もう一度説明していただけ（      ）か。",
                        promptFurigana = "もう一度{いちど}説明{せつめい}していただけ（　　　）か。",
                        answer = "ません",
                        accepted = listOf("ません"),
                        meaning = "polite request"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 5,
                        index = 0,
                        prompt = "すみません、宿題を忘れてしまいました。",
                        promptFurigana = "すみません、宿題{しゅくだい}を忘{わす}れてしまいました。",
                        answer = "Sorry, I forgot my homework.",
                        choices = listOf(
                            "Sorry, I forgot my homework.",
                            "Sorry, I did my homework.",
                            "Sorry, I lost my textbook.",
                            "Sorry, I was early."
                        ),
                        meaning = "てしまう (ended up)"
                    ),
                    sentenceComprehensionCard(
                        trackId = "school",
                        level = 5,
                        index = 1,
                        prompt = "わからないところがあれば、質問してください。",
                        promptFurigana = "わからないところがあれば、質問{しつもん}してください。",
                        answer = "If there are parts you don't understand, please ask.",
                        choices = listOf(
                            "If there are parts you don't understand, please ask.",
                            "If you understand, don't ask.",
                            "Please don't ask questions.",
                            "You must answer now."
                        ),
                        meaning = "conditional"
                    ),
                    sentenceBuildCard(
                        trackId = "school",
                        level = 5,
                        index = 0,
                        prompt = "Build: If you don't understand, please ask.",
                        answer = "わからないときは質問してください",
                        accepted = listOf("わからないときは質問してください", "わからない時は質問してください"),
                        meaning = "とき sentence"
                    )
                )
            )
        )

        return buildScenarioTrack(
            trackId = "school",
            title = "School Situations",
            description = "JLPT-aligned school vocab, grammar, and dialogues.",
            accentColor = "#4D7A67",
            displayOrder = 2,
            packs = packs
        )
    }

    private fun buildWorkTrack(): TrackSeed {
        val packs = listOf(
            ScenarioPack(
                level = 1,
                title = "Office Basics",
                cards = vocabCards(
                    trackId = "work",
                    level = 1,
                    items = listOf(
                        VocabSeed("会社", "かいしゃ", "company", "会社{かいしゃ}"),
                        VocabSeed("仕事", "しごと", "work", "仕事{しごと}"),
                        VocabSeed("社員", "しゃいん", "employee", "社員{しゃいん}"),
                        VocabSeed("上司", "じょうし", "boss", "上司{じょうし}"),
                        VocabSeed("同僚", "どうりょう", "coworker", "同僚{どうりょう}"),
                        VocabSeed("会議", "かいぎ", "meeting", "会議{かいぎ}"),
                        VocabSeed("部署", "ぶしょ", "department", "部署{ぶしょ}"),
                        VocabSeed("名刺", "めいし", "business card", "名刺{めいし}"),
                        VocabSeed("電話", "でんわ", "phone", "電話{でんわ}"),
                        VocabSeed("メール", null, "email", null),
                        VocabSeed("受付", "うけつけ", "reception", "受付{うけつけ}"),
                        VocabSeed("休憩", "きゅうけい", "break", "休憩{きゅうけい}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "work",
                        level = 1,
                        index = 0,
                        prompt = "会議は三時に始まり（  ）。",
                        promptFurigana = "会議{かいぎ}は三時{さんじ}に始{はじ}まり（　）。",
                        answer = "ます",
                        choices = listOf("ます", "ました", "ません", "たい"),
                        meaning = "polite present/future"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 1,
                        index = 1,
                        prompt = "こちらに名前を（  ）ください。",
                        promptFurigana = "こちらに名前{なまえ}を（　）ください。",
                        answer = "書いて",
                        choices = listOf("書いて", "書く", "書かないで", "書いても"),
                        meaning = "te-form + ください"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 1,
                        index = 2,
                        prompt = "今お時間が（  ）か。",
                        promptFurigana = "今{いま}お時間{じかん}が（　）か。",
                        answer = "あります",
                        choices = listOf("あります", "います", "します", "なります"),
                        meaning = "exist (inanimate)"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 1,
                        index = 0,
                        prompt = "メールを送って（      ）。",
                        promptFurigana = "メールを送{おく}って（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "request"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 1,
                        index = 1,
                        prompt = "今日の会議に参加し（      ）。",
                        promptFurigana = "今日{きょう}の会議{かいぎ}に参加{さんか}し（　　　）。",
                        answer = "ます",
                        accepted = listOf("ます", "ます。"),
                        meaning = "polite form"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 1,
                        index = 0,
                        prompt = "会議は三時からです。",
                        promptFurigana = "会議{かいぎ}は三時{さんじ}からです。",
                        answer = "The meeting is from 3 o'clock.",
                        choices = listOf(
                            "The meeting is from 3 o'clock.",
                            "The meeting is at 3 o'clock yesterday.",
                            "The meeting is at home.",
                            "The meeting is canceled."
                        ),
                        meaning = "time expression"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 1,
                        index = 1,
                        prompt = "少々お待ちください。",
                        promptFurigana = "少々{しょうしょう}お待{ま}ちください。",
                        answer = "Please wait a moment.",
                        choices = listOf(
                            "Please wait a moment.",
                            "Please call me.",
                            "Please sit down.",
                            "Please go now."
                        ),
                        meaning = "set phrase"
                    ),
                    sentenceBuildCard(
                        trackId = "work",
                        level = 1,
                        index = 0,
                        prompt = "Build: I work at a company.",
                        answer = "私は会社で働きます",
                        accepted = listOf("私は会社で働きます", "わたしは会社で働きます"),
                        meaning = "work statement"
                    )
                )
            ),
            ScenarioPack(
                level = 2,
                title = "Meetings",
                cards = vocabCards(
                    trackId = "work",
                    level = 2,
                    items = listOf(
                        VocabSeed("予定", "よてい", "schedule", "予定{よてい}"),
                        VocabSeed("打ち合わせ", "うちあわせ", "discussion", "打{う}ち合{あ}わせ"),
                        VocabSeed("資料", "しりょう", "materials", "資料{しりょう}"),
                        VocabSeed("報告", "ほうこく", "report", "報告{ほうこく}"),
                        VocabSeed("連絡", "れんらく", "contact", "連絡{れんらく}"),
                        VocabSeed("相談", "そうだん", "consultation", "相談{そうだん}"),
                        VocabSeed("変更", "へんこう", "change", "変更{へんこう}"),
                        VocabSeed("確認", "かくにん", "confirmation", "確認{かくにん}"),
                        VocabSeed("開始", "かいし", "start", "開始{かいし}"),
                        VocabSeed("終了", "しゅうりょう", "end", "終了{しゅうりょう}"),
                        VocabSeed("間に合う", "まにあう", "make it in time", "間{ま}に合{あ}う"),
                        VocabSeed("遅れる", "おくれる", "be late", "遅{おく}れる")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "work",
                        level = 2,
                        index = 0,
                        prompt = "金曜日までに（  ）ください。",
                        promptFurigana = "金曜日{きんようび}までに（　）ください。",
                        answer = "提出して",
                        choices = listOf("提出して", "提出すると", "提出した", "提出しないで"),
                        meaning = "until/by deadline + ください"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 2,
                        index = 1,
                        prompt = "予定を変更しても（  ）ですか。",
                        promptFurigana = "予定{よてい}を変更{へんこう}しても（　）ですか。",
                        answer = "いい",
                        choices = listOf("いい", "だめ", "ます", "でしょう"),
                        meaning = "permission"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 2,
                        index = 2,
                        prompt = "会議が終わった（  ）、連絡します。",
                        promptFurigana = "会議{かいぎ}が終{お}わった（　）、連絡{れんらく}します。",
                        answer = "ら",
                        choices = listOf("ら", "ので", "ながら", "だけ"),
                        meaning = "たら"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 2,
                        index = 0,
                        prompt = "明日までに確認して（      ）。",
                        promptFurigana = "明日{あした}までに確認{かくにん}して（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "request"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 2,
                        index = 1,
                        prompt = "少し遅れ（      ）。",
                        promptFurigana = "少{すこ}し遅{おく}れ（　　　）。",
                        answer = "ます",
                        accepted = listOf("ます", "ます。"),
                        meaning = "polite statement"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 2,
                        index = 0,
                        prompt = "予定が変更になりました。",
                        promptFurigana = "予定{よてい}が変更{へんこう}になりました。",
                        answer = "The schedule has changed.",
                        choices = listOf(
                            "The schedule has changed.",
                            "The schedule is the same.",
                            "The meeting is finished.",
                            "The report is done."
                        ),
                        meaning = "change notice"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 2,
                        index = 1,
                        prompt = "少し遅れます。",
                        promptFurigana = "少{すこ}し遅{おく}れます。",
                        answer = "I'll be a bit late.",
                        choices = listOf(
                            "I'll be a bit late.",
                            "I'll arrive early.",
                            "I can't go.",
                            "I'm leaving now."
                        ),
                        meaning = "late notice"
                    ),
                    sentenceBuildCard(
                        trackId = "work",
                        level = 2,
                        index = 0,
                        prompt = "Build: Please confirm the details.",
                        answer = "詳細を確認してください",
                        accepted = listOf("詳細を確認してください", "詳細を確認して下さい"),
                        meaning = "request"
                    )
                )
            ),
            ScenarioPack(
                level = 3,
                title = "Requests",
                cards = vocabCards(
                    trackId = "work",
                    level = 3,
                    items = listOf(
                        VocabSeed("お願い", "おねがい", "request", "お願い{おねがい}"),
                        VocabSeed("手伝う", "てつだう", "help", "手伝{てつだ}う"),
                        VocabSeed("準備", "じゅんび", "preparation", "準備{じゅんび}"),
                        VocabSeed("対応", "たいおう", "handling", "対応{たいおう}"),
                        VocabSeed("可能", "かのう", "possible", "可能{かのう}"),
                        VocabSeed("必要", "ひつよう", "necessary", "必要{ひつよう}"),
                        VocabSeed("返信", "へんしん", "reply", "返信{へんしん}"),
                        VocabSeed("修正", "しゅうせい", "revision", "修正{しゅうせい}"),
                        VocabSeed("送る", "おくる", "send", "送{おく}る"),
                        VocabSeed("受け取る", "うけとる", "receive", "受{う}け取{と}る"),
                        VocabSeed("確認", "かくにん", "confirm", "確認{かくにん}"),
                        VocabSeed("急ぐ", "いそぐ", "hurry", "急{いそ}ぐ")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "work",
                        level = 3,
                        index = 0,
                        prompt = "この資料を見ていただけ（  ）か。",
                        promptFurigana = "この資料{しりょう}を見{み}ていただけ（　）か。",
                        answer = "ません",
                        choices = listOf("ません", "ます", "ない", "ましょう"),
                        meaning = "polite request"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 3,
                        index = 1,
                        prompt = "今から少し相談しても（  ）ですか。",
                        promptFurigana = "今{いま}から少{すこ}し相談{そうだん}しても（　）ですか。",
                        answer = "いい",
                        choices = listOf("いい", "だめ", "ます", "でしょう"),
                        meaning = "permission"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 3,
                        index = 2,
                        prompt = "可能なら、今日中にお願いし（  ）。",
                        promptFurigana = "可能{かのう}なら、今日中{きょうじゅう}にお願いし（　）。",
                        answer = "ます",
                        choices = listOf("ます", "ました", "ません", "たい"),
                        meaning = "polite request"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 3,
                        index = 0,
                        prompt = "申し訳ありませんが、少々お待ちいただけ（      ）か。",
                        promptFurigana = "申{もう}し訳{わけ}ありませんが、少々{しょうしょう}お待{ま}ちいただけ（　　　）か。",
                        answer = "ません",
                        accepted = listOf("ません"),
                        meaning = "polite request"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 3,
                        index = 1,
                        prompt = "今週中に返信して（      ）。",
                        promptFurigana = "今週中{こんしゅうちゅう}に返信{へんしん}して（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "request"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 3,
                        index = 0,
                        prompt = "手伝いましょうか。",
                        promptFurigana = "手伝{てつだ}いましょうか。",
                        answer = "Shall I help?",
                        choices = listOf(
                            "Shall I help?",
                            "I can't help.",
                            "I helped yesterday.",
                            "Please help me."
                        ),
                        meaning = "offer"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 3,
                        index = 1,
                        prompt = "可能なら、今日中にお願いします。",
                        promptFurigana = "可能{かのう}なら、今日中{きょうじゅう}にお願いします。",
                        answer = "If possible, please do it by today.",
                        choices = listOf(
                            "If possible, please do it by today.",
                            "If possible, please do it next week.",
                            "It is impossible today.",
                            "Please don't do it."
                        ),
                        meaning = "conditional request"
                    ),
                    sentenceBuildCard(
                        trackId = "work",
                        level = 3,
                        index = 0,
                        prompt = "Build: Could you send me an email?",
                        answer = "メールを送っていただけませんか",
                        accepted = listOf("メールを送っていただけませんか", "メールを送ってもらえませんか"),
                        meaning = "polite request"
                    )
                )
            ),
            ScenarioPack(
                level = 4,
                title = "Deadlines",
                cards = vocabCards(
                    trackId = "work",
                    level = 4,
                    items = listOf(
                        VocabSeed("締め切り", "しめきり", "deadline", "締{し}め切{き}り"),
                        VocabSeed("納期", "のうき", "delivery date", "納期{のうき}"),
                        VocabSeed("急ぎ", "いそぎ", "urgent", "急{いそ}ぎ"),
                        VocabSeed("重要", "じゅうよう", "important", "重要{じゅうよう}"),
                        VocabSeed("優先", "ゆうせん", "priority", "優先{ゆうせん}"),
                        VocabSeed("進捗", "しんちょく", "progress", "進捗{しんちょく}"),
                        VocabSeed("状況", "じょうきょう", "situation", "状況{じょうきょう}"),
                        VocabSeed("問題", "もんだい", "problem", "問題{もんだい}"),
                        VocabSeed("解決", "かいけつ", "solution", "解決{かいけつ}"),
                        VocabSeed("依頼", "いらい", "request", "依頼{いらい}"),
                        VocabSeed("延期", "えんき", "postponement", "延期{えんき}"),
                        VocabSeed("詳細", "しょうさい", "details", "詳細{しょうさい}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "work",
                        level = 4,
                        index = 0,
                        prompt = "急いでいる（  ）、早めにお願いします。",
                        promptFurigana = "急{いそ}いでいる（　）、早{はや}めにお願いします。",
                        answer = "ので",
                        choices = listOf("ので", "でも", "だけ", "ながら"),
                        meaning = "because"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 4,
                        index = 1,
                        prompt = "できる（  ）早くしてください。",
                        promptFurigana = "できる（　）早{はや}くしてください。",
                        answer = "だけ",
                        choices = listOf("だけ", "ので", "でも", "ながら"),
                        meaning = "as much as possible"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 4,
                        index = 2,
                        prompt = "会議に間に合う（  ）急ぎます。",
                        promptFurigana = "会議{かいぎ}に間{ま}に合{あ}う（　）急{いそ}ぎます。",
                        answer = "ように",
                        choices = listOf("ように", "ので", "だけ", "ながら"),
                        meaning = "so that"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 4,
                        index = 0,
                        prompt = "締め切りに間に合わなかったら、残業しなければ（      ）。",
                        promptFurigana = "締{し}め切{き}りに間{ま}に合{あ}わなかったら、残業{ざんぎょう}しなければ（　　　）。",
                        answer = "なりません",
                        accepted = listOf("なりません"),
                        meaning = "must"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 4,
                        index = 1,
                        prompt = "問題があれば教えて（      ）。",
                        promptFurigana = "問題{もんだい}があれば教{おし}えて（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "request"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 4,
                        index = 0,
                        prompt = "締め切りは明日です。",
                        promptFurigana = "締{し}め切{き}りは明日{あした}です。",
                        answer = "The deadline is tomorrow.",
                        choices = listOf(
                            "The deadline is tomorrow.",
                            "The deadline was yesterday.",
                            "The deadline is next month.",
                            "There is no deadline."
                        ),
                        meaning = "deadline statement"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 4,
                        index = 1,
                        prompt = "問題があれば教えてください。",
                        promptFurigana = "問題{もんだい}があれば教{おし}えてください。",
                        answer = "If there are problems, please tell me.",
                        choices = listOf(
                            "If there are problems, please tell me.",
                            "If there are problems, don't tell me.",
                            "Please create problems.",
                            "There are no problems."
                        ),
                        meaning = "conditional request"
                    ),
                    sentenceBuildCard(
                        trackId = "work",
                        level = 4,
                        index = 0,
                        prompt = "Build: I will finish it by tomorrow.",
                        answer = "明日までに終わらせます",
                        accepted = listOf("明日までに終わらせます", "明日までに終わらせます。"),
                        meaning = "deadline plan"
                    )
                )
            ),
            ScenarioPack(
                level = 5,
                title = "Dialogs",
                cards = vocabCards(
                    trackId = "work",
                    level = 5,
                    items = listOf(
                        VocabSeed("お疲れ様です", "おつかれさまです", "thanks for your hard work", null),
                        VocabSeed("失礼します", "しつれいします", "excuse me", null),
                        VocabSeed("すみません", "すみません", "excuse me", null),
                        VocabSeed("了解", "りょうかい", "understood", "了解{りょうかい}"),
                        VocabSeed("確かに", "たしかに", "indeed", "確{たし}かに"),
                        VocabSeed("念のため", "ねんのため", "just in case", "念{ねん}のため"),
                        VocabSeed("そのまま", "そのまま", "as is", null),
                        VocabSeed("なるほど", "なるほど", "I see", null),
                        VocabSeed("例えば", "たとえば", "for example", "例{たと}えば"),
                        VocabSeed("とりあえず", "とりあえず", "for now", null),
                        VocabSeed("もう一度", "もういちど", "once more", "もう一度{いちど}"),
                        VocabSeed("ありがとうございます", "ありがとうございます", "thank you", null)
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "work",
                        level = 5,
                        index = 0,
                        prompt = "すみません、遅れて（  ）。",
                        promptFurigana = "すみません、遅{おく}れて（　）。",
                        answer = "しまいました",
                        choices = listOf("しまいました", "います", "ください", "ましょう"),
                        meaning = "てしまう (ended up)"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 5,
                        index = 1,
                        prompt = "この件は難しい（  ）思います。",
                        promptFurigana = "この件{けん}は難{むずか}しい（　）思{おも}います。",
                        answer = "と",
                        choices = listOf("と", "に", "で", "を"),
                        meaning = "と思います"
                    ),
                    grammarChoiceCard(
                        trackId = "work",
                        level = 5,
                        index = 2,
                        prompt = "もし時間があれば、手伝っていただけ（  ）か。",
                        promptFurigana = "もし時間{じかん}があれば、手伝{てつだ}っていただけ（　）か。",
                        answer = "ません",
                        choices = listOf("ません", "ます", "ない", "ましょう"),
                        meaning = "polite request"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 5,
                        index = 0,
                        prompt = "念のため、もう一度確認して（      ）。",
                        promptFurigana = "念{ねん}のため、もう一度{いちど}確認{かくにん}して（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "request"
                    ),
                    grammarClozeCard(
                        trackId = "work",
                        level = 5,
                        index = 1,
                        prompt = "もしよければ、手伝っていただけ（      ）か。",
                        promptFurigana = "もしよければ、手伝{てつだ}っていただけ（　　　）か。",
                        answer = "ません",
                        accepted = listOf("ません"),
                        meaning = "polite request"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 5,
                        index = 0,
                        prompt = "では、また連絡します。",
                        promptFurigana = "では、また連絡{れんらく}します。",
                        answer = "I'll contact you again.",
                        choices = listOf(
                            "I'll contact you again.",
                            "I won't contact you.",
                            "Please contact me tomorrow.",
                            "I'm contacting you now."
                        ),
                        meaning = "closing phrase"
                    ),
                    sentenceComprehensionCard(
                        trackId = "work",
                        level = 5,
                        index = 1,
                        prompt = "お疲れ様です。",
                        promptFurigana = "お疲{つか}れ様{さま}です。",
                        answer = "Thanks for your hard work.",
                        choices = listOf(
                            "Thanks for your hard work.",
                            "Good morning.",
                            "Good night.",
                            "Nice to meet you."
                        ),
                        meaning = "set phrase"
                    ),
                    sentenceBuildCard(
                        trackId = "work",
                        level = 5,
                        index = 0,
                        prompt = "Build: Sorry, could you repeat that?",
                        answer = "すみません、もう一度言っていただけませんか",
                        accepted = listOf("すみません、もう一度言っていただけませんか", "すみませんもう一度言っていただけませんか"),
                        meaning = "polite request"
                    )
                )
            )
        )

        return buildScenarioTrack(
            trackId = "work",
            title = "Work Situations",
            description = "JLPT-aligned office vocab, polite requests, and workplace dialogs.",
            accentColor = "#5A7897",
            displayOrder = 3,
            packs = packs
        )
    }

    private fun buildConversationTrack(): TrackSeed {
        val packs = listOf(
            ScenarioPack(
                level = 1,
                title = "Greetings",
                cards = vocabCards(
                    trackId = "conversation",
                    level = 1,
                    items = listOf(
                        VocabSeed("はじめまして", "はじめまして", "nice to meet you", null),
                        VocabSeed("よろしくお願いします", "よろしくおねがいします", "please treat me well", null),
                        VocabSeed("おはようございます", "おはようございます", "good morning", null),
                        VocabSeed("こんにちは", "こんにちは", "hello", null),
                        VocabSeed("こんばんは", "こんばんは", "good evening", null),
                        VocabSeed("ありがとう", "ありがとう", "thanks", null),
                        VocabSeed("ありがとうございます", "ありがとうございます", "thank you", null),
                        VocabSeed("すみません", "すみません", "excuse me", null),
                        VocabSeed("ごめんなさい", "ごめんなさい", "I'm sorry", null),
                        VocabSeed("さようなら", "さようなら", "goodbye", null),
                        VocabSeed("またね", "またね", "see you", null),
                        VocabSeed("お元気ですか", "おげんきですか", "how are you", "お元気{げんき}ですか")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 1,
                        index = 0,
                        prompt = "私（  ）学生です。",
                        promptFurigana = "私{わたし}（　）学生{がくせい}です。",
                        answer = "は",
                        choices = listOf("は", "が", "を", "に"),
                        meaning = "topic particle"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 1,
                        index = 1,
                        prompt = "これはペン（  ）。",
                        promptFurigana = "これはペン（　）。",
                        answer = "です",
                        choices = listOf("です", "ます", "でした", "ません"),
                        meaning = "copula"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 1,
                        index = 2,
                        prompt = "よろしくお願いし（  ）。",
                        promptFurigana = "よろしくお願い{おねが}し（　）。",
                        answer = "ます",
                        choices = listOf("ます", "ました", "ません", "たい"),
                        meaning = "polite form"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 1,
                        index = 0,
                        prompt = "私はアメリカ（      ）来ました。",
                        promptFurigana = "私{わたし}はアメリカ（　　　）来{き}ました。",
                        answer = "から",
                        accepted = listOf("から"),
                        meaning = "from"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 1,
                        index = 1,
                        prompt = "元気（      ）。",
                        promptFurigana = "元気{げんき}（　　　）。",
                        answer = "です",
                        accepted = listOf("です"),
                        meaning = "copula"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 1,
                        index = 0,
                        prompt = "お名前は何ですか。",
                        promptFurigana = "お名前{なまえ}は何{なん}ですか。",
                        answer = "What is your name?",
                        choices = listOf(
                            "What is your name?",
                            "How are you?",
                            "Where are you from?",
                            "What time is it?"
                        ),
                        meaning = "basic question"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 1,
                        index = 1,
                        prompt = "私は日本語を勉強しています。",
                        promptFurigana = "私{わたし}は日本語{にほんご}を勉強{べんきょう}しています。",
                        answer = "I am studying Japanese.",
                        choices = listOf(
                            "I am studying Japanese.",
                            "I can speak Japanese.",
                            "I don't like Japanese.",
                            "I am from Japan."
                        ),
                        meaning = "progressive"
                    ),
                    sentenceBuildCard(
                        trackId = "conversation",
                        level = 1,
                        index = 0,
                        prompt = "Build: My name is Tanji.",
                        answer = "私の名前はたんじです",
                        accepted = listOf("私の名前はたんじです", "わたしのなまえはたんじです"),
                        meaning = "self-introduction"
                    )
                )
            ),
            ScenarioPack(
                level = 2,
                title = "Help & Directions",
                cards = vocabCards(
                    trackId = "conversation",
                    level = 2,
                    items = listOf(
                        VocabSeed("助けてください", "たすけてください", "please help", "助{たす}けてください"),
                        VocabSeed("どこですか", "どこですか", "where is it", null),
                        VocabSeed("右", "みぎ", "right", "右{みぎ}"),
                        VocabSeed("左", "ひだり", "left", "左{ひだり}"),
                        VocabSeed("まっすぐ", "まっすぐ", "straight", null),
                        VocabSeed("近い", "ちかい", "near", "近{ちか}い"),
                        VocabSeed("遠い", "とおい", "far", "遠{とお}い"),
                        VocabSeed("駅", "えき", "station", "駅{えき}"),
                        VocabSeed("ここ", "ここ", "here", null),
                        VocabSeed("そこ", "そこ", "there", null),
                        VocabSeed("あそこ", "あそこ", "over there", null),
                        VocabSeed("道", "みち", "road", "道{みち}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 2,
                        index = 0,
                        prompt = "駅へはどうやって行き（  ）か。",
                        promptFurigana = "駅{えき}へはどうやって行{い}き（　）か。",
                        answer = "ます",
                        choices = listOf("ます", "ました", "ません", "たい"),
                        meaning = "polite question"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 2,
                        index = 1,
                        prompt = "右に曲がっ（  ）。",
                        promptFurigana = "右{みぎ}に曲{ま}がっ（　）。",
                        answer = "て",
                        choices = listOf("て", "た", "ない", "ます"),
                        meaning = "te-form sequence"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 2,
                        index = 2,
                        prompt = "ここから駅まで歩い（  ）ください。",
                        promptFurigana = "ここから駅{えき}まで歩{ある}い（　）ください。",
                        answer = "て",
                        choices = listOf("て", "た", "ない", "ます"),
                        meaning = "te-form + ください"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 2,
                        index = 0,
                        prompt = "すみません、駅はどこ（      ）か。",
                        promptFurigana = "すみません、駅{えき}はどこ（　　　）か。",
                        answer = "です",
                        accepted = listOf("です"),
                        meaning = "ですか question"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 2,
                        index = 1,
                        prompt = "まっすぐ行って、次の角を左に曲がって（      ）。",
                        promptFurigana = "まっすぐ行{い}って、次{つぎ}の角{かど}を左{ひだり}に曲{ま}がって（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "directions"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 2,
                        index = 0,
                        prompt = "この道をまっすぐ行ってください。",
                        promptFurigana = "この道{みち}をまっすぐ行{い}ってください。",
                        answer = "Please go straight on this road.",
                        choices = listOf(
                            "Please go straight on this road.",
                            "Please turn right immediately.",
                            "Please stop here.",
                            "Please go back."
                        ),
                        meaning = "direction phrase"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 2,
                        index = 1,
                        prompt = "駅はここから近いです。",
                        promptFurigana = "駅{えき}はここから近{ちか}いです。",
                        answer = "The station is near from here.",
                        choices = listOf(
                            "The station is near from here.",
                            "The station is far from here.",
                            "The station is closed.",
                            "The station is on the left."
                        ),
                        meaning = "near/far"
                    ),
                    sentenceBuildCard(
                        trackId = "conversation",
                        level = 2,
                        index = 0,
                        prompt = "Build: Where is the bathroom?",
                        answer = "トイレはどこですか",
                        accepted = listOf("トイレはどこですか", "お手洗いはどこですか"),
                        meaning = "location question"
                    )
                )
            ),
            ScenarioPack(
                level = 3,
                title = "Plans",
                cards = vocabCards(
                    trackId = "conversation",
                    level = 3,
                    items = listOf(
                        VocabSeed("今日", "きょう", "today", "今日{きょう}"),
                        VocabSeed("明日", "あした", "tomorrow", "明日{あした}"),
                        VocabSeed("来週", "らいしゅう", "next week", "来週{らいしゅう}"),
                        VocabSeed("予定", "よてい", "plan", "予定{よてい}"),
                        VocabSeed("会う", "あう", "meet", "会{あ}う"),
                        VocabSeed("行く", "いく", "go", "行{い}く"),
                        VocabSeed("来る", "くる", "come", "来{く}る"),
                        VocabSeed("旅行", "りょこう", "trip", "旅行{りょこう}"),
                        VocabSeed("映画", "えいが", "movie", "映画{えいが}"),
                        VocabSeed("一緒に", "いっしょに", "together", "一緒{いっしょ}に"),
                        VocabSeed("時間", "じかん", "time", "時間{じかん}"),
                        VocabSeed("約束", "やくそく", "appointment", "約束{やくそく}")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 3,
                        index = 0,
                        prompt = "明日映画を見（  ）。",
                        promptFurigana = "明日{あした}映画{えいが}を見{み}（　）。",
                        answer = "たいです",
                        choices = listOf("たいです", "ました", "ません", "ましょう"),
                        meaning = "たい (want to)"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 3,
                        index = 1,
                        prompt = "来週旅行するつもり（  ）。",
                        promptFurigana = "来週{らいしゅう}旅行{りょこう}するつもり（　）。",
                        answer = "です",
                        choices = listOf("です", "ます", "でした", "ません"),
                        meaning = "つもりです"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 3,
                        index = 2,
                        prompt = "時間があった（  ）、会いましょう。",
                        promptFurigana = "時間{じかん}があった（　）、会{あ}いましょう。",
                        answer = "ら",
                        choices = listOf("ら", "ので", "だけ", "ながら"),
                        meaning = "たら"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 3,
                        index = 0,
                        prompt = "明日、会い（      ）か。",
                        promptFurigana = "明日{あした}、会{あ}い（　　　）か。",
                        answer = "ません",
                        accepted = listOf("ません"),
                        meaning = "invitation"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 3,
                        index = 1,
                        prompt = "土曜日に出かける（      ）です。",
                        promptFurigana = "土曜日{どようび}に出{で}かける（　　　）です。",
                        answer = "つもり",
                        accepted = listOf("つもり"),
                        meaning = "intend"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 3,
                        index = 0,
                        prompt = "来週、友だちに会います。",
                        promptFurigana = "来週{らいしゅう}、友{とも}だちに会{あ}います。",
                        answer = "Next week I will meet a friend.",
                        choices = listOf(
                            "Next week I will meet a friend.",
                            "Next week I will go to work.",
                            "Today I met a friend.",
                            "I will not meet anyone."
                        ),
                        meaning = "future plan"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 3,
                        index = 1,
                        prompt = "今日は忙しいので、明日にしましょう。",
                        promptFurigana = "今日{きょう}は忙{いそが}しいので、明日{あした}にしましょう。",
                        answer = "I'm busy today, so let's do it tomorrow.",
                        choices = listOf(
                            "I'm busy today, so let's do it tomorrow.",
                            "I'm free today, so let's do it now.",
                            "Tomorrow is busy.",
                            "Let's never do it."
                        ),
                        meaning = "reason + suggestion"
                    ),
                    sentenceBuildCard(
                        trackId = "conversation",
                        level = 3,
                        index = 0,
                        prompt = "Build: Let's meet tomorrow.",
                        answer = "明日会いましょう",
                        accepted = listOf("明日会いましょう", "明日、会いましょう"),
                        meaning = "invitation"
                    )
                )
            ),
            ScenarioPack(
                level = 4,
                title = "Common Patterns",
                cards = vocabCards(
                    trackId = "conversation",
                    level = 4,
                    items = listOf(
                        VocabSeed("好き", "すき", "like", "好{す}き"),
                        VocabSeed("嫌い", "きらい", "dislike", "嫌{きら}い"),
                        VocabSeed("上手", "じょうず", "good at", "上手{じょうず}"),
                        VocabSeed("下手", "へた", "bad at", "下手{へた}"),
                        VocabSeed("できます", "できます", "can do", null),
                        VocabSeed("できません", "できません", "can't do", null),
                        VocabSeed("もう一度", "もういちど", "again", "もう一度{いちど}"),
                        VocabSeed("ゆっくり", "ゆっくり", "slowly", null),
                        VocabSeed("大丈夫", "だいじょうぶ", "okay", "大丈夫{だいじょうぶ}"),
                        VocabSeed("本当", "ほんとう", "true", "本当{ほんとう}"),
                        VocabSeed("例えば", "たとえば", "for example", "例{たと}えば"),
                        VocabSeed("たぶん", "たぶん", "maybe", null)
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 4,
                        index = 0,
                        prompt = "日本語が話せる（  ）なりました。",
                        promptFurigana = "日本語{にほんご}が話{はな}せる（　）なりました。",
                        answer = "ように",
                        choices = listOf("ように", "ので", "だけ", "ながら"),
                        meaning = "ようになる"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 4,
                        index = 1,
                        prompt = "音楽を聞き（  ）運転します。",
                        promptFurigana = "音楽{おんがく}を聞{き}き（　）運転{うんてん}します。",
                        answer = "ながら",
                        choices = listOf("ながら", "ので", "だけ", "でも"),
                        meaning = "while"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 4,
                        index = 2,
                        prompt = "週末は映画を見たり、本を読んだりし（  ）。",
                        promptFurigana = "週末{しゅうまつ}は映画{えいが}を見{み}たり、本{ほん}を読{よ}んだりし（　）。",
                        answer = "ます",
                        choices = listOf("ます", "ました", "ません", "たい"),
                        meaning = "habit"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 4,
                        index = 0,
                        prompt = "もう一度言って（      ）。",
                        promptFurigana = "もう一度{いちど}言{い}って（　　　）。",
                        answer = "ください",
                        accepted = listOf("ください"),
                        meaning = "request"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 4,
                        index = 1,
                        prompt = "わからない（      ）、教えてください。",
                        promptFurigana = "わからない（　　　）、教{おし}えてください。",
                        answer = "ときは",
                        accepted = listOf("とき", "ときは"),
                        meaning = "when"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 4,
                        index = 0,
                        prompt = "ゆっくり話してください。",
                        promptFurigana = "ゆっくり話{はな}してください。",
                        answer = "Please speak slowly.",
                        choices = listOf(
                            "Please speak slowly.",
                            "Please speak quickly.",
                            "Please write it.",
                            "Please listen."
                        ),
                        meaning = "request"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 4,
                        index = 1,
                        prompt = "大丈夫ですか。",
                        promptFurigana = "大丈夫{だいじょうぶ}ですか。",
                        answer = "Are you okay?",
                        choices = listOf(
                            "Are you okay?",
                            "Are you busy?",
                            "Are you a teacher?",
                            "Are you from Japan?"
                        ),
                        meaning = "check-in"
                    ),
                    sentenceBuildCard(
                        trackId = "conversation",
                        level = 4,
                        index = 0,
                        prompt = "Build: I can speak Japanese a little.",
                        answer = "日本語が少し話せます",
                        accepted = listOf("日本語が少し話せます", "日本語を少し話せます"),
                        meaning = "ability"
                    )
                )
            ),
            ScenarioPack(
                level = 5,
                title = "Dialogs",
                cards = vocabCards(
                    trackId = "conversation",
                    level = 5,
                    items = listOf(
                        VocabSeed("もし", "もし", "if", null),
                        VocabSeed("場合", "ばあい", "case", "場合{ばあい}"),
                        VocabSeed("かもしれません", "かもしれません", "might", null),
                        VocabSeed("と思います", "とおもいます", "I think", null),
                        VocabSeed("でしょう", "でしょう", "probably", null),
                        VocabSeed("もちろん", "もちろん", "of course", null),
                        VocabSeed("たしかに", "たしかに", "indeed", null),
                        VocabSeed("しまいました", "しまいました", "ended up", null),
                        VocabSeed("もうすぐ", "もうすぐ", "soon", null),
                        VocabSeed("それから", "それから", "and then", null),
                        VocabSeed("そのあと", "そのあと", "after that", null),
                        VocabSeed("念のため", "ねんのため", "just in case", "念{ねん}のため")
                    )
                ) + listOf(
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 5,
                        index = 0,
                        prompt = "明日行けない（  ）、連絡します。",
                        promptFurigana = "明日{あした}行{い}けない（　）、連絡{れんらく}します。",
                        answer = "ときは",
                        choices = listOf("ときは", "ので", "だけ", "ながら"),
                        meaning = "when/if"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 5,
                        index = 1,
                        prompt = "この店は高い（  ）思います。",
                        promptFurigana = "この店{みせ}は高{たか}い（　）思{おも}います。",
                        answer = "と",
                        choices = listOf("と", "に", "で", "を"),
                        meaning = "I think that"
                    ),
                    grammarChoiceCard(
                        trackId = "conversation",
                        level = 5,
                        index = 2,
                        prompt = "もし時間があれば、一緒に行き（  ）。",
                        promptFurigana = "もし時間{じかん}があれば、一緒{いっしょ}に行{い}き（　）。",
                        answer = "ませんか",
                        choices = listOf("ませんか", "ません", "ました", "たいです"),
                        meaning = "invitation"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 5,
                        index = 0,
                        prompt = "たぶん明日は雨が降る（      ）。",
                        promptFurigana = "たぶん明日{あした}は雨{あめ}が降{ふ}る（　　　）。",
                        answer = "と思います",
                        accepted = listOf("と思います"),
                        meaning = "I think"
                    ),
                    grammarClozeCard(
                        trackId = "conversation",
                        level = 5,
                        index = 1,
                        prompt = "もしかしたら遅れる（      ）。",
                        promptFurigana = "もしかしたら遅{おく}れる（　　　）。",
                        answer = "かもしれません",
                        accepted = listOf("かもしれません"),
                        meaning = "might"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 5,
                        index = 0,
                        prompt = "すみません、遅れてしまいました。",
                        promptFurigana = "すみません、遅{おく}れてしまいました。",
                        answer = "Sorry, I ended up being late.",
                        choices = listOf(
                            "Sorry, I ended up being late.",
                            "Sorry, I arrived early.",
                            "Sorry, I can't go.",
                            "Sorry, I'm leaving now."
                        ),
                        meaning = "てしまう"
                    ),
                    sentenceComprehensionCard(
                        trackId = "conversation",
                        level = 5,
                        index = 1,
                        prompt = "もしよければ、一緒に行きませんか。",
                        promptFurigana = "もしよければ、一緒{いっしょ}に行{い}きませんか。",
                        answer = "If you'd like, shall we go together?",
                        choices = listOf(
                            "If you'd like, shall we go together?",
                            "If you'd like, don't go.",
                            "I will go alone tomorrow.",
                            "Where is the station?"
                        ),
                        meaning = "invitation"
                    ),
                    sentenceBuildCard(
                        trackId = "conversation",
                        level = 5,
                        index = 0,
                        prompt = "Build: I think it will be ok.",
                        answer = "大丈夫だと思います",
                        accepted = listOf("大丈夫だと思います", "大丈夫だと思う"),
                        meaning = "I think"
                    )
                )
            )
        )

        return buildScenarioTrack(
            trackId = "conversation",
            title = "Everyday Conversation",
            description = "Core phrases, grammar patterns, and short dialogs for daily conversation.",
            accentColor = "#8B6B55",
            displayOrder = 4,
            packs = packs
        )
    }

    private data class SupplementalCardSeed(
        val packLevel: Int,
        val positionOffset: Int,
        val card: CardEntity
    )

    private fun buildSupplementalCards(): List<SupplementalCardSeed> {
        return listOf(
            SupplementalCardSeed(
                packLevel = 1,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_vocab_01",
                    type = CardType.VOCAB_READING,
                    prompt = "行く",
                    canonicalAnswer = "to go",
                    acceptedAnswersRaw = "to go",
                    reading = "いく",
                    meaning = "to go",
                    promptFurigana = "行{い}く",
                    choicesRaw = "to come|to go|to wait|to stop",
                    difficulty = 1,
                    templateId = "tmpl_n5_extra_vocab_01"
                )
            ),
            SupplementalCardSeed(
                packLevel = 1,
                positionOffset = 2,
                card = CardEntity(
                    cardId = "n5_extra_reading_01",
                    type = CardType.KANJI_READING,
                    prompt = "\u65e5",
                    canonicalAnswer = "nichi",
                    acceptedAnswersRaw = "nichi|jitsu",
                    reading = "nichi",
                    meaning = "kanji reading",
                    promptFurigana = null,
                    choicesRaw = "nichi|jitsu|getsu|mizu",
                    difficulty = 1,
                    templateId = "tmpl_n5_extra_reading_01"
                )
            ),
            SupplementalCardSeed(
                packLevel = 2,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_grammar_01",
                    type = CardType.GRAMMAR_CHOICE,
                    prompt = "音楽を聞き（  ）勉強します。",
                    canonicalAnswer = "ながら",
                    acceptedAnswersRaw = "ながら",
                    reading = "ながら",
                    meaning = "while doing ...",
                    promptFurigana = "音楽{おんがく}を聞{き}き（　）勉強{べんきょう}します。",
                    choicesRaw = "ながら|ので|だけ|でも",
                    difficulty = 2,
                    templateId = "tmpl_n5_extra_grammar_01"
                )
            ),
            SupplementalCardSeed(
                packLevel = 2,
                positionOffset = 2,
                card = CardEntity(
                    cardId = "n5_extra_reading_02",
                    type = CardType.KANJI_READING,
                    prompt = "\u6708",
                    canonicalAnswer = "getsu",
                    acceptedAnswersRaw = "getsu|gatsu",
                    reading = "getsu",
                    meaning = "kanji reading",
                    promptFurigana = null,
                    choicesRaw = "getsu|gatsu|nichi|sui",
                    difficulty = 2,
                    templateId = "tmpl_n5_extra_reading_02"
                )
            ),
            SupplementalCardSeed(
                packLevel = 3,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_sentence_01",
                    type = CardType.SENTENCE_COMPREHENSION,
                    prompt = "彼は薬を飲まないで寝た。",
                    canonicalAnswer = "He slept without taking medicine.",
                    acceptedAnswersRaw = "He slept without taking medicine.",
                    reading = null,
                    meaning = "without doing ...",
                    promptFurigana = "彼{かれ}は薬{くすり}を飲{の}まないで寝{ね}た。",
                    choicesRaw = "He took medicine and slept.|He slept without taking medicine.|He did not sleep because he skipped medicine.|He took medicine but could not sleep.",
                    difficulty = 3,
                    templateId = "tmpl_n5_extra_sentence_01"
                )
            ),
            SupplementalCardSeed(
                packLevel = 3,
                positionOffset = 2,
                card = CardEntity(
                    cardId = "n5_extra_reading_03",
                    type = CardType.KANJI_READING,
                    prompt = "\u5b66",
                    canonicalAnswer = "gaku",
                    acceptedAnswersRaw = "gaku|manabu",
                    reading = "gaku",
                    meaning = "kanji reading",
                    promptFurigana = null,
                    choicesRaw = "gaku|manabu|kou|sho",
                    difficulty = 3,
                    templateId = "tmpl_n5_extra_reading_03"
                )
            ),
            SupplementalCardSeed(
                packLevel = 4,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_vocab_02",
                    type = CardType.VOCAB_READING,
                    prompt = "予約",
                    canonicalAnswer = "reservation",
                    acceptedAnswersRaw = "reservation",
                    reading = "よやく",
                    meaning = "reservation",
                    promptFurigana = "予約{よやく}",
                    choicesRaw = "library|reservation|passport|restaurant",
                    difficulty = 4,
                    templateId = "tmpl_n5_extra_vocab_02"
                )
            ),
            SupplementalCardSeed(
                packLevel = 4,
                positionOffset = 2,
                card = CardEntity(
                    cardId = "n5_extra_reading_04",
                    type = CardType.KANJI_READING,
                    prompt = "\u751f",
                    canonicalAnswer = "sei",
                    acceptedAnswersRaw = "sei|shou|ikiru",
                    reading = "sei",
                    meaning = "kanji reading",
                    promptFurigana = null,
                    choicesRaw = "sei|shou|jitsu|kaku",
                    difficulty = 4,
                    templateId = "tmpl_n5_extra_reading_04"
                )
            ),
            SupplementalCardSeed(
                packLevel = 5,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_grammar_02",
                    type = CardType.GRAMMAR_CLOZE_WRITE,
                    prompt = "来週、病院に行（      ）。",
                    canonicalAnswer = "かなければなりません",
                    acceptedAnswersRaw = "かなければなりません|かなければならない",
                    reading = null,
                    meaning = "must do ...",
                    promptFurigana = "来週{らいしゅう}、病院{びょういん}に行{い}（　　　）。",
                    choicesRaw = null,
                    difficulty = 5,
                    templateId = "tmpl_n5_extra_grammar_02"
                )
            ),
            SupplementalCardSeed(
                packLevel = 5,
                positionOffset = 2,
                card = CardEntity(
                    cardId = "n5_extra_reading_05",
                    type = CardType.KANJI_READING,
                    prompt = "\u6821",
                    canonicalAnswer = "kou",
                    acceptedAnswersRaw = "kou",
                    reading = "kou",
                    meaning = "kanji reading",
                    promptFurigana = null,
                    choicesRaw = "kou|gaku|sei|mon",
                    difficulty = 5,
                    templateId = "tmpl_n5_extra_reading_05"
                )
            ),
            SupplementalCardSeed(
                packLevel = 6,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_build_01",
                    type = CardType.SENTENCE_BUILD,
                    prompt = "Use つもり with 行く and 明日",
                    canonicalAnswer = "明日行くつもりです",
                    acceptedAnswersRaw = "明日行くつもりです|明日、行くつもりです",
                    reading = null,
                    meaning = "intend to ...",
                    promptFurigana = "明日{あした}行{い}くつもりです",
                    choicesRaw = null,
                    difficulty = 6,
                    templateId = "tmpl_n5_extra_build_01"
                )
            ),
            SupplementalCardSeed(
                packLevel = 7,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_sentence_02",
                    type = CardType.SENTENCE_COMPREHENSION,
                    prompt = "明日までにレポートを出してください。",
                    canonicalAnswer = "It should be submitted by tomorrow.",
                    acceptedAnswersRaw = "It should be submitted by tomorrow.",
                    reading = null,
                    meaning = "deadline request",
                    promptFurigana = "明日{あした}までにレポートを出{だ}してください。",
                    choicesRaw = "It was submitted yesterday.|It should be submitted by tomorrow.|You may submit next week.|No report is needed.",
                    difficulty = 7,
                    templateId = "tmpl_n5_extra_sentence_02"
                )
            ),
            SupplementalCardSeed(
                packLevel = 8,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_grammar_03",
                    type = CardType.GRAMMAR_CLOZE_WRITE,
                    prompt = "雨が降った（      ）、出かけます。",
                    canonicalAnswer = "としても",
                    acceptedAnswersRaw = "としても",
                    reading = null,
                    meaning = "even if ...",
                    promptFurigana = "雨{あめ}が降{ふ}った（　　　）、出{で}かけます。",
                    choicesRaw = null,
                    difficulty = 8,
                    templateId = "tmpl_n5_extra_grammar_03"
                )
            ),
            SupplementalCardSeed(
                packLevel = 9,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_build_02",
                    type = CardType.SENTENCE_BUILD,
                    prompt = "Use ないで with 飲む and 寝る",
                    canonicalAnswer = "薬を飲まないで寝ました",
                    acceptedAnswersRaw = "薬を飲まないで寝ました|薬を飲まないで寝た",
                    reading = null,
                    meaning = "without doing ...",
                    promptFurigana = "薬{くすり}を飲{の}まないで寝{ね}ました",
                    choicesRaw = null,
                    difficulty = 9,
                    templateId = "tmpl_n5_extra_build_02"
                )
            ),
            SupplementalCardSeed(
                packLevel = 10,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_vocab_03",
                    type = CardType.VOCAB_READING,
                    prompt = "進める",
                    canonicalAnswer = "to advance",
                    acceptedAnswersRaw = "to advance",
                    reading = "すすめる",
                    meaning = "to advance",
                    promptFurigana = "進{すす}める",
                    choicesRaw = "to hide|to explain|to advance|to borrow",
                    difficulty = 10,
                    templateId = "tmpl_n5_extra_vocab_03"
                )
            ),
            SupplementalCardSeed(
                packLevel = 11,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_grammar_04",
                    type = CardType.GRAMMAR_CLOZE_WRITE,
                    prompt = "時間があれば、映画を見（      ）。",
                    canonicalAnswer = "たいです",
                    acceptedAnswersRaw = "たいです",
                    reading = null,
                    meaning = "want to do ...",
                    promptFurigana = "時間{じかん}があれば、映画{えいが}を見{み}（　　　）。",
                    choicesRaw = null,
                    difficulty = 11,
                    templateId = "tmpl_n5_extra_grammar_04"
                )
            ),
            SupplementalCardSeed(
                packLevel = 12,
                positionOffset = 1,
                card = CardEntity(
                    cardId = "n5_extra_build_03",
                    type = CardType.SENTENCE_BUILD,
                    prompt = "Build a sentence with 〜ように and 忘れない",
                    canonicalAnswer = "忘れないようにメモします",
                    acceptedAnswersRaw = "忘れないようにメモします",
                    reading = null,
                    meaning = "so that ...",
                    promptFurigana = "忘{わす}れないようにメモします",
                    choicesRaw = null,
                    difficulty = 12,
                    templateId = "tmpl_n5_extra_build_03"
                )
            )
        )
    }

    private fun strokeCountFor(symbol: String): Int {
        return strokeOverrides[symbol] ?: ((symbol.first().code % 8) + 3)
    }

    private fun buildTemplateStrokePaths(symbol: String, strokeCount: Int): String {
        val code = symbol.first().code
        val strokes = (0 until strokeCount).map { index ->
            val selector = (code + index * 17) % 4
            val xOffset = (((code + index * 31) % 16) - 8) / 260f
            val yOffset = (((code + index * 19) % 14) - 7) / 260f
            val startX: Float
            val startY: Float
            val endX: Float
            val endY: Float
            when (selector) {
                0 -> {
                    startX = 0.18f + xOffset
                    endX = 0.82f + xOffset
                    startY = 0.14f + (index.toFloat() / (strokeCount + 2f)) + yOffset
                    endY = startY
                }

                1 -> {
                    startY = 0.16f + yOffset
                    endY = 0.84f + yOffset
                    startX = 0.14f + (index.toFloat() / (strokeCount + 2f)) + xOffset
                    endX = startX
                }

                2 -> {
                    startX = 0.20f + xOffset
                    startY = 0.22f + yOffset
                    endX = 0.80f + xOffset
                    endY = 0.78f + yOffset
                }

                else -> {
                    startX = 0.80f + xOffset
                    startY = 0.20f + yOffset
                    endX = 0.22f + xOffset
                    endY = 0.82f + yOffset
                }
            }
            buildStrokePoints(startX, startY, endX, endY)
        }

        return strokes.joinToString(separator = "|")
    }

    private fun englishPromptFor(index: Int): String {
        return n5EnglishPrompts.getOrElse(index) {
            error("Missing English prompt for index=$index")
        }
    }

    private fun buildStrokePoints(startX: Float, startY: Float, endX: Float, endY: Float): String {
        val points = (0..4).map { step ->
            val t = step / 4f
            val x = (startX + (endX - startX) * t).coerceIn(0.05f, 0.95f)
            val y = (startY + (endY - startY) * t).coerceIn(0.05f, 0.95f)
            "${"%.3f".format(Locale.US, x)},${"%.3f".format(Locale.US, y)}"
        }
        return points.joinToString(separator = ";")
    }

    private val strokeOverrides = mapOf(
        "一" to 1,
        "二" to 2,
        "三" to 3,
        "十" to 2,
        "人" to 2,
        "口" to 3,
        "日" to 4,
        "月" to 4,
        "木" to 4,
        "水" to 4,
        "火" to 4,
        "土" to 3,
        "山" to 3,
        "川" to 3,
        "田" to 5,
        "目" to 5,
        "手" to 4,
        "足" to 7,
        "車" to 7,
        "学" to 8,
        "食" to 9,
        "語" to 14,
        "電" to 13
    )

    private val n5EnglishPrompts = listOf(
        "one", "right", "rain", "yen circle", "king", "sound", "down", "fire", "flower", "shell money",
        "study", "spirit", "nine", "rest", "jewel", "gold", "sky", "moon month", "dog", "see",
        "five", "mouth", "school", "left", "three", "mountain", "child", "four", "thread", "character",
        "ear", "seven", "car", "hand", "ten", "exit", "woman", "small", "up", "forest",
        "person", "water", "correct", "life", "blue green", "evening", "stone", "red", "thousand", "river",
        "previous", "early", "grass", "foot", "village", "big", "man", "bamboo", "middle", "insect",
        "town", "heaven", "rice field", "earth", "two", "sun day", "enter", "year", "white", "eight",
        "hundred", "writing", "tree", "book origin", "name", "eye", "stand", "power", "woods", "six",
        "pull", "feather", "cloud", "garden", "far", "what", "subject", "summer", "home", "song",
        "picture", "times", "meeting", "sea", "draw", "outside", "angle corner", "music fun", "active", "interval",
        "circle", "rock", "face", "steam", "record", "return", "bow", "cow", "fish", "capital",
        "strong", "teach", "near", "older brother", "shape", "measure", "origin", "source", "door", "old",
        "noon", "after", "language", "craft", "public", "wide", "exchange", "light", "consider", "go",
        "high", "yellow", "fit combine", "valley", "country", "black", "now", "talent", "detail", "make",
        "calculate", "stop", "city market", "arrow", "older sister", "think", "paper", "temple", "self", "time",
        "room", "company shrine", "weak", "head", "autumn", "week", "spring", "write", "few", "place",
        "color", "eat food", "heart", "new", "parent", "map", "number", "west", "voice", "star",
        "clear weather", "cut", "snow", "boat", "line", "before", "group", "run", "many", "thick",
        "body", "platform", "ground", "pond", "know", "tea", "daytime", "long", "bird", "morning",
        "straight", "pass through", "younger brother", "shop", "point dot", "electricity", "sword", "winter", "hit", "east",
        "answer", "head top", "same", "road", "read", "inside", "south", "meat", "horse", "sell",
        "buy", "wheat", "half", "numbered turn", "father", "wind", "minute part", "hear ask", "rice", "walk",
        "mother", "direction", "north", "every", "younger sister", "ten thousand", "bright", "chirp", "hair", "gate",
        "night", "field plain", "friend", "use", "weekday", "come", "village inside", "reason", "talk", "harmony"
    )

    private val rawKanji = listOf(
        "一", "右", "雨", "円", "王", "音", "下", "火", "花", "貝",
        "学", "気", "九", "休", "玉", "金", "空", "月", "犬", "見",
        "五", "口", "校", "左", "三", "山", "子", "四", "糸", "字",
        "耳", "七", "車", "手", "十", "出", "女", "小", "上", "森",
        "人", "水", "正", "生", "青", "夕", "石", "赤", "千", "川",
        "先", "早", "草", "足", "村", "大", "男", "竹", "中", "虫",
        "町", "天", "田", "土", "二", "日", "入", "年", "白", "八",
        "百", "文", "木", "本", "名", "目", "立", "力", "林", "六",
        "引", "羽", "雲", "園", "遠", "何", "科", "夏", "家", "歌",
        "画", "回", "会", "海", "絵", "外", "角", "楽", "活", "間",
        "丸", "岩", "顔", "汽", "記", "帰", "弓", "牛", "魚", "京",
        "強", "教", "近", "兄", "形", "計", "元", "原", "戸", "古",
        "午", "後", "語", "工", "公", "広", "交", "光", "考", "行",
        "高", "黄", "合", "谷", "国", "黒", "今", "才", "細", "作",
        "算", "止", "市", "矢", "姉", "思", "紙", "寺", "自", "時",
        "室", "社", "弱", "首", "秋", "週", "春", "書", "少", "場",
        "色", "食", "心", "新", "親", "図", "数", "西", "声", "星",
        "晴", "切", "雪", "船", "線", "前", "組", "走", "多", "太",
        "体", "台", "地", "池", "知", "茶", "昼", "長", "鳥", "朝",
        "直", "通", "弟", "店", "点", "電", "刀", "冬", "当", "東",
        "答", "頭", "同", "道", "読", "内", "南", "肉", "馬", "売",
        "買", "麦", "半", "番", "父", "風", "分", "聞", "米", "歩",
        "母", "方", "北", "毎", "妹", "万", "明", "鳴", "毛", "門",
        "夜", "野", "友", "用", "曜", "来", "里", "理", "話", "和"
    )
}
