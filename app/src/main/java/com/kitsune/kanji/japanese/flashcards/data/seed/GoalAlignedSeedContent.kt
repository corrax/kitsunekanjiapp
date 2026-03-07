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

object GoalAlignedSeedContent {
    const val dailyChallengeTrackId = "daily_challenge_core"

    fun build(): SeedBundle {
        val foundations = buildFoundationsTrack()
        val n4 = buildJlptN4Track()
        val n3 = buildJlptN3Track()
        val daily = buildDailyChallengeTrack()
        val dailyLife = buildDailyLifeTrack()
        val food = buildFoodTrack()
        val transport = buildTransportTrack()
        val shopping = buildShoppingTrack()
        val konbini = buildKonbiniTrack()
        val signs = buildSignsTrack()
        val adulting = buildAdultingTrack()
        val all = listOf(foundations, n4, n3, daily, dailyLife, food, transport, shopping, konbini, signs, adulting)
        return SeedBundle(
            tracks = all.map { it.track },
            packs = all.flatMap { it.packs },
            cards = all.flatMap { it.cards },
            templates = all.flatMap { it.templates },
            packCards = all.flatMap { it.packCards },
            progress = all.flatMap { it.progress }
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

    private data class PackSeed(
        val level: Int,
        val title: String,
        val cards: List<CardEntity>
    )

    private data class KanjiSeed(
        val symbol: String,
        val meaning: String
    )

    private data class VocabSeed(
        val term: String,
        val reading: String,
        val meaning: String,
        val choices: List<String>
    )

    private data class GrammarSeed(
        val prompt: String,
        val promptFurigana: String? = null,
        val answer: String,
        val choices: List<String>,
        val meaning: String
    )

    private data class ClozeSeed(
        val prompt: String,
        val promptFurigana: String? = null,
        val answer: String,
        val accepted: List<String>,
        val meaning: String
    )

    private data class SentenceSeed(
        val prompt: String,
        val promptFurigana: String? = null,
        val answer: String,
        val choices: List<String>,
        val meaning: String
    )

    private data class BuildSeed(
        val prompt: String,
        val answer: String,
        val accepted: List<String>,
        val meaning: String
    )

    private data class FoundationsVocab(
        val term: String,
        val reading: String,
        val meaning: String,
        val choices: List<String>
    )

    private data class FoundationsKanji(
        val symbol: String,
        val reading: String,
        val meaning: String,
        val readingChoices: List<String>
    )

    private fun buildFoundationsTrack(): TrackSeed {
        val trackId = "foundations"

        // Pack 1: Greetings & Basics (difficulty 0)
        val pack1Vocab = listOf(
            FoundationsVocab("こんにちは", "konnichiwa", "hello",
                listOf("hello", "goodbye", "thank you", "excuse me")),
            FoundationsVocab("ありがとう", "arigatou", "thank you",
                listOf("sorry", "thank you", "good morning", "goodbye")),
            FoundationsVocab("すみません", "sumimasen", "excuse me",
                listOf("excuse me", "hello", "please", "goodbye")),
            FoundationsVocab("おはよう", "ohayou", "good morning",
                listOf("good evening", "good night", "good morning", "good afternoon"))
        )
        val pack1Kanji = listOf(
            FoundationsKanji("日", "nichi", "day/sun",
                listOf("nichi", "getsu", "ka", "sui")),
            FoundationsKanji("人", "hito", "person",
                listOf("hito", "yama", "kawa", "ki"))
        )

        // Pack 2: Numbers & Counting (difficulty 0)
        val pack2Vocab = listOf(
            FoundationsVocab("一つ", "hitotsu", "one (thing)",
                listOf("one (thing)", "two (things)", "three (things)", "five (things)")),
            FoundationsVocab("二つ", "futatsu", "two (things)",
                listOf("one (thing)", "two (things)", "four (things)", "ten (things)")),
            FoundationsVocab("三つ", "mittsu", "three (things)",
                listOf("three (things)", "six (things)", "two (things)", "nine (things)")),
            FoundationsVocab("五つ", "itsutsu", "five (things)",
                listOf("four (things)", "five (things)", "seven (things)", "eight (things)"))
        )
        val pack2Kanji = listOf(
            FoundationsKanji("一", "ichi", "one",
                listOf("ichi", "ni", "san", "go")),
            FoundationsKanji("十", "juu", "ten",
                listOf("juu", "hyaku", "sen", "man"))
        )

        // Pack 3: Self & Others (difficulty 1)
        val pack3Vocab = listOf(
            FoundationsVocab("わたし", "watashi", "I/me",
                listOf("I/me", "you", "he/she", "we")),
            FoundationsVocab("あなた", "anata", "you",
                listOf("I/me", "you", "they", "everyone")),
            FoundationsVocab("名前", "namae", "name",
                listOf("name", "age", "place", "word")),
            FoundationsVocab("友達", "tomodachi", "friend",
                listOf("teacher", "student", "friend", "family"))
        )
        val pack3Kanji = listOf(
            FoundationsKanji("名", "na", "name",
                listOf("na", "mae", "ato", "ue")),
            FoundationsKanji("前", "mae", "before/front",
                listOf("mae", "ushiro", "naka", "soto"))
        )

        // Pack 4: Places & Things (difficulty 1)
        val pack4Vocab = listOf(
            FoundationsVocab("学校", "gakkou", "school",
                listOf("school", "house", "store", "park")),
            FoundationsVocab("家", "ie", "house",
                listOf("station", "house", "hospital", "library")),
            FoundationsVocab("水", "mizu", "water",
                listOf("water", "fire", "tea", "milk")),
            FoundationsVocab("本", "hon", "book",
                listOf("pen", "desk", "book", "chair"))
        )
        val pack4Kanji = listOf(
            FoundationsKanji("学", "gaku", "study",
                listOf("gaku", "kou", "sei", "shi")),
            FoundationsKanji("水", "sui", "water",
                listOf("sui", "ka", "moku", "kin"))
        )

        data class FoundationsPack(
            val level: Int,
            val title: String,
            val difficulty: Int,
            val vocab: List<FoundationsVocab>,
            val kanji: List<FoundationsKanji>
        )

        val packDefs = listOf(
            FoundationsPack(1, "Greetings & Basics", 0, pack1Vocab, pack1Kanji),
            FoundationsPack(2, "Numbers & Counting", 0, pack2Vocab, pack2Kanji),
            FoundationsPack(3, "Self & Others", 1, pack3Vocab, pack3Kanji),
            FoundationsPack(4, "Places & Things", 1, pack4Vocab, pack4Kanji)
        )

        val packs = packDefs.map { def ->
            val vocabCards = def.vocab.mapIndexed { index, v ->
                val cardId = "${trackId}_v_${def.level}_${index + 1}"
                CardEntity(
                    cardId = cardId,
                    type = CardType.VOCAB_READING,
                    prompt = v.term,
                    canonicalAnswer = v.meaning,
                    acceptedAnswersRaw = listOf(v.meaning, v.meaning.lowercase(Locale.US)).distinct().joinToString("|"),
                    reading = v.reading,
                    meaning = v.meaning,
                    promptFurigana = null,
                    choicesRaw = (v.choices + v.meaning).distinct().joinToString("|"),
                    difficulty = def.difficulty,
                    templateId = "tmpl_$cardId"
                )
            }
            val kanjiCards = def.kanji.mapIndexed { index, k ->
                val cardId = "${trackId}_r_${def.level}_${index + 1}"
                CardEntity(
                    cardId = cardId,
                    type = CardType.KANJI_READING,
                    prompt = k.symbol,
                    canonicalAnswer = k.reading,
                    acceptedAnswersRaw = k.reading,
                    reading = k.reading,
                    meaning = k.meaning,
                    promptFurigana = null,
                    choicesRaw = (k.readingChoices + k.reading).distinct().joinToString("|"),
                    difficulty = def.difficulty,
                    templateId = "tmpl_$cardId"
                )
            }
            PackSeed(
                level = def.level,
                title = def.title,
                cards = vocabCards + kanjiCards
            )
        }

        return buildTrack(
            trackId = trackId,
            title = "Foundations",
            description = "Start here — greetings, numbers, and everyday words with no writing required.",
            accentColor = "#4CAF50",
            displayOrder = 0,
            minTotalScore = 70,
            minHandwritingScore = 0,
            packs = packs
        )
    }

    private fun buildJlptN4Track(): TrackSeed {
        val trackId = "jlpt_n4_core"
        val packs = listOf(
            PackSeed(
                level = 1,
                title = "N4 Foundation",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 1,
                    difficulty = 3,
                    kanji = listOf(
                        KanjiSeed("\u7d4c", "manage"),
                        KanjiSeed("\u9a13", "experience"),
                        KanjiSeed("\u5909", "change")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u6e96\u5099",
                            reading = "junbi",
                            meaning = "preparation",
                            choices = listOf("deadline", "preparation", "direction", "promise")
                        ),
                        VocabSeed(
                            term = "\u4e88\u5b9a",
                            reading = "yotei",
                            meaning = "schedule",
                            choices = listOf("schedule", "mistake", "entrance", "distance")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Choose the best connector: Isogashii ( ) ikemasen.",
                        promptFurigana = "忙{いそが}しい（　）行{い}けません。",
                        answer = "node",
                        choices = listOf("kara", "node", "to", "kedo"),
                        meaning = "reason connector"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Fill blank: Kaigi wa san-ji ( ____ ) hajimarimasu.",
                        promptFurigana = "会議{かいぎ}は三時{さんじ}（　）始{はじ}まります。",
                        answer = "kara",
                        accepted = listOf("kara"),
                        meaning = "time starting point"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kaigi wa san-ji kara hajimarimasu.",
                        promptFurigana = "会議{かいぎ}は三時{さんじ}から始{はじ}まります。",
                        answer = "The meeting starts at 3:00.",
                        choices = listOf(
                            "The meeting starts at 3:00.",
                            "The meeting ended at 3:00.",
                            "The meeting starts tomorrow.",
                            "The meeting starts after lunch."
                        ),
                        meaning = "basic scheduling sentence"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Today I will prepare the report.",
                        answer = "今日は報告を準備します",
                        accepted = listOf(
                            "今日は報告を準備します",
                            "きょうはほうこくをじゅんびします",
                            "kyou wa houkoku o junbi shimasu",
                            "kyou houkoku o junbi shimasu"
                        ),
                        meaning = "N4 production baseline"
                    )
                )
            ),
            PackSeed(
                level = 2,
                title = "N4 Connectors",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 2,
                    difficulty = 4,
                    kanji = listOf(
                        KanjiSeed("\u9023", "connect"),
                        KanjiSeed("\u7d50", "tie"),
                        KanjiSeed("\u679c", "result")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u9023\u7d61",
                            reading = "renraku",
                            meaning = "contact",
                            choices = listOf("contact", "ticket", "weather", "floor")
                        ),
                        VocabSeed(
                            term = "\u7d50\u679c",
                            reading = "kekka",
                            meaning = "result",
                            choices = listOf("result", "excuse", "rule", "tool")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Kono keikaku wa muzukashii ( ) omou.",
                        promptFurigana = "この計画{けいかく}は難{むずか}しい（　）思{おも}う。",
                        answer = "to",
                        choices = listOf("ni", "to", "de", "made"),
                        meaning = "quotation marker"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Moshi okureru nara, sugu ( ____ ) kudasai.",
                        promptFurigana = "もし遅{おく}れるなら、すぐ（　）ください。",
                        answer = "renraku shite",
                        accepted = listOf("renraku shite"),
                        meaning = "request with te-form"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kekka wa raishuu happyou saremasu.",
                        promptFurigana = "結果{けっか}は来週{らいしゅう}発表{はっぴょう}されます。",
                        answer = "The results will be announced next week.",
                        choices = listOf(
                            "The results will be announced next week.",
                            "The results were sent yesterday.",
                            "The results are not ready.",
                            "The results were wrong."
                        ),
                        meaning = "result announcement"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Please contact me after the meeting.",
                        answer = "会議の後で連絡してください",
                        accepted = listOf(
                            "会議の後で連絡してください",
                            "会議の後に連絡してください",
                            "かいぎのあとでれんらくしてください",
                            "kaigi no ato de renraku shite kudasai",
                            "renraku shite kudasai kaigi no ato de"
                        ),
                        meaning = "te-form request practice"
                    )
                )
            ),
            PackSeed(
                level = 3,
                title = "N4 Explanation",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 3,
                    difficulty = 5,
                    kanji = listOf(
                        KanjiSeed("\u8aac", "explain"),
                        KanjiSeed("\u660e", "clear"),
                        KanjiSeed("\u610f", "meaning")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u8aac\u660e",
                            reading = "setsumei",
                            meaning = "explanation",
                            choices = listOf("explanation", "decision", "record", "reservation")
                        ),
                        VocabSeed(
                            term = "\u610f\u898b",
                            reading = "iken",
                            meaning = "opinion",
                            choices = listOf("opinion", "holiday", "salary", "experience")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Sensei no setsumei wa wakariyasui ( ).",
                        promptFurigana = "先生{せんせい}の説明{せつめい}は分{わ}かりやすい（　）。",
                        answer = "desu",
                        choices = listOf("desu", "ni", "made", "kara"),
                        meaning = "formal statement"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Watashi no iken o ( ____ ) ii desu ka.",
                        promptFurigana = "私{わたし}の意見{いけん}を（　）いいですか。",
                        answer = "itte mo",
                        accepted = listOf("itte mo"),
                        meaning = "permission pattern"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kono bun no imi o setsumei shite kudasai.",
                        promptFurigana = "この文{ぶん}の意味{いみ}を説明{せつめい}してください。",
                        answer = "Please explain the meaning of this sentence.",
                        choices = listOf(
                            "Please explain the meaning of this sentence.",
                            "Please repeat this sentence.",
                            "Please translate this sentence later.",
                            "Please read this sentence quickly."
                        ),
                        meaning = "instruction comprehension"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: I explained my opinion clearly.",
                        answer = "私は意見を分かりやすく説明しました",
                        accepted = listOf(
                            "私は意見を分かりやすく説明しました",
                            "意見を分かりやすく説明しました",
                            "わたしはいけんをわかりやすくせつめいしました",
                            "watashi wa iken o wakariyasuku setsumei shimashita",
                            "iken o wakariyasuku setsumei shimashita"
                        ),
                        meaning = "opinion production"
                    )
                )
            ),
            PackSeed(
                level = 4,
                title = "N4 Reading",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 4,
                    difficulty = 6,
                    kanji = listOf(
                        KanjiSeed("\u6700", "most"),
                        KanjiSeed("\u8fd1", "recent"),
                        KanjiSeed("\u7531", "reason")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u6700\u8fd1",
                            reading = "saikin",
                            meaning = "recently",
                            choices = listOf("recently", "suddenly", "exactly", "quietly")
                        ),
                        VocabSeed(
                            term = "\u7406\u7531",
                            reading = "riyuu",
                            meaning = "reason",
                            choices = listOf("reason", "holiday", "chance", "condition")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Mada shukudai ga owaranai ( ) nemasen.",
                        promptFurigana = "まだ宿題{しゅくだい}が終{お}わらない（　）寝{ね}ません。",
                        answer = "node",
                        choices = listOf("node", "made", "to", "dake"),
                        meaning = "cause and effect"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Okureta riyuu o ( ____ ) kudasai.",
                        promptFurigana = "遅{おく}れた理由{りゆう}を（　）ください。",
                        answer = "oshiete",
                        accepted = listOf("oshiete"),
                        meaning = "te-form request"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Saikin wa densen ga konde imasu.",
                        promptFurigana = "最近{さいきん}は電車{でんしゃ}が混{こ}んでいます。",
                        answer = "Recently, the train line has been crowded.",
                        choices = listOf(
                            "Recently, the train line has been crowded.",
                            "Recently, the train line was canceled.",
                            "Recently, the station closed early.",
                            "Recently, the station was moved."
                        ),
                        meaning = "reading fluency"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Tell me the reason for the delay.",
                        answer = "遅延の理由を教えてください",
                        accepted = listOf(
                            "遅延の理由を教えてください",
                            "理由を教えてください",
                            "ちえんのりゆうをおしえてください",
                            "chien no riyuu o oshiete kudasai",
                            "riyuu o oshiete kudasai"
                        ),
                        meaning = "reason inquiry"
                    )
                )
            ),
            PackSeed(
                level = 5,
                title = "N4 Mock",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 5,
                    difficulty = 7,
                    kanji = listOf(
                        KanjiSeed("\u5fc5", "certain"),
                        KanjiSeed("\u8981", "important"),
                        KanjiSeed("\u984c", "topic")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u5fc5\u8981",
                            reading = "hitsuyou",
                            meaning = "necessary",
                            choices = listOf("necessary", "possible", "separate", "careful")
                        ),
                        VocabSeed(
                            term = "\u554f\u984c",
                            reading = "mondai",
                            meaning = "problem",
                            choices = listOf("problem", "meeting", "library", "answer")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Shiken mae wa fukushuu ( ) hou ga ii.",
                        promptFurigana = "試験前{しけんまえ}は復習{ふくしゅう}（　）ほうがいい。",
                        answer = "shita",
                        choices = listOf("shita", "suru", "shite", "shiyou"),
                        meaning = "advice pattern"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Kono mondai ni kotae o ( ____ ) kudasai.",
                        promptFurigana = "この問題{もんだい}に答{こた}えを（　）ください。",
                        answer = "kaite",
                        accepted = listOf("kaite"),
                        meaning = "instruction form"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Shiken ni wa jikan kanri ga hitsuyou desu.",
                        promptFurigana = "試験{しけん}には時間管理{じかんかんり}が必要{ひつよう}です。",
                        answer = "Time management is necessary for the exam.",
                        choices = listOf(
                            "Time management is necessary for the exam.",
                            "Time management is optional for the exam.",
                            "The exam has no time limit.",
                            "The exam starts next month."
                        ),
                        meaning = "exam strategy sentence"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: This question is difficult but important.",
                        answer = "この問題は難しいですが重要です",
                        accepted = listOf(
                            "この問題は難しいですが重要です",
                            "問題は難しいが重要です",
                            "このもんだいはむずかしいですがじゅうようです",
                            "kono mondai wa muzukashii desu ga juuyou desu",
                            "mondai wa muzukashii ga juuyou desu"
                        ),
                        meaning = "contrast and emphasis"
                    )
                )
            )
        )
        return buildTrack(
            trackId = trackId,
            title = "JLPT N4 Builder",
            description = "N4 progression with kanji production, grammar checks, and reading-style prompts.",
            accentColor = "#3D7BA8",
            displayOrder = 5,
            minTotalScore = 80,
            minHandwritingScore = 68,
            packs = packs
        )
    }

    private fun buildJlptN3Track(): TrackSeed {
        val trackId = "jlpt_n3_core"
        val packs = listOf(
            PackSeed(
                level = 1,
                title = "N3 Foundation",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 1,
                    difficulty = 6,
                    kanji = listOf(
                        KanjiSeed("\u8cc7", "resource"),
                        KanjiSeed("\u6e90", "source"),
                        KanjiSeed("\u7387", "rate")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u8cc7\u6e90",
                            reading = "shigen",
                            meaning = "resource",
                            choices = listOf("resource", "edition", "policy", "proposal")
                        ),
                        VocabSeed(
                            term = "\u52b9\u7387",
                            reading = "kouritsu",
                            meaning = "efficiency",
                            choices = listOf("efficiency", "disaster", "opinion", "permission")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Kono houhou wa kouritsu ga ii ( ) omoimasu.",
                        promptFurigana = "この方法{ほうほう}は効率{こうりつ}がいい（　）思{おも}います。",
                        answer = "to",
                        choices = listOf("to", "de", "ni", "yori"),
                        meaning = "quoted thought"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Shiryou o atsumeru tame ni ( ____ ) hitsuyou ga aru.",
                        promptFurigana = "資料{しりょう}を集{あつ}めるために（　）必要{ひつよう}がある。",
                        answer = "chousa ga",
                        accepted = listOf("chousa ga"),
                        meaning = "purpose and requirement"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Shigen no setsuyaku ga hitsuyou desu.",
                        promptFurigana = "資源{しげん}の節約{せつやく}が必要{ひつよう}です。",
                        answer = "Conserving resources is necessary.",
                        choices = listOf(
                            "Conserving resources is necessary.",
                            "Resources are unlimited.",
                            "Resources are easy to find.",
                            "Resources are expensive to ship."
                        ),
                        meaning = "N3 policy statement"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: We should use resources carefully.",
                        answer = "shigen o chuui shite tsukau beki desu",
                        accepted = listOf(
                            "shigen o chuui shite tsukau beki desu",
                            "shigen o taisetsu ni tsukau beki desu"
                        ),
                        meaning = "recommendation form"
                    )
                )
            ),
            PackSeed(
                level = 2,
                title = "N3 Operations",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 2,
                    difficulty = 7,
                    kanji = listOf(
                        KanjiSeed("\u63a1", "adopt"),
                        KanjiSeed("\u7528", "use"),
                        KanjiSeed("\u7248", "edition")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u63a1\u7528",
                            reading = "saiyou",
                            meaning = "hiring",
                            choices = listOf("hiring", "arrival", "warning", "approach")
                        ),
                        VocabSeed(
                            term = "\u7248\u672c",
                            reading = "hanbon",
                            meaning = "edition",
                            choices = listOf("edition", "opinion", "presentation", "culture")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Atarashii an o ( ) suru yotei desu.",
                        promptFurigana = "新{あたら}しい案{あん}を（　）する予定{よてい}です。",
                        answer = "saiyou",
                        choices = listOf("saiyou", "shuuryou", "kakunin", "teian"),
                        meaning = "action selection"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Kono shiryou wa saishin-ban ni ( ____ ) kudasai.",
                        promptFurigana = "この資料{しりょう}は最新版{さいしんばん}に（　）ください。",
                        answer = "koushin shite",
                        accepted = listOf("koushin shite"),
                        meaning = "update request"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kaisha wa rainen no saiyou keikaku o happyou shimashita.",
                        promptFurigana = "会社{かいしゃ}は来年{らいねん}の採用計画{さいようけいかく}を発表{はっぴょう}しました。",
                        answer = "The company announced next year's hiring plan.",
                        choices = listOf(
                            "The company announced next year's hiring plan.",
                            "The company canceled hiring this year.",
                            "The company changed its office location.",
                            "The company reduced training time."
                        ),
                        meaning = "business reading"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Please update this file to the latest edition.",
                        answer = "kono fairu o saishin-ban ni koushin shite kudasai",
                        accepted = listOf(
                            "kono fairu o saishin-ban ni koushin shite kudasai",
                            "saishin-ban ni koushin shite kudasai"
                        ),
                        meaning = "operational instruction"
                    )
                )
            ),
            PackSeed(
                level = 3,
                title = "N3 Analysis",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 3,
                    difficulty = 8,
                    kanji = listOf(
                        KanjiSeed("\u8b70", "discussion"),
                        KanjiSeed("\u8ad6", "argument"),
                        KanjiSeed("\u67fb", "investigate")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u4f1a\u8b70",
                            reading = "kaigi",
                            meaning = "meeting",
                            choices = listOf("meeting", "budget", "template", "support")
                        ),
                        VocabSeed(
                            term = "\u8abf\u67fb",
                            reading = "chousa",
                            meaning = "survey",
                            choices = listOf("survey", "contract", "delivery", "rule")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Kono mondai ni tsuite ( ) shimashou.",
                        promptFurigana = "この問題{もんだい}について（　）しましょう。",
                        answer = "giron",
                        choices = listOf("giron", "yoyaku", "seiri", "renshuu"),
                        meaning = "discussion vocabulary"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Saigo ni kekka o ( ____ ) houkoku shimasu.",
                        promptFurigana = "最後{さいご}に結果{けっか}を（　）報告{ほうこく}します。",
                        answer = "matomete",
                        accepted = listOf("matomete"),
                        meaning = "sequence expression"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Chousa no kekka ni motozuite teian o tsukurimasu.",
                        promptFurigana = "調査{ちょうさ}の結果{けっか}に基{もと}づいて提案{ていあん}を作{つく}ります。",
                        answer = "We will make a proposal based on the survey results.",
                        choices = listOf(
                            "We will make a proposal based on the survey results.",
                            "We will ignore the survey results.",
                            "We will cancel the proposal immediately.",
                            "We will delay the survey by a month."
                        ),
                        meaning = "analysis workflow"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Let's discuss this issue carefully.",
                        answer = "kono mondai o chuubun ni giron shimashou",
                        accepted = listOf(
                            "kono mondai o chuubun ni giron shimashou",
                            "chuubun ni giron shimashou"
                        ),
                        meaning = "meeting language"
                    )
                )
            ),
            PackSeed(
                level = 4,
                title = "N3 Management",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 4,
                    difficulty = 9,
                    kanji = listOf(
                        KanjiSeed("\u76e3", "supervise"),
                        KanjiSeed("\u7dad", "maintain"),
                        KanjiSeed("\u5fdc", "respond")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u7ba1\u7406",
                            reading = "kanri",
                            meaning = "management",
                            choices = listOf("management", "deadline", "contract", "incident")
                        ),
                        VocabSeed(
                            term = "\u5bfe\u5fdc",
                            reading = "taiou",
                            meaning = "response",
                            choices = listOf("response", "effort", "summary", "culture")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Toraburu ni sugu ( ) dekiru you ni junbi shite kudasai.",
                        promptFurigana = "トラブルにすぐ（　）できるように準備{じゅんび}してください。",
                        answer = "taiou",
                        choices = listOf("taiou", "teian", "saiyou", "hyouka"),
                        meaning = "rapid response expression"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Hinshitsu o iji suru tame ni ( ____ ) hitsuyou da.",
                        promptFurigana = "品質{ひんしつ}を維持{いじ}するために（　）必要{ひつよう}だ。",
                        answer = "kanri ga",
                        accepted = listOf("kanri ga"),
                        meaning = "maintenance condition"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Shisutemu no iji to kanri wa juuyou desu.",
                        promptFurigana = "システムの維持{いじ}と管理{かんり}は重要{じゅうよう}です。",
                        answer = "System maintenance and management are important.",
                        choices = listOf(
                            "System maintenance and management are important.",
                            "System maintenance is optional.",
                            "System management is automatic.",
                            "System quality cannot be measured."
                        ),
                        meaning = "operations reading"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: We need a fast response to this issue.",
                        answer = "kono mondai ni wa hayai taiou ga hitsuyou desu",
                        accepted = listOf(
                            "kono mondai ni wa hayai taiou ga hitsuyou desu",
                            "hayai taiou ga hitsuyou desu"
                        ),
                        meaning = "critical response phrase"
                    )
                )
            ),
            PackSeed(
                level = 5,
                title = "N3 Mock",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 5,
                    difficulty = 10,
                    kanji = listOf(
                        KanjiSeed("\u9700", "demand"),
                        KanjiSeed("\u7d66", "supply"),
                        KanjiSeed("\u6a21", "scale")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u9700\u8981",
                            reading = "juyou",
                            meaning = "demand",
                            choices = listOf("demand", "release", "improvement", "delivery")
                        ),
                        VocabSeed(
                            term = "\u898f\u6a21",
                            reading = "kibo",
                            meaning = "scale",
                            choices = listOf("scale", "cost", "difference", "candidate")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Juyou ga fueru ni tsurete, kyoukyuu mo ( ).",
                        promptFurigana = "需要{じゅよう}が増{ふ}えるにつれて、供給{きょうきゅう}も（　）。",
                        answer = "hitsuyou ni naru",
                        choices = listOf("hitsuyou ni naru", "yasuku naru", "muzukashiku nai", "owaru"),
                        meaning = "parallel change"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Shijou no henka ni ( ____ ) taiou shinakereba naranai.",
                        promptFurigana = "市場{しじょう}の変化{へんか}に（　）対応{たいおう}しなければならない。",
                        answer = "awasete",
                        accepted = listOf("awasete"),
                        meaning = "must adapt expression"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kono purojekuto wa daikibo de, juubun na kanri ga irimasu.",
                        promptFurigana = "このプロジェクトは大規模{だいきぼ}で、十分{じゅうぶん}な管理{かんり}が要{い}ります。",
                        answer = "This project is large-scale and requires careful management.",
                        choices = listOf(
                            "This project is large-scale and requires careful management.",
                            "This project is small and simple.",
                            "This project has already finished.",
                            "This project has no clear goal."
                        ),
                        meaning = "advanced project statement"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Demand increased, so supply had to expand.",
                        answer = "juyou ga fueta node kyoukyuu o kakudai suru hitsuyou ga atta",
                        accepted = listOf(
                            "juyou ga fueta node kyoukyuu o kakudai suru hitsuyou ga atta",
                            "juyou ga fueta kara kyoukyuu o kakudai shita"
                        ),
                        meaning = "cause and response"
                    )
                )
            )
        )
        return buildTrack(
            trackId = trackId,
            title = "JLPT N3 Bridge",
            description = "Intermediate N3 content with production-heavy kanji and applied comprehension prompts.",
            accentColor = "#4A6172",
            displayOrder = 6,
            minTotalScore = 82,
            minHandwritingScore = 70,
            packs = packs
        )
    }

    private fun buildDailyChallengeTrack(): TrackSeed {
        val trackId = dailyChallengeTrackId
        val packs = listOf(
            PackSeed(
                level = 1,
                title = "Daily Boosters",
                cards = buildPackCards(
                    trackId = trackId,
                    level = 1,
                    difficulty = 4,
                    kanji = listOf(
                        KanjiSeed("\u99c5", "station"),
                        KanjiSeed("\u65c5", "travel"),
                        KanjiSeed("\u7d04", "promise")
                    ),
                    vocab = listOf(
                        VocabSeed(
                            term = "\u6539\u672d",
                            reading = "kaisatsu",
                            meaning = "ticket gate",
                            choices = listOf("ticket gate", "crosswalk", "office desk", "menu")
                        ),
                        VocabSeed(
                            term = "\u4e57\u63db",
                            reading = "norikae",
                            meaning = "transfer",
                            choices = listOf("transfer", "discount", "deadline", "receipt")
                        )
                    ),
                    grammarChoice = GrammarSeed(
                        prompt = "Choose the best request ending: Mite ( )",
                        promptFurigana = "見{み}て（　）",
                        answer = "kudasai",
                        choices = listOf("kudasai", "mashita", "masen", "deshita"),
                        meaning = "daily polite request"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Fill blank: Mou ichido ( ____ ) kudasai.",
                        promptFurigana = "もう一度{いちど}（　）ください。",
                        answer = "itte",
                        accepted = listOf("itte"),
                        meaning = "repeat request"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Norikae wa tsugi no eki desu.",
                        promptFurigana = "乗換{のりか}えは次{つぎ}の駅{えき}です。",
                        answer = "The transfer is at the next station.",
                        choices = listOf(
                            "The transfer is at the next station.",
                            "The station is closed today.",
                            "The transfer was canceled.",
                            "The transfer takes an hour."
                        ),
                        meaning = "navigation booster"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build: Please tell me where the ticket gate is.",
                        answer = "kaisatsu wa doko ka oshiete kudasai",
                        accepted = listOf(
                            "kaisatsu wa doko ka oshiete kudasai",
                            "doko ka oshiete kudasai"
                        ),
                        meaning = "daily challenge-specific seed"
                    )
                )
            )
        )
        return buildTrack(
            trackId = trackId,
            title = "Daily Challenge Boosters",
            description = "Compact daily-only prompts blended into challenge generation for variety and reinforcement.",
            accentColor = "#6D8B4C",
            displayOrder = 7,
            minTotalScore = 75,
            minHandwritingScore = 65,
            packs = packs
        )
    }

    private fun buildDailyLifeTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "daily_life_core",
            title = "Daily Life",
            description = "Theme-specific daily life progression from home routines to practical dialogues.",
            accentColor = "#80624D",
            displayOrder = 8,
            levelTitles = listOf(
                "Home",
                "School",
                "Work",
                "Social",
                "Real Dialogues",
                "Situational Patterns",
                "Nuanced Responses",
                "Advanced Daily Flow"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u5bb6", "house"),
                KanjiSeed("\u90e8", "section"),
                KanjiSeed("\u5ba4", "room"),
                KanjiSeed("\u671d", "morning"),
                KanjiSeed("\u591c", "night"),
                KanjiSeed("\u6642", "time"),
                KanjiSeed("\u53cb", "friend"),
                KanjiSeed("\u8a71", "talk"),
                KanjiSeed("\u4f4f", "live"),
                KanjiSeed("\u65cf", "family"),
                KanjiSeed("\u96fb", "electricity"),
                KanjiSeed("\u5929", "sky"),
                KanjiSeed("\u4f11", "rest"),
                KanjiSeed("\u6b69", "walk"),
                KanjiSeed("\u6708", "moon/month"),
                KanjiSeed("\u706b", "fire"),
                KanjiSeed("\u91d1", "gold/money"),
                KanjiSeed("\u4eca", "now"),
                KanjiSeed("\u524d", "before/front"),
                KanjiSeed("\u898b", "see/look"),
                KanjiSeed("\u50cd", "work"),
                KanjiSeed("\u8cb7", "buy"),
                KanjiSeed("\u4f1a", "meet"),
                KanjiSeed("\u5b66", "study")
            ),
            vocabPool = listOf(
                VocabSeed("\u53f0\u6240", "daidokoro", "kitchen", listOf("kitchen", "station", "office", "menu")),
                VocabSeed("\u7384\u95a2", "genkan", "entryway", listOf("entryway", "ticket gate", "line", "counter")),
                VocabSeed("\u6d17\u6fef", "sentaku", "laundry", listOf("laundry", "meeting", "lesson", "fare")),
                VocabSeed("\u6383\u9664", "souji", "cleaning", listOf("cleaning", "boarding", "checkout", "discount")),
                VocabSeed("\u8fd1\u6240", "kinjo", "neighborhood", listOf("neighborhood", "ingredient", "payment", "receipt")),
                VocabSeed("\u4e88\u5b9a", "yotei", "plan", listOf("plan", "platform", "ticket", "dessert")),
                VocabSeed("\u7d04\u675f", "yakusoku", "promise", listOf("promise", "total", "route", "salad")),
                VocabSeed("\u4f1a\u8a71", "kaiwa", "conversation", listOf("conversation", "change", "kitchen", "deadline")),
                VocabSeed("\u652f\u5ea6", "shitaku", "getting ready", listOf("getting ready", "line", "stock", "invoice")),
                VocabSeed("\u5e30\u5b85", "kitaku", "returning home", listOf("returning home", "checkout", "meeting", "station")),
                VocabSeed("\u8cb7\u3044\u7269", "kaimono", "shopping", listOf("shopping", "cooking", "travel", "homework")),
                VocabSeed("\u96fb\u8a71", "denwa", "telephone", listOf("telephone", "television", "transfer", "ticket")),
                VocabSeed("\u5929\u6c17", "tenki", "weather", listOf("weather", "promise", "direction", "price")),
                VocabSeed("\u4f11\u307f", "yasumi", "holiday", listOf("holiday", "cleaning", "lesson", "ingredient")),
                VocabSeed("\u6563\u6b69", "sanpo", "walk", listOf("walk", "meeting", "payment", "route")),
                VocabSeed("\u4eca\u65e5", "kyou", "today", listOf("today", "tomorrow", "yesterday", "next week")),
                VocabSeed("\u660e\u65e5", "ashita", "tomorrow", listOf("tomorrow", "today", "yesterday", "next week")),
                VocabSeed("\u6708\u66dc\u65e5", "getsuyoubi", "Monday", listOf("Monday", "Tuesday", "Friday", "Sunday")),
                VocabSeed("\u706b\u66dc\u65e5", "kayoubi", "Tuesday", listOf("Tuesday", "Monday", "Wednesday", "Thursday")),
                VocabSeed("\u6c34\u66dc\u65e5", "suiyoubi", "Wednesday", listOf("Wednesday", "Thursday", "Monday", "Friday")),
                VocabSeed("\u91d1\u66dc\u65e5", "kinyoubi", "Friday", listOf("Friday", "Saturday", "Monday", "Wednesday")),
                VocabSeed("\u4f1a\u793e", "kaisha", "company", listOf("company", "school", "station", "hospital")),
                VocabSeed("\u5b66\u6821", "gakkou", "school", listOf("school", "company", "library", "station")),
                VocabSeed("\u4ed5\u4e8b", "shigoto", "work/job", listOf("work/job", "hobby", "vacation", "study")),
                VocabSeed("\u52c9\u5f37", "benkyou", "study", listOf("study", "play", "rest", "work")),
                VocabSeed("\u898b\u308b", "miru", "to see/watch", listOf("to see/watch", "to hear", "to go", "to eat")),
                VocabSeed("\u50cd\u304f", "hataraku", "to work", listOf("to work", "to rest", "to play", "to eat")),
                VocabSeed("\u4f1a\u3046", "au", "to meet", listOf("to meet", "to leave", "to buy", "to go"))
            )
        )
    }

    private fun buildFoodTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "food_core",
            title = "Food & Ordering",
            description = "Read menus, order confidently, and stop playing protein roulette.",
            accentColor = "#A86B3D",
            displayOrder = 9,
            levelTitles = listOf(
                "What's on the Menu",
                "Protein Roulette",
                "Sizing & Sides",
                "Takeout & Sold Out",
                "Restaurant Dialogues",
                "Kitchen & Cooking",
                "Dietary Needs",
                "Full Service Flow"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u725b", "cow"),
                KanjiSeed("\u8c5a", "pig"),
                KanjiSeed("\u9d8f", "chicken"),
                KanjiSeed("\u5927", "big"),
                KanjiSeed("\u76db", "serve/pile"),
                KanjiSeed("\u6301", "hold"),
                KanjiSeed("\u58f2", "sell"),
                KanjiSeed("\u5207", "cut"),
                KanjiSeed("\u5e2d", "seat"),
                KanjiSeed("\u7159", "smoke"),
                KanjiSeed("\u5b9a", "fixed"),
                KanjiSeed("\u98df", "eat"),
                KanjiSeed("\u98f2", "drink"),
                KanjiSeed("\u9aa8", "bone"),
                KanjiSeed("\u9b5a", "fish"),
                KanjiSeed("\u91ce", "field"),
                KanjiSeed("\u83dc", "vegetable"),
                KanjiSeed("\u9eba", "noodle"),
                KanjiSeed("\u4e3c", "rice bowl"),
                KanjiSeed("\u8f9b", "spicy"),
                KanjiSeed("\u5473", "flavor"),
                KanjiSeed("\u4e88", "advance"),
                KanjiSeed("\u7c73", "rice"),
                KanjiSeed("\u6c34", "water"),
                KanjiSeed("\u8089", "meat"),
                KanjiSeed("\u671d", "morning"),
                KanjiSeed("\u591c", "night"),
                KanjiSeed("\u4eca", "now"),
                KanjiSeed("\u8336", "tea")
            ),
            vocabPool = listOf(
                VocabSeed("\u30e1\u30cb\u30e5\u30fc", "menyuu", "menu", listOf("menu", "receipt", "ticket", "platform")),
                VocabSeed("\u304a\u3059\u3059\u3081", "osusume", "recommendation", listOf("recommendation", "discount", "receipt", "delay")),
                VocabSeed("\u725b\u8089", "gyuuniku", "beef", listOf("beef", "pork", "chicken", "fish")),
                VocabSeed("\u8c5a\u8089", "butaniku", "pork", listOf("pork", "beef", "chicken", "vegetables")),
                VocabSeed("\u9d8f\u8089", "toriniku", "chicken", listOf("chicken", "beef", "fish", "pork")),
                VocabSeed("\u5927\u76db", "oomori", "large serving", listOf("large serving", "small portion", "half price", "takeout")),
                VocabSeed("\u6301\u3061\u5e30\u308a", "mochikaeri", "takeout", listOf("takeout", "dine-in", "delivery", "reservation")),
                VocabSeed("\u58f2\u308a\u5207\u308c", "urikire", "sold out", listOf("sold out", "in stock", "half price", "new item")),
                VocabSeed("\u713c\u304d\u9ce5", "yakitori", "grilled chicken", listOf("grilled chicken", "fried rice", "ramen", "sushi")),
                VocabSeed("\u3064\u3051\u9eba", "tsukemen", "dipping noodles", listOf("dipping noodles", "fried noodles", "udon", "soba")),
                VocabSeed("\u8c5a\u9aa8", "tonkotsu", "pork bone broth", listOf("pork bone broth", "soy sauce", "salt", "miso")),
                VocabSeed("\u7981\u7159", "kinen", "no smoking", listOf("no smoking", "smoking area", "exit", "entrance")),
                VocabSeed("\u5b9a\u98df", "teishoku", "set meal", listOf("set meal", "single item", "dessert", "appetizer")),
                VocabSeed("\u6ce8\u6587", "chuumon", "order", listOf("order", "receipt", "payment", "menu")),
                VocabSeed("\u4f1a\u8a08", "kaikei", "bill", listOf("bill", "tip", "tax", "change")),
                VocabSeed("\u9b5a", "sakana", "fish", listOf("fish", "meat", "rice", "soup")),
                VocabSeed("\u91ce\u83dc", "yasai", "vegetables", listOf("vegetables", "fruit", "meat", "rice")),
                VocabSeed("\u30e9\u30fc\u30e1\u30f3", "raamen", "ramen", listOf("ramen", "udon", "soba", "curry")),
                VocabSeed("\u4e3c", "donburi", "rice bowl", listOf("rice bowl", "soup bowl", "plate", "cup")),
                VocabSeed("\u8f9b\u3044", "karai", "spicy", listOf("spicy", "sweet", "sour", "salty")),
                VocabSeed("\u4e88\u7d04", "yoyaku", "reservation", listOf("reservation", "cancellation", "order", "receipt")),
                VocabSeed("\u5473", "aji", "taste/flavor", listOf("taste/flavor", "color", "smell", "sound")),
                VocabSeed("\u304a\u6c34", "omizu", "water", listOf("water", "tea", "juice", "beer")),
                VocabSeed("\u3054\u98ef", "gohan", "rice/meal", listOf("rice/meal", "bread", "noodles", "dessert")),
                VocabSeed("\u30c7\u30b6\u30fc\u30c8", "dezaato", "dessert", listOf("dessert", "appetizer", "main course", "salad")),
                VocabSeed("\u98df\u3079\u653e\u984c", "tabehoudai", "all-you-can-eat", listOf("all-you-can-eat", "set meal", "takeout", "reservation")),
                VocabSeed("\u3044\u305f\u3060\u304d\u307e\u3059", "itadakimasu", "meal greeting", listOf("meal greeting", "goodbye", "thank you", "excuse me")),
                VocabSeed("\u304a\u52d8\u5b9a", "okanjou", "check/bill", listOf("check/bill", "menu", "receipt", "tip")),
                VocabSeed("\u8089", "niku", "meat", listOf("meat", "fish", "rice", "vegetable")),
                VocabSeed("\u671d\u3054\u306f\u3093", "asagohan", "breakfast", listOf("breakfast", "lunch", "dinner", "snack")),
                VocabSeed("\u665a\u3054\u306f\u3093", "bangohan", "dinner", listOf("dinner", "breakfast", "lunch", "snack")),
                VocabSeed("\u304a\u8336", "ocha", "tea", listOf("tea", "water", "coffee", "juice")),
                VocabSeed("\u4eca\u65e5\u306e\u304a\u3059\u3059\u3081", "kyou no osusume", "today's recommendation", listOf("today's recommendation", "set meal", "dessert", "drink menu"))
            )
        )
    }

    private fun buildTransportTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "transport_core",
            title = "Trains & Getting Around",
            description = "Stop following crowds and start reading the signs. Exit, express, transfer, last train.",
            accentColor = "#5B7891",
            displayOrder = 10,
            levelTitles = listOf(
                "Finding Your Way",
                "Express or Local",
                "Transfers & Connections",
                "Service Alerts",
                "Station Dialogues",
                "Complex Routes",
                "Travel Planning",
                "Commuter Confidence"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u51fa", "exit"),
                KanjiSeed("\u5165", "enter"),
                KanjiSeed("\u53e3", "gate/mouth"),
                KanjiSeed("\u6025", "express/urgent"),
                KanjiSeed("\u5404", "each"),
                KanjiSeed("\u99c5", "station"),
                KanjiSeed("\u4e57", "ride"),
                KanjiSeed("\u63db", "exchange"),
                KanjiSeed("\u904b", "operate/carry"),
                KanjiSeed("\u8ee2", "turn/roll"),
                KanjiSeed("\u6700", "most"),
                KanjiSeed("\u7d42", "end/finish"),
                KanjiSeed("\u512a", "gentle/priority"),
                KanjiSeed("\u5148", "ahead/previous"),
                KanjiSeed("\u5317", "north"),
                KanjiSeed("\u5357", "south"),
                KanjiSeed("\u6771", "east"),
                KanjiSeed("\u897f", "west"),
                KanjiSeed("\u756a", "number"),
                KanjiSeed("\u7dda", "line"),
                KanjiSeed("\u767a", "depart"),
                KanjiSeed("\u7740", "arrive"),
                KanjiSeed("\u7247", "one-way"),
                KanjiSeed("\u8fd4", "return"),
                KanjiSeed("\u9580", "gate"),
                KanjiSeed("\u884c", "go"),
                KanjiSeed("\u6765", "come"),
                KanjiSeed("\u96fb", "electric")
            ),
            vocabPool = listOf(
                VocabSeed("\u51fa\u53e3", "deguchi", "exit", listOf("exit", "entrance", "ticket gate", "platform")),
                VocabSeed("\u5165\u53e3", "iriguchi", "entrance", listOf("entrance", "exit", "transfer", "platform")),
                VocabSeed("\u6025\u884c", "kyuukou", "express train", listOf("express train", "local train", "bullet train", "bus")),
                VocabSeed("\u5404\u99c5\u505c\u8eca", "kakueki teisha", "local train", listOf("local train", "express train", "last train", "first train")),
                VocabSeed("\u4e57\u63db", "norikae", "transfer", listOf("transfer", "exit", "ticket", "delay")),
                VocabSeed("\u884c\u5148", "yukisaki", "destination", listOf("destination", "departure", "transfer", "delay")),
                VocabSeed("\u904b\u8ee2\u898b\u5408\u308f\u305b", "unten miawase", "service suspended", listOf("service suspended", "on time", "delayed", "canceled")),
                VocabSeed("\u6700\u7d42", "saishuu", "last (train)", listOf("last (train)", "first (train)", "express", "local")),
                VocabSeed("\u512a\u5148\u5e2d", "yuusenseki", "priority seat", listOf("priority seat", "reserved seat", "free seat", "exit seat")),
                VocabSeed("\u9045\u5ef6", "chien", "delay", listOf("delay", "on time", "canceled", "suspended")),
                VocabSeed("\u6539\u672d", "kaisatsu", "ticket gate", listOf("ticket gate", "exit", "platform", "counter")),
                VocabSeed("\u5207\u7b26", "kippu", "ticket", listOf("ticket", "receipt", "pass", "fare")),
                VocabSeed("\u6642\u523b\u8868", "jikokuhyou", "timetable", listOf("timetable", "ticket", "fare chart", "map")),
                VocabSeed("\u65b9\u9762", "houmen", "direction (bound for)", listOf("direction (bound for)", "transfer", "exit", "entrance")),
                VocabSeed("\u9045\u5ef6\u8a3c\u660e\u66f8", "chien shoumeisho", "delay certificate", listOf("delay certificate", "ticket refund", "lost item form", "complaint form")),
                VocabSeed("\u5317\u53e3", "kitaguchi", "north exit", listOf("north exit", "south exit", "east exit", "west exit")),
                VocabSeed("\u5357\u53e3", "minamiguchi", "south exit", listOf("south exit", "north exit", "main exit", "back exit")),
                VocabSeed("\u30db\u30fc\u30e0", "hoomu", "platform", listOf("platform", "ticket gate", "exit", "escalator")),
                VocabSeed("\u7247\u9053", "katamichi", "one-way", listOf("one-way", "round trip", "transfer", "express")),
                VocabSeed("\u5f80\u5fa9", "oufuku", "round trip", listOf("round trip", "one-way", "transfer", "express")),
                VocabSeed("\u767a\u8eca", "hassha", "departure", listOf("departure", "arrival", "transfer", "delay")),
                VocabSeed("\u5230\u7740", "touchaku", "arrival", listOf("arrival", "departure", "transfer", "canceled")),
                VocabSeed("\u5feb\u901f", "kaisoku", "rapid train", listOf("rapid train", "local train", "express", "bullet train")),
                VocabSeed("\u7279\u6025", "tokkyuu", "limited express", listOf("limited express", "local train", "rapid", "commuter")),
                VocabSeed("\u904b\u8cc3", "unchin", "fare", listOf("fare", "ticket", "pass", "receipt")),
                VocabSeed("\u5b9a\u671f\u5238", "teikiken", "commuter pass", listOf("commuter pass", "single ticket", "fare card", "receipt")),
                VocabSeed("\u59cb\u767a", "shihatsu", "first train", listOf("first train", "last train", "express", "local")),
                VocabSeed("\u7d42\u96fb", "shuuden", "last train", listOf("last train", "first train", "express", "rapid")),
                VocabSeed("\u96fb\u8eca", "densha", "train", listOf("train", "bus", "taxi", "bicycle")),
                VocabSeed("\u6539\u672d\u53e3", "kaisatsuguchi", "ticket gate", listOf("ticket gate", "exit", "platform", "entrance")),
                VocabSeed("\u884c\u304f", "iku", "to go", listOf("to go", "to come", "to return", "to stop")),
                VocabSeed("\u6765\u308b", "kuru", "to come", listOf("to come", "to go", "to return", "to leave"))
            )
        )
    }

    private fun buildShoppingTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "shopping_core",
            title = "Shopping & Prices",
            description = "Theme-specific shopping packs covering prices, items, payments, comparisons, and customer dialogues.",
            accentColor = "#7F6756",
            displayOrder = 11,
            levelTitles = listOf(
                "Numbers",
                "Items",
                "Payments",
                "Comparisons",
                "Conversations",
                "Returns & Exchanges",
                "Price Negotiation",
                "Customer Scenarios"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u5024", "value"),
                KanjiSeed("\u6570", "number"),
                KanjiSeed("\u91d1", "money"),
                KanjiSeed("\u5e97", "shop"),
                KanjiSeed("\u54c1", "item"),
                KanjiSeed("\u5b89", "cheap"),
                KanjiSeed("\u9ad8", "high"),
                KanjiSeed("\u6bd4", "compare"),
                KanjiSeed("\u6255", "pay"),
                KanjiSeed("\u5ba2", "customer"),
                KanjiSeed("\u888b", "bag"),
                KanjiSeed("\u58f2", "sell"),
                KanjiSeed("\u8cb7", "buy"),
                KanjiSeed("\u9818", "receipt/territory"),
                KanjiSeed("\u8272", "color"),
                KanjiSeed("\u91cd", "heavy/weight"),
                KanjiSeed("\u8efd", "light (weight)"),
                KanjiSeed("\u65b0", "new"),
                KanjiSeed("\u53e4", "old"),
                KanjiSeed("\u7a0e", "tax"),
                KanjiSeed("\u7a7a", "empty"),
                KanjiSeed("\u5305", "wrap"),
                KanjiSeed("\u9078", "choose"),
                KanjiSeed("\u63a2", "search"),
                KanjiSeed("\u5343", "thousand"),
                KanjiSeed("\u767e", "hundred"),
                KanjiSeed("\u4e07", "ten thousand"),
                KanjiSeed("\u5927", "big"),
                KanjiSeed("\u5c0f", "small"),
                KanjiSeed("\u591a", "many"),
                KanjiSeed("\u5c11", "few")
            ),
            vocabPool = listOf(
                VocabSeed("\u4fa1\u683c", "kakaku", "price", listOf("price", "size", "color", "weight")),
                VocabSeed("\u5408\u8a08", "goukei", "total", listOf("total", "subtotal", "tax", "discount")),
                VocabSeed("\u5728\u5eab", "zaiko", "stock", listOf("stock", "sold out", "on order", "limited")),
                VocabSeed("\u652f\u6255\u3044", "shiharai", "payment", listOf("payment", "refund", "deposit", "receipt")),
                VocabSeed("\u73fe\u91d1", "genkin", "cash", listOf("cash", "credit card", "e-money", "points")),
                VocabSeed("\u5272\u5f15", "waribiki", "discount", listOf("discount", "surcharge", "tax", "tip")),
                VocabSeed("\u8fd4\u54c1", "henpin", "return (item)", listOf("return (item)", "exchange", "refund", "repair")),
                VocabSeed("\u4ea4\u63db", "koukan", "exchange", listOf("exchange", "return", "repair", "refund")),
                VocabSeed("\u63a5\u5ba2", "sekkyaku", "customer service", listOf("customer service", "self-service", "delivery", "pickup")),
                VocabSeed("\u8a66\u7740", "shichaku", "fitting/trying on", listOf("fitting/trying on", "purchase", "return", "exchange")),
                VocabSeed("\u30b5\u30a4\u30ba", "saizu", "size", listOf("size", "color", "price", "weight")),
                VocabSeed("\u30ec\u30b8", "reji", "register", listOf("register", "exit", "shelf", "counter")),
                VocabSeed("\u888b", "fukuro", "bag", listOf("bag", "box", "plate", "cup")),
                VocabSeed("\u9818\u53ce\u66f8", "ryoushuusho", "receipt", listOf("receipt", "ticket", "invoice", "warranty card")),
                VocabSeed("\u30bb\u30fc\u30eb", "seeru", "sale", listOf("sale", "return", "exchange", "stock")),
                VocabSeed("\u8272", "iro", "color", listOf("color", "size", "shape", "weight")),
                VocabSeed("\u65b0\u54c1", "shinpin", "brand new", listOf("brand new", "used", "refurbished", "damaged")),
                VocabSeed("\u4e2d\u53e4", "chuuko", "secondhand", listOf("secondhand", "brand new", "limited", "exclusive")),
                VocabSeed("\u7a0e\u8fbc", "zeikomi", "tax included", listOf("tax included", "before tax", "after discount", "free")),
                VocabSeed("\u5305\u88c5", "housou", "wrapping", listOf("wrapping", "bag", "box", "delivery")),
                VocabSeed("\u304a\u91e3\u308a", "otsuri", "change (money)", listOf("change (money)", "receipt", "total", "tax")),
                VocabSeed("\u30af\u30ec\u30b8\u30c3\u30c8\u30ab\u30fc\u30c9", "kurejitto kaado", "credit card", listOf("credit card", "cash", "e-money", "gift card")),
                VocabSeed("\u54c1\u5207\u308c", "shinagire", "out of stock", listOf("out of stock", "in stock", "on sale", "new arrival")),
                VocabSeed("\u304a\u5f97", "otoku", "good deal/bargain", listOf("good deal/bargain", "overpriced", "sold out", "limited")),
                VocabSeed("\u5024\u6bb5", "nedan", "price (tag)", listOf("price (tag)", "barcode", "label", "receipt")),
                VocabSeed("\u304a\u53d6\u308a\u7f6e\u304d", "otorioki", "item hold/reserve", listOf("item hold/reserve", "return", "delivery", "exchange")),
                VocabSeed("\u914d\u9001", "haisou", "delivery/shipping", listOf("delivery/shipping", "pickup", "return", "exchange")),
                VocabSeed("\u9001\u6599", "souryou", "shipping fee", listOf("shipping fee", "handling fee", "tax", "discount")),
                VocabSeed("\u5343\u5186", "sen en", "1,000 yen", listOf("1,000 yen", "100 yen", "10,000 yen", "500 yen")),
                VocabSeed("\u767e\u5186", "hyaku en", "100 yen", listOf("100 yen", "1,000 yen", "10 yen", "500 yen")),
                VocabSeed("\u4e00\u4e07\u5186", "ichiman en", "10,000 yen", listOf("10,000 yen", "1,000 yen", "100 yen", "5,000 yen")),
                VocabSeed("\u5927\u304d\u3044", "ookii", "big/large", listOf("big/large", "small", "heavy", "light")),
                VocabSeed("\u5c0f\u3055\u3044", "chiisai", "small", listOf("small", "big", "many", "few")),
                VocabSeed("\u591a\u3044", "ooi", "many/much", listOf("many/much", "few", "big", "small")),
                VocabSeed("\u5c11\u306a\u3044", "sukunai", "few/little", listOf("few/little", "many", "big", "cheap"))
            )
        )
    }

    private fun buildKonbiniTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "konbini_core",
            title = "Konbini & Labels",
            description = "Read drink labels, survive checkout, and decode limited-edition snacks.",
            accentColor = "#E06820",
            displayOrder = 8,
            levelTitles = listOf(
                "Drinks & Snacks",
                "Checkout Phrases",
                "Deals & Labels",
                "Allergy & Ingredients",
                "Konbini Conversations",
                "Seasonal Specials",
                "Health Labels",
                "Konbini Pro"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u7cd6", "sugar"),
                KanjiSeed("\u7518", "sweet"),
                KanjiSeed("\u8f9b", "spicy"),
                KanjiSeed("\u6e29", "warm"),
                KanjiSeed("\u888b", "bag"),
                KanjiSeed("\u534a", "half"),
                KanjiSeed("\u984d", "amount"),
                KanjiSeed("\u9650", "limit"),
                KanjiSeed("\u671f", "period"),
                KanjiSeed("\u9593", "between"),
                KanjiSeed("\u65b0", "new"),
                KanjiSeed("\u5546", "commerce"),
                KanjiSeed("\u54c1", "goods"),
                KanjiSeed("\u8cde", "prize/expiry"),
                KanjiSeed("\u51b7", "cold"),
                KanjiSeed("\u70ed", "hot"),
                KanjiSeed("\u5869", "salt"),
                KanjiSeed("\u5375", "egg"),
                KanjiSeed("\u4e73", "milk"),
                KanjiSeed("\u7a0e", "tax"),
                KanjiSeed("\u5186", "yen"),
                KanjiSeed("\u8336", "tea"),
                KanjiSeed("\u5f01", "lunch box"),
                KanjiSeed("\u5f53", "this/hit")
            ),
            vocabPool = listOf(
                VocabSeed("\u7121\u7cd6", "mutou", "sugar-free", listOf("sugar-free", "sweet", "spicy", "sour")),
                VocabSeed("\u65b0\u5546\u54c1", "shinshouhin", "new product", listOf("new product", "sold out", "half price", "limited")),
                VocabSeed("\u6e29\u3081\u307e\u3059\u304b", "atatamemasu ka", "shall I heat it?", listOf("shall I heat it?", "need a bag?", "is that all?", "cash or card?")),
                VocabSeed("\u888b\u3044\u308a\u307e\u3059\u304b", "fukuro irimasu ka", "need a bag?", listOf("need a bag?", "shall I heat it?", "need chopsticks?", "is that all?")),
                VocabSeed("\u534a\u984d", "hangaku", "half price", listOf("half price", "full price", "new product", "sold out")),
                VocabSeed("\u671f\u9593\u9650\u5b9a", "kikan gentei", "limited time", listOf("limited time", "always available", "new product", "sold out")),
                VocabSeed("\u5b63\u7bc0\u9650\u5b9a", "kisetsu gentei", "seasonal limited", listOf("seasonal limited", "year-round", "half price", "new release")),
                VocabSeed("\u30a2\u30ec\u30eb\u30ae\u30fc\u8868\u793a", "arerugii hyouji", "allergy info", listOf("allergy info", "nutrition facts", "ingredients", "expiry date")),
                VocabSeed("\u8cde\u5473\u671f\u9650", "shoumi kigen", "best before date", listOf("best before date", "allergy info", "price tag", "barcode")),
                VocabSeed("\u30ec\u30b8", "reji", "register", listOf("register", "exit", "shelf", "fridge")),
                VocabSeed("\u304a\u4f1a\u8a08", "okaikei", "checkout/bill", listOf("checkout/bill", "receipt", "change", "bag")),
                VocabSeed("\u539f\u6750\u6599", "genzairyou", "ingredients (label)", listOf("ingredients (label)", "allergy info", "calories", "price")),
                VocabSeed("\u7518\u53e3", "amakuchi", "mild/sweet", listOf("mild/sweet", "spicy", "sour", "bitter")),
                VocabSeed("\u8f9b\u53e3", "karakuchi", "spicy/hot", listOf("spicy/hot", "mild", "sweet", "salty")),
                VocabSeed("\u304a\u7b38", "ohashi", "chopsticks", listOf("chopsticks", "spoon", "fork", "bag")),
                VocabSeed("\u51b7\u305f\u3044", "tsumetai", "cold (to touch)", listOf("cold (to touch)", "hot", "warm", "cool")),
                VocabSeed("\u6e29\u304b\u3044", "atatakai", "warm", listOf("warm", "cold", "hot", "cool")),
                VocabSeed("\u5869\u5206", "enbun", "salt content", listOf("salt content", "sugar content", "calories", "fat")),
                VocabSeed("\u5375", "tamago", "egg", listOf("egg", "milk", "wheat", "soy")),
                VocabSeed("\u7a0e\u8fbc", "zeikomi", "tax included", listOf("tax included", "before tax", "discount", "free")),
                VocabSeed("\u7a0e\u629c", "zeinuki", "before tax", listOf("before tax", "tax included", "total", "change")),
                VocabSeed("\u304a\u5f01\u5f53", "obentou", "bento/boxed lunch", listOf("bento/boxed lunch", "rice ball", "sandwich", "salad")),
                VocabSeed("\u304a\u306b\u304e\u308a", "onigiri", "rice ball", listOf("rice ball", "bento", "sandwich", "bread")),
                VocabSeed("\u304a\u8336", "ocha", "tea", listOf("tea", "coffee", "water", "juice")),
                VocabSeed("\u30ab\u30ed\u30ea\u30fc", "karorii", "calories", listOf("calories", "salt", "sugar", "protein")),
                VocabSeed("\u304a\u91e3\u308a", "otsuri", "change (money)", listOf("change (money)", "receipt", "total", "discount")),
                VocabSeed("\u30dd\u30a4\u30f3\u30c8\u30ab\u30fc\u30c9", "pointo kaado", "point card", listOf("point card", "credit card", "cash", "receipt")),
                VocabSeed("\u96fb\u5b50\u30ec\u30f3\u30b8", "denshi renji", "microwave", listOf("microwave", "oven", "fridge", "stove"))
            )
        )
    }

    private fun buildSignsTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "signs_core",
            title = "Signs You See Everywhere",
            description = "Push or pull? Open or closed? Stop tugging on locked doors.",
            accentColor = "#6B8E5A",
            displayOrder = 12,
            levelTitles = listOf(
                "Push or Pull",
                "Open or Closed",
                "Don't Do That",
                "Useful Signs",
                "Reading Real Signs",
                "Warnings & Safety",
                "Public Facilities",
                "Sign Fluency"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u62bc", "push"),
                KanjiSeed("\u5f15", "pull"),
                KanjiSeed("\u958b", "open"),
                KanjiSeed("\u55b6", "operate"),
                KanjiSeed("\u696d", "business"),
                KanjiSeed("\u4f11", "rest/holiday"),
                KanjiSeed("\u7981", "prohibit"),
                KanjiSeed("\u6b62", "stop"),
                KanjiSeed("\u6ce8", "caution"),
                KanjiSeed("\u5de5", "construction"),
                KanjiSeed("\u4e8b", "thing/matter"),
                KanjiSeed("\u975e", "not/emergency"),
                KanjiSeed("\u5e38", "usual"),
                KanjiSeed("\u7acb", "stand"),
                KanjiSeed("\u53f3", "right"),
                KanjiSeed("\u5de6", "left"),
                KanjiSeed("\u4e0a", "up"),
                KanjiSeed("\u4e0b", "down"),
                KanjiSeed("\u968e", "floor/story"),
                KanjiSeed("\u7537", "man"),
                KanjiSeed("\u5973", "woman"),
                KanjiSeed("\u7121", "none/without"),
                KanjiSeed("\u6709", "have/exist"),
                KanjiSeed("\u6545", "reason/breakdown"),
                KanjiSeed("\u9589", "close/shut"),
                KanjiSeed("\u4e2d", "middle/inside"),
                KanjiSeed("\u6e80", "full"),
                KanjiSeed("\u6771", "east"),
                KanjiSeed("\u897f", "west"),
                KanjiSeed("\u5317", "north"),
                KanjiSeed("\u5357", "south")
            ),
            vocabPool = listOf(
                VocabSeed("\u62bc\u3059", "osu", "push", listOf("push", "pull", "open", "close")),
                VocabSeed("\u5f15\u304f", "hiku", "pull", listOf("pull", "push", "lift", "slide")),
                VocabSeed("\u5165\u53e3", "iriguchi", "entrance", listOf("entrance", "exit", "stairs", "elevator")),
                VocabSeed("\u51fa\u53e3", "deguchi", "exit", listOf("exit", "entrance", "gate", "door")),
                VocabSeed("\u55b6\u696d\u4e2d", "eigyouchuu", "open for business", listOf("open for business", "closed", "under construction", "out of stock")),
                VocabSeed("\u5b9a\u4f11\u65e5", "teikyuubi", "regular holiday", listOf("regular holiday", "business hours", "open today", "24 hours")),
                VocabSeed("\u6e96\u5099\u4e2d", "junbichuu", "preparing/not ready", listOf("preparing/not ready", "open now", "closed forever", "on sale")),
                VocabSeed("\u7981\u6b62", "kinshi", "prohibited", listOf("prohibited", "allowed", "recommended", "required")),
                VocabSeed("\u6ce8\u610f", "chuui", "caution", listOf("caution", "safe", "exit", "entrance")),
                VocabSeed("\u5de5\u4e8b\u4e2d", "koujichuu", "under construction", listOf("under construction", "open for business", "closed today", "on sale")),
                VocabSeed("\u975e\u5e38\u53e3", "hijouguchi", "emergency exit", listOf("emergency exit", "main entrance", "elevator", "stairs")),
                VocabSeed("\u304a\u624b\u6d17\u3044", "otearai", "restroom", listOf("restroom", "kitchen", "office", "exit")),
                VocabSeed("\u53d7\u4ed8", "uketsuke", "reception", listOf("reception", "exit", "elevator", "stairs")),
                VocabSeed("\u7acb\u5165\u7981\u6b62", "tachiiri kinshi", "no entry", listOf("no entry", "free entry", "staff only", "members only")),
                VocabSeed("\u95a2\u4fc2\u8005\u4ee5\u5916", "kankeisha igai", "unauthorized persons", listOf("unauthorized persons", "all visitors", "customers only", "members welcome")),
                VocabSeed("\u53f3", "migi", "right", listOf("right", "left", "up", "down")),
                VocabSeed("\u5de6", "hidari", "left", listOf("left", "right", "up", "straight")),
                VocabSeed("\u5730\u4e0b", "chika", "basement", listOf("basement", "rooftop", "first floor", "elevator")),
                VocabSeed("\u7537\u6027", "dansei", "men's", listOf("men's", "women's", "children's", "unisex")),
                VocabSeed("\u5973\u6027", "josei", "women's", listOf("women's", "men's", "children's", "unisex")),
                VocabSeed("\u6545\u969c\u4e2d", "koshoochuu", "out of order", listOf("out of order", "in use", "available", "under repair")),
                VocabSeed("\u7121\u6599", "muryou", "free of charge", listOf("free of charge", "paid", "half price", "sold out")),
                VocabSeed("\u6709\u6599", "yuuryou", "paid", listOf("paid", "free", "half price", "discount")),
                VocabSeed("\u99d0\u8eca\u5834", "chuushajou", "parking lot", listOf("parking lot", "bus stop", "train station", "taxi stand")),
                VocabSeed("\u559c\u7159\u6240", "kitsuenjo", "smoking area", listOf("smoking area", "no smoking", "rest area", "exit")),
                VocabSeed("\u6848\u5185", "annai", "information/guide", listOf("information/guide", "exit", "entrance", "caution")),
                VocabSeed("\u30a8\u30ec\u30d9\u30fc\u30bf\u30fc", "erebeetaa", "elevator", listOf("elevator", "escalator", "stairs", "exit")),
                VocabSeed("\u5e8a\u304c\u6ed1\u308a\u307e\u3059", "yuka ga suberimasu", "slippery floor", listOf("slippery floor", "wet paint", "out of order", "no entry")),
                VocabSeed("\u9589\u5e97", "heiten", "closed (shop)", listOf("closed (shop)", "open", "preparing", "sold out")),
                VocabSeed("\u4f7f\u7528\u4e2d", "shiyouchuu", "in use/occupied", listOf("in use/occupied", "available", "out of order", "closed")),
                VocabSeed("\u7a7a\u5e2d", "kuuseki", "vacant seat", listOf("vacant seat", "full", "reserved", "no entry")),
                VocabSeed("\u6e80\u5e2d", "manseki", "full (no seats)", listOf("full (no seats)", "available", "reserved", "half empty")),
                VocabSeed("\u6771\u53e3", "higashiguchi", "east exit", listOf("east exit", "west exit", "north exit", "south exit")),
                VocabSeed("\u897f\u53e3", "nishiguchi", "west exit", listOf("west exit", "east exit", "south exit", "north exit")),
                VocabSeed("\u5317\u53e3", "kitaguchi", "north exit", listOf("north exit", "south exit", "east exit", "west exit")),
                VocabSeed("\u5357\u53e3", "minamiguchi", "south exit", listOf("south exit", "north exit", "west exit", "east exit")),
                VocabSeed("\u9589\u307e\u308b", "shimaru", "to close (door)", listOf("to close (door)", "to open", "to push", "to pull"))
            )
        )
    }

    private fun buildAdultingTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "adulting_core",
            title = "Mail & Adulting",
            description = "Delivery slips, forms, receipts, and fees. Adulting in Japanese.",
            accentColor = "#7A6B5D",
            displayOrder = 13,
            levelTitles = listOf(
                "Delivery Basics",
                "Forms & Addresses",
                "Receipts & Fees",
                "Deadlines & Submissions",
                "Office & Reception",
                "Banking Basics",
                "Utilities & Bills",
                "Paperwork Pro"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u5c4a", "deliver"),
                KanjiSeed("\u914d", "distribute"),
                KanjiSeed("\u5728", "exist/be at"),
                KanjiSeed("\u4f4f", "live"),
                KanjiSeed("\u6240", "place"),
                KanjiSeed("\u540d", "name"),
                KanjiSeed("\u9818", "receipt/territory"),
                KanjiSeed("\u53ce", "collect"),
                KanjiSeed("\u624b", "hand"),
                KanjiSeed("\u63d0", "present/submit"),
                KanjiSeed("\u7de0", "close/tighten"),
                KanjiSeed("\u53d7", "receive"),
                KanjiSeed("\u4ed8", "attach"),
                KanjiSeed("\u7a93", "window"),
                KanjiSeed("\u9001", "send"),
                KanjiSeed("\u8fd4", "return"),
                KanjiSeed("\u6255", "pay"),
                KanjiSeed("\u7533", "apply"),
                KanjiSeed("\u53f7", "number"),
                KanjiSeed("\u5951", "contract"),
                KanjiSeed("\u4fdd", "protect/insure"),
                KanjiSeed("\u967a", "danger/risk"),
                KanjiSeed("\u5bb6", "house"),
                KanjiSeed("\u8cac", "duty/fee")
            ),
            vocabPool = listOf(
                VocabSeed("\u4e0d\u5728\u7968", "fuzaihyou", "missed delivery slip", listOf("missed delivery slip", "receipt", "invoice", "ticket")),
                VocabSeed("\u518d\u914d\u9054", "saihaitatsu", "redelivery", listOf("redelivery", "return", "cancellation", "refund")),
                VocabSeed("\u4f4f\u6240", "juusho", "address", listOf("address", "name", "phone number", "email")),
                VocabSeed("\u6c0f\u540d", "shimei", "full name", listOf("full name", "address", "phone", "signature")),
                VocabSeed("\u9818\u53ce\u66f8", "ryoushuusho", "receipt", listOf("receipt", "invoice", "ticket", "contract")),
                VocabSeed("\u624b\u6570\u6599", "tesuuryou", "handling fee", listOf("handling fee", "discount", "tax", "tip")),
                VocabSeed("\u63d0\u51fa", "teishutsu", "submission", listOf("submission", "receipt", "cancellation", "delivery")),
                VocabSeed("\u7de0\u5207", "shimekiri", "deadline", listOf("deadline", "start date", "holiday", "payday")),
                VocabSeed("\u53d7\u4ed8", "uketsuke", "reception desk", listOf("reception desk", "exit", "waiting room", "restroom")),
                VocabSeed("\u7a93\u53e3", "madoguchi", "service counter", listOf("service counter", "entrance", "exit", "ATM")),
                VocabSeed("\u8a18\u5165", "kinyuu", "fill in (a form)", listOf("fill in (a form)", "submit", "cancel", "sign")),
                VocabSeed("\u5370\u9451", "inkan", "personal seal", listOf("personal seal", "signature", "stamp", "ID card")),
                VocabSeed("\u66f8\u985e", "shorui", "documents", listOf("documents", "receipt", "money", "ticket")),
                VocabSeed("\u632f\u8fbc", "furikomi", "bank transfer", listOf("bank transfer", "cash payment", "credit card", "refund")),
                VocabSeed("\u8acb\u6c42\u66f8", "seikyuusho", "invoice", listOf("invoice", "receipt", "contract", "delivery slip")),
                VocabSeed("\u53e3\u5ea7", "kouza", "bank account", listOf("bank account", "credit card", "cash", "savings")),
                VocabSeed("\u6697\u8a3c\u756a\u53f7", "anshou bangou", "PIN number", listOf("PIN number", "phone number", "address", "account number")),
                VocabSeed("\u7533\u8fbc", "moushikomi", "application", listOf("application", "cancellation", "receipt", "invoice")),
                VocabSeed("\u5951\u7d04", "keiyaku", "contract", listOf("contract", "receipt", "refund", "invoice")),
                VocabSeed("\u66f4\u65b0", "koushin", "renewal", listOf("renewal", "cancellation", "new", "expired")),
                VocabSeed("\u4fdd\u8a3c", "hoshou", "guarantee/warranty", listOf("guarantee/warranty", "refund", "exchange", "repair")),
                VocabSeed("\u8eab\u5206\u8a3c\u660e\u66f8", "mibun shoumeisho", "ID card", listOf("ID card", "passport", "license", "receipt")),
                VocabSeed("\u5f15\u843d", "hikiotoshi", "auto-deduction", listOf("auto-deduction", "cash payment", "transfer", "refund")),
                VocabSeed("\u5149\u71b1\u8cbb", "kounetsuhi", "utility costs", listOf("utility costs", "rent", "insurance", "tax")),
                VocabSeed("\u5bb6\u8cc3", "yachin", "rent", listOf("rent", "utilities", "deposit", "insurance")),
                VocabSeed("\u6577\u91d1", "shikikin", "security deposit", listOf("security deposit", "rent", "key money", "brokerage fee")),
                VocabSeed("\u4fdd\u967a", "hoken", "insurance", listOf("insurance", "warranty", "tax", "fee")),
                VocabSeed("\u8fd4\u9001", "hensou", "return shipping", listOf("return shipping", "delivery", "pickup", "refund"))
            )
        )
    }

    private fun buildSimpleThemeTrack(
        trackId: String,
        title: String,
        description: String,
        accentColor: String,
        displayOrder: Int,
        levelTitles: List<String>,
        kanjiPool: List<KanjiSeed>,
        vocabPool: List<VocabSeed>
    ): TrackSeed {
        val packs = levelTitles.mapIndexed { index, levelTitle ->
            val level = index + 1
            val difficulty = (index + 2).coerceIn(2, 10)
            val kanji = cyclicSlice(kanjiPool, start = index * 3, count = 5)
            val vocab = cyclicSlice(vocabPool, start = index * 3, count = 4)
            // Rotate grammar patterns across packs for variety
            val gv = vocab[index % vocab.size]               // grammar vocab
            val sv = vocab[(index + 2) % vocab.size]         // sentence vocab
            val gvRuby = furiganaTokenForVocab(gv)
            val svRuby = furiganaTokenForVocab(sv)
            data class GFrame(val prompt: String, val furigana: String, val answer: String,
                              val choices: List<String>)
            val grammarFrames = listOf(
                GFrame("${gv.reading} wa doko ( ) arimasu ka.",
                    "${gvRuby}はどこ（　）ありますか。", "ni",
                    listOf("ni", "de", "to", "e", "ka")),
                GFrame("${gv.reading} ga ( ) desu.",
                    "${gvRuby}が（　）です。", "hoshii",
                    listOf("hoshii", "kirai", "jouzu", "suki", "dame")),
                GFrame("${gv.reading} o misete ( ).",
                    "${gvRuby}を見{み}せて（　）。", "kudasai",
                    listOf("kudasai", "shimasu", "masu", "mashita", "desu")),
                GFrame("${gv.reading} wa ikura ( ) ka.",
                    "${gvRuby}はいくら（　）か。", "desu",
                    listOf("desu", "masu", "da", "deshita", "mashita")),
                GFrame("${gv.reading} to ${sv.reading} ( ) kudasai.",
                    "${gvRuby}と${svRuby}（　）ください。", "o",
                    listOf("o", "ga", "wa", "ni", "de")),
                GFrame("${gv.reading} wa ${sv.reading} ( ) ii desu.",
                    "${gvRuby}は${svRuby}（　）いいです。", "yori",
                    listOf("yori", "to", "de", "ni", "ka")),
                GFrame("${gv.reading} o ( ) mo ii desu ka.",
                    "${gvRuby}を（　）もいいですか。", "tsukatte",
                    listOf("tsukatte", "tabete", "nonde", "mite", "kaite")),
                GFrame("${gv.reading} wa ( ) desu ka.",
                    "${gvRuby}は（　）ですか。", "dou",
                    listOf("dou", "nani", "dare", "itsu", "doko"))
            )
            val gf = grammarFrames[index % grammarFrames.size]
            val clozeAnswers = listOf("kudasai", "onegai shimasu", "shite", "mite", "itte",
                "tabete", "kaite", "tsukatte")
            val clozeAns = clozeAnswers[index % clozeAnswers.size]
            val v0Ruby = furiganaTokenForVocab(vocab[0])
            val v1Ruby = furiganaTokenForVocab(vocab[1])
            val v2Ruby = furiganaTokenForVocab(vocab[2])
            val v3Ruby = furiganaTokenForVocab(vocab[3])
            val sentenceBuildJapanese = "${vocab[0].term}と${vocab[1].term}をお願いします"
            PackSeed(
                level = level,
                title = levelTitle,
                cards = buildPackCards(
                    trackId = trackId,
                    level = level,
                    difficulty = difficulty,
                    kanji = kanji,
                    vocab = vocab,
                    grammarChoice = GrammarSeed(
                        prompt = gf.prompt,
                        promptFurigana = gf.furigana,
                        answer = gf.answer,
                        choices = gf.choices,
                        meaning = "$title grammar pattern"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "${vocab[2].reading} o ( ____ ).",
                        promptFurigana = "${v2Ruby}を（　）。",
                        answer = clozeAns,
                        accepted = listOf(clozeAns),
                        meaning = "$title request pattern"
                    ),
                    sentence = SentenceSeed(
                        prompt = "${vocab[2].reading} to ${vocab[3].reading} ga hitsuyou desu.",
                        promptFurigana = "${v2Ruby}と${v3Ruby}が必要{ひつよう}です。",
                        answer = "$levelTitle uses ${vocab[2].meaning} and ${vocab[3].meaning}.",
                        choices = listOf(
                            "$levelTitle uses ${vocab[2].meaning} and ${vocab[3].meaning}.",
                            "$levelTitle avoids both terms completely.",
                            "$levelTitle focuses only on grammar drills.",
                            "$levelTitle is unrelated to this theme."
                        ),
                        meaning = "$title comprehension check"
                    ),
                    sentenceBuild = BuildSeed(
                        prompt = "Build a $levelTitle phrase with theme words.",
                        answer = sentenceBuildJapanese,
                        accepted = listOf(
                            sentenceBuildJapanese,
                            "${vocab[0].reading} to ${vocab[1].reading} o onegai shimasu",
                            "${vocab[0].reading} o onegai shimasu",
                            "${vocab[0].term}をお願いします"
                        ),
                        meaning = "$title production check"
                    )
                )
            )
        }
        return buildTrack(
            trackId = trackId,
            title = title,
            description = description,
            accentColor = accentColor,
            displayOrder = displayOrder,
            minTotalScore = 78,
            minHandwritingScore = 66,
            packs = packs
        )
    }

    private fun <T> cyclicSlice(items: List<T>, start: Int, count: Int): List<T> {
        if (items.isEmpty()) return emptyList()
        return (0 until count).map { offset ->
            items[(start + offset) % items.size]
        }
    }

    private fun buildPackCards(
        trackId: String,
        level: Int,
        difficulty: Int,
        kanji: List<KanjiSeed>,
        vocab: List<VocabSeed>,
        grammarChoice: GrammarSeed,
        grammarCloze: ClozeSeed,
        sentence: SentenceSeed,
        sentenceBuild: BuildSeed
    ): List<CardEntity> {
        val cards = mutableListOf<CardEntity>()
        // VOCAB_READING — always included (recognition)
        cards += vocab.mapIndexed { index, seed ->
            val cardId = "${trackId}_v_${level}_${index + 1}"
            val answer = seed.meaning.trim()
            val accepted = listOf(answer, answer.lowercase(Locale.US)).distinct()
            val normalizedChoices = (seed.choices + answer).distinct()
            CardEntity(
                cardId = cardId,
                type = CardType.VOCAB_READING,
                prompt = seed.term,
                canonicalAnswer = answer,
                acceptedAnswersRaw = accepted.joinToString("|"),
                reading = seed.reading,
                meaning = answer,
                promptFurigana = null,
                choicesRaw = normalizedChoices.joinToString("|"),
                difficulty = difficulty,
                templateId = "tmpl_$cardId"
            )
        }
        // KANJI_MEANING — all levels (meaning recognition, easiest entry point)
        if (kanji.isNotEmpty()) {
            val vocabMeanings = vocab.map { it.meaning.trim() }.toSet()
            val meaningPool = (kanji.map { it.meaning } + vocab.map { it.meaning.trim() }).distinct()
            cards += kanji.mapIndexedNotNull { index, seed ->
                if (seed.meaning in vocabMeanings) return@mapIndexedNotNull null
                val cardId = "${trackId}_m_${level}_${index + 1}"
                val answer = seed.meaning
                val distractors = meaningPool
                    .filter { it != answer }
                    .shuffled(Random(cardId.hashCode()))
                    .take(3)
                val choices = (distractors + answer).shuffled(Random(cardId.hashCode() xor 0x5BD1E995.toInt()))
                CardEntity(
                    cardId = cardId,
                    type = CardType.KANJI_MEANING,
                    prompt = seed.symbol,
                    canonicalAnswer = answer,
                    acceptedAnswersRaw = answer,
                    reading = null,
                    meaning = answer,
                    promptFurigana = null,
                    choicesRaw = choices.joinToString("|"),
                    difficulty = difficulty,
                    templateId = "tmpl_$cardId"
                )
            }
        }
        // KANJI_WRITE — difficulty 2+ (production)
        if (difficulty >= 2) {
            cards += kanji.mapIndexed { index, seed ->
                val cardId = "${trackId}_k_${level}_${index + 1}"
                CardEntity(
                    cardId = cardId,
                    type = CardType.KANJI_WRITE,
                    prompt = "Write the kanji: ${seed.meaning}",
                    canonicalAnswer = seed.symbol,
                    acceptedAnswersRaw = seed.symbol,
                    reading = null,
                    meaning = seed.meaning,
                    promptFurigana = null,
                    choicesRaw = null,
                    difficulty = difficulty,
                    templateId = "tmpl_$cardId"
                )
            }
        }
        // KANJI_READING — difficulty 2+ (reading recognition)
        if (difficulty >= 2) {
            val kanjiReadingAnswer = vocab.firstOrNull()?.reading?.trim().orEmpty()
            if (kanjiReadingAnswer.isNotBlank() && kanji.isNotEmpty()) {
                val readingPool = (
                    vocab.map { it.reading.trim() } +
                        listOf("desu", "masu", "kudasai", "ka")
                    )
                    .filter { it.isNotBlank() }
                    .distinct()
                val readingChoices = (readingPool.shuffled(Random(trackId.hashCode() + level)).take(3) + kanjiReadingAnswer)
                    .distinct()
                    .shuffled(Random((trackId.hashCode() xor level) + 17))
                cards += CardEntity(
                    cardId = "${trackId}_r_${level}_1",
                    type = CardType.KANJI_READING,
                    prompt = kanji.first().symbol,
                    canonicalAnswer = kanjiReadingAnswer,
                    acceptedAnswersRaw = listOf(kanjiReadingAnswer, kanjiReadingAnswer.lowercase(Locale.US)).distinct()
                        .joinToString("|"),
                    reading = kanjiReadingAnswer,
                    meaning = "kanji reading",
                    promptFurigana = null,
                    choicesRaw = readingChoices.joinToString("|"),
                    difficulty = difficulty,
                    templateId = "tmpl_${trackId}_r_${level}_1"
                )
            }
        }
        // GRAMMAR_CHOICE — difficulty 2+ (grammar recognition)
        if (difficulty >= 2) {
            cards += CardEntity(
                cardId = "${trackId}_g_${level}_1",
                type = CardType.GRAMMAR_CHOICE,
                prompt = grammarChoice.prompt,
                canonicalAnswer = grammarChoice.answer,
                acceptedAnswersRaw = grammarChoice.answer,
                reading = grammarChoice.answer,
                meaning = grammarChoice.meaning,
                promptFurigana = grammarChoice.promptFurigana,
                choicesRaw = (grammarChoice.choices + grammarChoice.answer).distinct().joinToString("|"),
                difficulty = difficulty,
                templateId = "tmpl_${trackId}_g_${level}_1"
            )
        }
        // GRAMMAR_CLOZE_WRITE — difficulty 4+ (grammar production)
        if (difficulty >= 4) {
            cards += CardEntity(
                cardId = "${trackId}_c_${level}_1",
                type = CardType.GRAMMAR_CLOZE_WRITE,
                prompt = grammarCloze.prompt,
                canonicalAnswer = grammarCloze.answer,
                acceptedAnswersRaw = (grammarCloze.accepted + grammarCloze.answer).distinct().joinToString("|"),
                reading = null,
                meaning = grammarCloze.meaning,
                promptFurigana = grammarCloze.promptFurigana,
                choicesRaw = null,
                difficulty = difficulty,
                templateId = "tmpl_${trackId}_c_${level}_1"
            )
        }
        // SENTENCE_COMPREHENSION — difficulty 6+ (sentence-level understanding)
        if (difficulty >= 6) {
            cards += CardEntity(
                cardId = "${trackId}_s_${level}_1",
                type = CardType.SENTENCE_COMPREHENSION,
                prompt = sentence.prompt,
                canonicalAnswer = sentence.answer,
                acceptedAnswersRaw = sentence.answer,
                reading = null,
                meaning = sentence.meaning,
                promptFurigana = sentence.promptFurigana,
                choicesRaw = (sentence.choices + sentence.answer).distinct().joinToString("|"),
                difficulty = difficulty,
                templateId = "tmpl_${trackId}_s_${level}_1"
            )
        }
        // SENTENCE_BUILD — difficulty 6+ (sentence production)
        if (difficulty >= 6) {
            cards += CardEntity(
                cardId = "${trackId}_b_${level}_1",
                type = CardType.SENTENCE_BUILD,
                prompt = sentenceBuild.prompt,
                canonicalAnswer = sentenceBuild.answer,
                acceptedAnswersRaw = (sentenceBuild.accepted + sentenceBuild.answer).distinct().joinToString("|"),
                reading = null,
                meaning = sentenceBuild.meaning,
                promptFurigana = null,
                choicesRaw = null,
                difficulty = difficulty,
                templateId = "tmpl_${trackId}_b_${level}_1"
            )
        }
        return cards
    }

    private fun buildTrack(
        trackId: String,
        title: String,
        description: String,
        accentColor: String,
        displayOrder: Int,
        minTotalScore: Int,
        minHandwritingScore: Int,
        packs: List<PackSeed>
    ): TrackSeed {
        val track = TrackEntity(
            trackId = trackId,
            title = title,
            description = description,
            accentColor = accentColor,
            displayOrder = displayOrder
        )

        val packEntities = packs.map { pack ->
            PackEntity(
                packId = packId(trackId, pack.level),
                trackId = trackId,
                level = pack.level,
                title = pack.title,
                minTotalScore = minTotalScore,
                minHandwritingScore = minHandwritingScore,
                cardCount = pack.cards.size,
                displayOrder = pack.level
            )
        }
        val cards = packs.flatMap { it.cards }
        val packCards = packs.flatMap { pack ->
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

    private fun strokeCountFor(target: String): Int {
        val symbol = target.firstOrNull()?.toString().orEmpty()
        return strokeCountMap[symbol] ?: ((abs(symbol.hashCode()) % 8) + 5)
    }

    private fun furiganaTokenForVocab(seed: VocabSeed): String {
        if (!containsKanji(seed.term)) return seed.term
        val reading = romanizedToHiragana(seed.reading).ifBlank { seed.reading }
        return "${seed.term}{$reading}"
    }

    private fun containsKanji(text: String): Boolean {
        return text.any { character ->
            Character.UnicodeScript.of(character.code) == Character.UnicodeScript.HAN
        }
    }

    private fun romanizedToHiragana(romaji: String): String {
        val normalized = romaji
            .lowercase(Locale.US)
            .replace("-", "")
            .replace(" ", "")
        if (normalized.isBlank()) return ""
        val result = StringBuilder()
        var index = 0
        while (index < normalized.length) {
            val current = normalized[index]
            val next = normalized.getOrNull(index + 1)

            if (
                next != null &&
                current == next &&
                current in "bcdfghjklmpqrstvwxyz" &&
                current != 'n'
            ) {
                result.append('っ')
                index += 1
                continue
            }

            if (current == 'n') {
                if (next == null) {
                    result.append('ん')
                    index += 1
                    continue
                }
                if (next == '\'') {
                    result.append('ん')
                    index += 2
                    continue
                }
                if (next == 'n') {
                    result.append('ん')
                    index += 1
                    continue
                }
                if (next !in "aeiouy") {
                    result.append('ん')
                    index += 1
                    continue
                }
            }

            val kanaEntry = ROMAJI_TO_HIRAGANA.firstOrNull { (latin, _) ->
                normalized.startsWith(latin, startIndex = index)
            }
            if (kanaEntry != null) {
                result.append(kanaEntry.second)
                index += kanaEntry.first.length
            } else {
                result.append(current)
                index += 1
            }
        }
        return result.toString()
    }

    private fun buildTemplateStrokePaths(target: String, expectedStrokeCount: Int): String {
        val seed = target.firstOrNull()?.code ?: 0
        return (0 until expectedStrokeCount).joinToString("|") { strokeIndex ->
            val startX = ((seed + (strokeIndex * 17)) % 54 + 20) / 100f
            val startY = ((seed + (strokeIndex * 11)) % 54 + 20) / 100f
            val midX = (startX + 0.12f + ((strokeIndex % 3) * 0.02f)).coerceIn(0.12f, 0.92f)
            val midY = (startY + 0.09f + ((strokeIndex % 4) * 0.015f)).coerceIn(0.12f, 0.92f)
            val endX = (midX + 0.08f).coerceIn(0.12f, 0.92f)
            val endY = (midY + 0.08f).coerceIn(0.12f, 0.92f)
            encodeStroke(startX, startY, midX, midY, endX, endY)
        }
    }

    private fun encodeStroke(
        startX: Float,
        startY: Float,
        midX: Float,
        midY: Float,
        endX: Float,
        endY: Float
    ): String {
        return "${point(startX, startY)};${point(midX, midY)};${point(endX, endY)}"
    }

    private fun point(x: Float, y: Float): String {
        return "${format(x)},${format(y)}"
    }

    private fun format(value: Float): String {
        return String.format(Locale.US, "%.3f", value)
    }

    private val ROMAJI_TO_HIRAGANA = listOf(
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
        "shi" to "し", "chi" to "ち", "tsu" to "つ", "fu" to "ふ",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "sa" to "さ", "su" to "す", "se" to "せ", "so" to "そ",
        "ta" to "た", "te" to "て", "to" to "と",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "he" to "へ", "ho" to "ほ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "za" to "ざ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "da" to "だ", "de" to "で", "do" to "ど",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お"
    )

    private val strokeCountMap = mapOf(
        "\u7d4c" to 11,
        "\u9a13" to 18,
        "\u5909" to 9,
        "\u9023" to 10,
        "\u7d50" to 12,
        "\u679c" to 8,
        "\u8aac" to 14,
        "\u660e" to 8,
        "\u610f" to 13,
        "\u6700" to 12,
        "\u8fd1" to 7,
        "\u7531" to 5,
        "\u5fc5" to 5,
        "\u8981" to 9,
        "\u984c" to 18,
        "\u8cc7" to 13,
        "\u6e90" to 13,
        "\u7387" to 11,
        "\u63a1" to 11,
        "\u7528" to 5,
        "\u7248" to 8,
        "\u8b70" to 20,
        "\u8ad6" to 15,
        "\u67fb" to 9,
        "\u76e3" to 14,
        "\u7dad" to 14,
        "\u5fdc" to 7,
        "\u9700" to 14,
        "\u7d66" to 12,
        "\u6a21" to 14,
        "\u99c5" to 14,
        "\u65c5" to 10,
        "\u7d04" to 9,
        // Food & Ordering
        "\u725b" to 4,
        "\u8c5a" to 11,
        "\u9d8f" to 14,
        "\u76db" to 11,
        "\u6301" to 9,
        "\u58f2" to 7,
        "\u5207" to 4,
        "\u5e2d" to 10,
        "\u7159" to 13,
        "\u5b9a" to 8,
        "\u9aa8" to 10,
        // Transport
        "\u51fa" to 5,
        "\u5165" to 2,
        "\u53e3" to 3,
        "\u6025" to 9,
        "\u5404" to 6,
        "\u4e57" to 9,
        "\u63db" to 12,
        "\u904b" to 12,
        "\u8ee2" to 11,
        "\u7d42" to 11,
        "\u512a" to 17,
        "\u5148" to 6,
        // Konbini
        "\u7cd6" to 16,
        "\u7518" to 5,
        "\u8f9b" to 7,
        "\u6e29" to 12,
        "\u888b" to 11,
        "\u534a" to 5,
        "\u984d" to 18,
        "\u9650" to 9,
        "\u671f" to 12,
        "\u9593" to 12,
        "\u65b0" to 13,
        "\u5546" to 11,
        "\u54c1" to 9,
        "\u8cde" to 15,
        // Signs
        "\u62bc" to 8,
        "\u5f15" to 4,
        "\u958b" to 12,
        "\u55b6" to 12,
        "\u696d" to 13,
        "\u4f11" to 6,
        "\u7981" to 13,
        "\u6b62" to 4,
        "\u6ce8" to 8,
        "\u5de5" to 3,
        "\u4e8b" to 8,
        "\u975e" to 8,
        "\u5e38" to 11,
        "\u7acb" to 5,
        // Adulting
        "\u5c4a" to 8,
        "\u914d" to 10,
        "\u5728" to 6,
        "\u4f4f" to 7,
        "\u6240" to 8,
        "\u540d" to 6,
        "\u9818" to 14,
        "\u53ce" to 4,
        "\u624b" to 4,
        "\u63d0" to 12,
        "\u7de0" to 15,
        "\u53d7" to 8,
        "\u4ed8" to 5,
        "\u7a93" to 11,
        // Food (new)
        "\u9b5a" to 11,  // 魚 fish
        "\u91ce" to 11,  // 野 field
        "\u83dc" to 11,  // 菜 vegetable
        "\u9eba" to 16,  // 麺 noodle
        "\u4e3c" to 5,   // 丼 rice bowl
        "\u5473" to 8,   // 味 flavor
        "\u4e88" to 4,   // 予 advance
        "\u7c73" to 6,   // 米 rice
        "\u6c34" to 4,   // 水 water
        // Transport (new)
        "\u5317" to 5,   // 北 north
        "\u5357" to 9,   // 南 south
        "\u6771" to 8,   // 東 east
        "\u897f" to 6,   // 西 west
        "\u756a" to 12,  // 番 number
        "\u7dda" to 15,  // 線 line
        "\u767a" to 9,   // 発 depart
        "\u7740" to 12,  // 着 arrive
        "\u7247" to 4,   // 片 one-way
        "\u8fd4" to 7,   // 返 return
        // Konbini (new)
        "\u51b7" to 7,   // 冷 cold
        "\u71b1" to 15,  // 熱 hot
        "\u5869" to 13,  // 塩 salt
        "\u5375" to 7,   // 卵 egg
        "\u4e73" to 8,   // 乳 milk
        "\u7a0e" to 12,  // 税 tax
        "\u5186" to 4,   // 円 yen
        "\u8336" to 9,   // 茶 tea
        "\u5f01" to 5,   // 弁 lunch box
        "\u5f53" to 6,   // 当 this
        // Signs (new)
        "\u53f3" to 5,   // 右 right
        "\u5de6" to 5,   // 左 left
        "\u4e0a" to 3,   // 上 up
        "\u4e0b" to 3,   // 下 down
        "\u968e" to 12,  // 階 floor
        "\u7537" to 7,   // 男 man
        "\u5973" to 3,   // 女 woman
        "\u7121" to 12,  // 無 none
        "\u6709" to 6,   // 有 have
        "\u6545" to 9,   // 故 breakdown
        // Adulting (new)
        "\u9001" to 9,   // 送 send
        "\u6255" to 5,   // 払 pay
        "\u7533" to 5,   // 申 apply
        "\u53f7" to 5,   // 号 number
        "\u5951" to 9,   // 契 contract
        "\u4fdd" to 9,   // 保 insure
        "\u96aa" to 11,  // 険 risk
        "\u5bb6" to 10,  // 家 house
        "\u8cac" to 11,  // 責 fee
        // Shopping (new)
        "\u8272" to 6,   // 色 color
        "\u91cd" to 9,   // 重 heavy
        "\u8efd" to 12,  // 軽 light
        "\u53e4" to 5,   // 古 old
        "\u7a7a" to 8,   // 空 empty
        "\u5305" to 5,   // 包 wrap
        "\u9078" to 15,  // 選 choose
        "\u63a2" to 11,  // 探 search
        // Signs (new batch 2)
        "\u9589" to 11,  // 閉 close
        "\u4e2d" to 4,   // 中 middle
        "\u6e80" to 12,  // 満 full
        // Shopping (new batch 2)
        "\u5343" to 3,   // 千 thousand
        "\u767e" to 6,   // 百 hundred
        "\u4e07" to 3,   // 万 ten thousand
        "\u5927" to 3,   // 大 big
        "\u5c0f" to 3,   // 小 small
        "\u591a" to 6,   // 多 many
        "\u5c11" to 4,   // 少 few
        // Food (new batch 2)
        "\u8089" to 6,   // 肉 meat
        // Transport (new batch 2)
        "\u9580" to 8,   // 門 gate
        "\u884c" to 6,   // 行 go
        "\u6765" to 7,   // 来 come
        "\u96fb" to 13,  // 電 electric
        // Daily Life (new)
        "\u6708" to 4,   // 月 moon/month
        "\u706b" to 4,   // 火 fire
        "\u91d1" to 8,   // 金 gold/money
        "\u4eca" to 4,   // 今 now
        "\u524d" to 9,   // 前 before
        "\u898b" to 7,   // 見 see
        "\u50cd" to 13,  // 働 work
        "\u8cb7" to 12,  // 買 buy
        "\u4f1a" to 6,   // 会 meet
        "\u5b66" to 8    // 学 study
    )
}
