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
        val all = listOf(foundations, n4, n3, daily, dailyLife, food, transport, shopping)
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
                KanjiSeed("\u6b69", "walk")
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
                VocabSeed("\u6563\u6b69", "sanpo", "walk", listOf("walk", "meeting", "payment", "route"))
            )
        )
    }

    private fun buildFoodTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "food_core",
            title = "Food & Menus",
            description = "Theme-specific food progression from ingredients and ordering through menu and dialogue practice.",
            accentColor = "#A86B3D",
            displayOrder = 9,
            levelTitles = listOf(
                "Ingredients",
                "Ordering",
                "Menus",
                "Cooking Terms",
                "Food Dialogues",
                "Restaurant Requests",
                "Nutrition & Preferences",
                "Service Nuance"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u98df", "eat"),
                KanjiSeed("\u98f2", "drink"),
                KanjiSeed("\u6599", "cooking"),
                KanjiSeed("\u5473", "taste"),
                KanjiSeed("\u76bf", "plate"),
                KanjiSeed("\u7c73", "rice"),
                KanjiSeed("\u8089", "meat"),
                KanjiSeed("\u9b5a", "fish"),
                KanjiSeed("\u8336", "tea"),
                KanjiSeed("\u5e97", "shop"),
                KanjiSeed("\u91ce", "field"),
                KanjiSeed("\u679c", "fruit"),
                KanjiSeed("\u5f01", "valve/bento"),
                KanjiSeed("\u751f", "raw/fresh")
            ),
            vocabPool = listOf(
                VocabSeed("\u98df\u6750", "shokuzai", "ingredients", listOf("ingredients", "route", "ticket", "desk")),
                VocabSeed("\u6ce8\u6587", "chuumon", "order", listOf("order", "schedule", "train line", "payment")),
                VocabSeed("\u5358\u54c1", "tanpin", "single item", listOf("single item", "classroom", "discount", "station")),
                VocabSeed("\u5b9a\u98df", "teishoku", "set meal", listOf("set meal", "ticket gate", "deadline", "contract")),
                VocabSeed("\u98f2\u307f\u7269", "nomimono", "drink", listOf("drink", "meeting", "library", "receipt")),
                VocabSeed("\u7518\u5473", "amami", "sweets", listOf("sweets", "transfer", "lecture", "invoice")),
                VocabSeed("\u8f9b\u53e3", "karakuchi", "spicy", listOf("spicy", "cheap", "boarding", "change")),
                VocabSeed("\u8abf\u7406", "chouri", "cooking", listOf("cooking", "route", "checkout", "station")),
                VocabSeed("\u624b\u9806", "tejun", "steps", listOf("steps", "fare", "delivery", "platform")),
                VocabSeed("\u4f1a\u8a08", "kaikei", "checkout", listOf("checkout", "class", "transfer", "menu")),
                VocabSeed("\u91ce\u83dc", "yasai", "vegetables", listOf("vegetables", "station", "promise", "receipt")),
                VocabSeed("\u679c\u7269", "kudamono", "fruit", listOf("fruit", "cleaning", "ticket", "meeting")),
                VocabSeed("\u30c7\u30b6\u30fc\u30c8", "dezaato", "dessert", listOf("dessert", "direction", "payment", "homework")),
                VocabSeed("\u5f01\u5f53", "bentou", "bento", listOf("bento", "kitchen", "laundry", "route")),
                VocabSeed("\u30e1\u30cb\u30e5\u30fc", "menyuu", "menu", listOf("menu", "stock", "fare", "plan"))
            )
        )
    }

    private fun buildTransportTrack(): TrackSeed {
        return buildSimpleThemeTrack(
            trackId = "transport_core",
            title = "Transport & Signs",
            description = "Theme-specific progression for station flow, tickets, routes, warnings, and travel phrases.",
            accentColor = "#5B7891",
            displayOrder = 10,
            levelTitles = listOf(
                "Stations",
                "Tickets",
                "Directions",
                "Warnings",
                "Travel Phrases",
                "Service Disruptions",
                "Complex Transfers",
                "Travel Dialogues+"
            ),
            kanjiPool = listOf(
                KanjiSeed("\u99c5", "station"),
                KanjiSeed("\u7dda", "line"),
                KanjiSeed("\u53e3", "gate"),
                KanjiSeed("\u9053", "road"),
                KanjiSeed("\u65c5", "travel"),
                KanjiSeed("\u4e57", "board"),
                KanjiSeed("\u964d", "get off"),
                KanjiSeed("\u901f", "fast"),
                KanjiSeed("\u9045", "late"),
                KanjiSeed("\u8eca", "vehicle"),
                KanjiSeed("\u98db", "fly"),
                KanjiSeed("\u51fa", "exit"),
                KanjiSeed("\u5165", "enter"),
                KanjiSeed("\u523b", "engrave/time")
            ),
            vocabPool = listOf(
                VocabSeed("\u6539\u672d", "kaisatsu", "ticket gate", listOf("ticket gate", "menu", "discount", "assignment")),
                VocabSeed("\u5207\u7b26", "kippu", "ticket", listOf("ticket", "kitchen", "meeting", "price")),
                VocabSeed("\u4e57\u63db", "norikae", "transfer", listOf("transfer", "checkout", "dialogue", "lesson")),
                VocabSeed("\u65b9\u9762", "houmen", "direction", listOf("direction", "receipt", "ingredient", "homework")),
                VocabSeed("\u8def\u7dda", "rosen", "route", listOf("route", "menu", "cash", "office")),
                VocabSeed("\u904b\u8cc3", "unchin", "fare", listOf("fare", "dish", "stationery", "topic")),
                VocabSeed("\u7d42\u70b9", "shuuten", "terminal", listOf("terminal", "payment", "set meal", "submission")),
                VocabSeed("\u4e57\u8eca", "jousha", "boarding", listOf("boarding", "lesson", "laundry", "discount")),
                VocabSeed("\u4e0b\u8eca", "gesha", "getting off", listOf("getting off", "inventory", "conversation", "kitchen")),
                VocabSeed("\u9045\u5ef6", "chien", "delay", listOf("delay", "entryway", "sweets", "report")),
                VocabSeed("\u30d0\u30b9", "basu", "bus", listOf("bus", "train", "taxi", "bicycle")),
                VocabSeed("\u98db\u884c\u6a5f", "hikouki", "airplane", listOf("airplane", "ticket", "station", "platform")),
                VocabSeed("\u51fa\u53e3", "deguchi", "exit", listOf("exit", "entrance", "ticket gate", "platform")),
                VocabSeed("\u5165\u53e3", "iriguchi", "entrance", listOf("entrance", "exit", "fare", "delay")),
                VocabSeed("\u6642\u523b\u8868", "jikokuhyou", "timetable", listOf("timetable", "receipt", "menu", "plan"))
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
                KanjiSeed("\u9818", "receipt/territory")
            ),
            vocabPool = listOf(
                VocabSeed("\u4fa1\u683c", "kakaku", "price", listOf("price", "station line", "dish", "class")),
                VocabSeed("\u5408\u8a08", "goukei", "total", listOf("total", "platform", "kitchen", "route")),
                VocabSeed("\u5728\u5eab", "zaiko", "stock", listOf("stock", "ticket gate", "meeting", "fare")),
                VocabSeed("\u652f\u6255\u3044", "shiharai", "payment", listOf("payment", "menu", "schedule", "teacher")),
                VocabSeed("\u73fe\u91d1", "genkin", "cash", listOf("cash", "set meal", "transfer", "dialogue")),
                VocabSeed("\u5272\u5f15", "waribiki", "discount", listOf("discount", "homework", "boarding", "ingredient")),
                VocabSeed("\u8fd4\u54c1", "henpin", "return", listOf("return", "kitchen", "route", "station")),
                VocabSeed("\u4ea4\u63db", "koukan", "exchange", listOf("exchange", "lesson", "laundry", "topic")),
                VocabSeed("\u63a5\u5ba2", "sekkyaku", "customer service", listOf("customer service", "line", "ticket", "menu")),
                VocabSeed("\u8a66\u7740", "shichaku", "fitting", listOf("fitting", "receipt", "meeting", "entryway")),
                VocabSeed("\u30b5\u30a4\u30ba", "saizu", "size", listOf("size", "color", "price", "weight")),
                VocabSeed("\u30ec\u30b8", "reji", "register", listOf("register", "exit", "gate", "counter")),
                VocabSeed("\u888b", "fukuro", "bag", listOf("bag", "box", "plate", "cup")),
                VocabSeed("\u9818\u53ce\u66f8", "ryoushuusho", "receipt", listOf("receipt", "ticket", "menu", "invoice")),
                VocabSeed("\u30bb\u30fc\u30eb", "seeru", "sale", listOf("sale", "return", "exchange", "stock"))
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
            val kanji = cyclicSlice(kanjiPool, start = index * 2, count = 3)
            val vocab = cyclicSlice(vocabPool, start = index * 2, count = 2)
            val choiceAnswer = listOf("desu", "masu", "ni", "de", "ka")[index % 5]
            val clozeAnswer = listOf("shite", "mite", "itte", "kudasai", "onegai")[index % 5]
            val firstTermRuby = furiganaTokenForVocab(vocab[0])
            val secondTermRuby = furiganaTokenForVocab(vocab[1])
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
                        prompt = "${vocab[0].reading} wa benri ( ).",
                        promptFurigana = "${firstTermRuby}は便利{べんり}（　）。",
                        answer = choiceAnswer,
                        choices = listOf("desu", "masu", "ni", "de", "ka"),
                        meaning = "$title grammar pattern"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "${vocab[0].reading} o ( ____ ) kudasai.",
                        promptFurigana = "${firstTermRuby}を（　）ください。",
                        answer = clozeAnswer,
                        accepted = listOf(clozeAnswer),
                        meaning = "$title request pattern"
                    ),
                    sentence = SentenceSeed(
                        prompt = "${vocab[0].reading} to ${vocab[1].reading} ga hitsuyou desu.",
                        promptFurigana = "${firstTermRuby}と${secondTermRuby}が必要{ひつよう}です。",
                        answer = "$levelTitle uses ${vocab[0].meaning} and ${vocab[1].meaning}.",
                        choices = listOf(
                            "$levelTitle uses ${vocab[0].meaning} and ${vocab[1].meaning}.",
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
        "\u7d04" to 9
    )
}
