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
        val n4 = buildJlptN4Track()
        val n3 = buildJlptN3Track()
        val daily = buildDailyChallengeTrack()
        val dailyLife = buildDailyLifeTrack()
        val food = buildFoodTrack()
        val transport = buildTransportTrack()
        val shopping = buildShoppingTrack()
        return SeedBundle(
            tracks = listOf(
                n4.track,
                n3.track,
                daily.track,
                dailyLife.track,
                food.track,
                transport.track,
                shopping.track
            ),
            packs = n4.packs + n3.packs + daily.packs + dailyLife.packs + food.packs + transport.packs + shopping.packs,
            cards = n4.cards + n3.cards + daily.cards + dailyLife.cards + food.cards + transport.cards + shopping.cards,
            templates = n4.templates + n3.templates + daily.templates + dailyLife.templates + food.templates + transport.templates + shopping.templates,
            packCards = n4.packCards + n3.packCards + daily.packCards + dailyLife.packCards + food.packCards + transport.packCards + shopping.packCards,
            progress = n4.progress + n3.progress + daily.progress + dailyLife.progress + food.progress + transport.progress + shopping.progress
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
        val answer: String,
        val choices: List<String>,
        val meaning: String
    )

    private data class ClozeSeed(
        val prompt: String,
        val answer: String,
        val accepted: List<String>,
        val meaning: String
    )

    private data class SentenceSeed(
        val prompt: String,
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
                        answer = "node",
                        choices = listOf("kara", "node", "to", "kedo"),
                        meaning = "reason connector"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Fill blank: Kaigi wa san-ji ( ____ ) hajimarimasu.",
                        answer = "kara",
                        accepted = listOf("kara"),
                        meaning = "time starting point"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kaigi wa san-ji kara hajimarimasu.",
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
                        answer = "kyou wa houkoku o junbi shimasu",
                        accepted = listOf(
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
                        answer = "to",
                        choices = listOf("ni", "to", "de", "made"),
                        meaning = "quotation marker"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Moshi okureru nara, sugu ( ____ ) kudasai.",
                        answer = "renraku shite",
                        accepted = listOf("renraku shite"),
                        meaning = "request with te-form"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kekka wa raishuu happyou saremasu.",
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
                        answer = "kaigi no ato de renraku shite kudasai",
                        accepted = listOf(
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
                        answer = "desu",
                        choices = listOf("desu", "ni", "made", "kara"),
                        meaning = "formal statement"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Watashi no iken o ( ____ ) ii desu ka.",
                        answer = "itte mo",
                        accepted = listOf("itte mo"),
                        meaning = "permission pattern"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kono bun no imi o setsumei shite kudasai.",
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
                        answer = "watashi wa iken o wakariyasuku setsumei shimashita",
                        accepted = listOf(
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
                        answer = "node",
                        choices = listOf("node", "made", "to", "dake"),
                        meaning = "cause and effect"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Okureta riyuu o ( ____ ) kudasai.",
                        answer = "oshiete",
                        accepted = listOf("oshiete"),
                        meaning = "te-form request"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Saikin wa densen ga konde imasu.",
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
                        answer = "chien no riyuu o oshiete kudasai",
                        accepted = listOf(
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
                        answer = "shita",
                        choices = listOf("shita", "suru", "shite", "shiyou"),
                        meaning = "advice pattern"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Kono mondai ni kotae o ( ____ ) kudasai.",
                        answer = "kaite",
                        accepted = listOf("kaite"),
                        meaning = "instruction form"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Shiken ni wa jikan kanri ga hitsuyou desu.",
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
                        answer = "kono mondai wa muzukashii desu ga juuyou desu",
                        accepted = listOf(
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
                        answer = "to",
                        choices = listOf("to", "de", "ni", "yori"),
                        meaning = "quoted thought"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Shiryou o atsumeru tame ni ( ____ ) hitsuyou ga aru.",
                        answer = "chousa ga",
                        accepted = listOf("chousa ga"),
                        meaning = "purpose and requirement"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Shigen no setsuyaku ga hitsuyou desu.",
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
                        answer = "saiyou",
                        choices = listOf("saiyou", "shuuryou", "kakunin", "teian"),
                        meaning = "action selection"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Kono shiryou wa saishin-ban ni ( ____ ) kudasai.",
                        answer = "koushin shite",
                        accepted = listOf("koushin shite"),
                        meaning = "update request"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kaisha wa rainen no saiyou keikaku o happyou shimashita.",
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
                        answer = "giron",
                        choices = listOf("giron", "yoyaku", "seiri", "renshuu"),
                        meaning = "discussion vocabulary"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Saigo ni kekka o ( ____ ) houkoku shimasu.",
                        answer = "matomete",
                        accepted = listOf("matomete"),
                        meaning = "sequence expression"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Chousa no kekka ni motozuite teian o tsukurimasu.",
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
                        answer = "taiou",
                        choices = listOf("taiou", "teian", "saiyou", "hyouka"),
                        meaning = "rapid response expression"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Hinshitsu o iji suru tame ni ( ____ ) hitsuyou da.",
                        answer = "kanri ga",
                        accepted = listOf("kanri ga"),
                        meaning = "maintenance condition"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Shisutemu no iji to kanri wa juuyou desu.",
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
                        answer = "hitsuyou ni naru",
                        choices = listOf("hitsuyou ni naru", "yasuku naru", "muzukashiku nai", "owaru"),
                        meaning = "parallel change"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Shijou no henka ni ( ____ ) taiou shinakereba naranai.",
                        answer = "awasete",
                        accepted = listOf("awasete"),
                        meaning = "must adapt expression"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Kono purojekuto wa daikibo de, juubun na kanri ga irimasu.",
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
                        answer = "kudasai",
                        choices = listOf("kudasai", "mashita", "masen", "deshita"),
                        meaning = "daily polite request"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "Fill blank: Mou ichido ( ____ ) kudasai.",
                        answer = "itte",
                        accepted = listOf("itte"),
                        meaning = "repeat request"
                    ),
                    sentence = SentenceSeed(
                        prompt = "Norikae wa tsugi no eki desu.",
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
            levelTitles = listOf("Home", "School", "Work", "Social", "Real Dialogues"),
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
                KanjiSeed("\u65cf", "family")
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
                VocabSeed("\u5e30\u5b85", "kitaku", "returning home", listOf("returning home", "checkout", "meeting", "station"))
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
            levelTitles = listOf("Ingredients", "Ordering", "Menus", "Cooking Terms", "Food Dialogues"),
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
                KanjiSeed("\u5e97", "shop")
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
                VocabSeed("\u4f1a\u8a08", "kaikei", "checkout", listOf("checkout", "class", "transfer", "menu"))
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
            levelTitles = listOf("Stations", "Tickets", "Directions", "Warnings", "Travel Phrases"),
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
                KanjiSeed("\u8eca", "vehicle")
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
                VocabSeed("\u9045\u5ef6", "chien", "delay", listOf("delay", "entryway", "sweets", "report"))
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
            levelTitles = listOf("Numbers", "Items", "Payments", "Comparisons", "Conversations"),
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
                KanjiSeed("\u5ba2", "customer")
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
                VocabSeed("\u8a66\u7740", "shichaku", "fitting", listOf("fitting", "receipt", "meeting", "entryway"))
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
                        prompt = "$levelTitle: choose the best polite connector ( ).",
                        answer = choiceAnswer,
                        choices = listOf("desu", "masu", "ni", "de", "ka"),
                        meaning = "$title grammar pattern"
                    ),
                    grammarCloze = ClozeSeed(
                        prompt = "$levelTitle: complete the request form ( ____ ) kudasai.",
                        answer = clozeAnswer,
                        accepted = listOf(clozeAnswer),
                        meaning = "$title request pattern"
                    ),
                    sentence = SentenceSeed(
                        prompt = "$levelTitle context: ${vocab[0].term} to ${vocab[1].term} ga juuyou desu.",
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
                        answer = "${vocab[0].reading} to ${vocab[1].reading} o onegai shimasu",
                        accepted = listOf(
                            "${vocab[0].reading} to ${vocab[1].reading} o onegai shimasu",
                            "${vocab[0].reading} o onegai shimasu"
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
        cards += CardEntity(
            cardId = "${trackId}_g_${level}_1",
            type = CardType.GRAMMAR_CHOICE,
            prompt = grammarChoice.prompt,
            canonicalAnswer = grammarChoice.answer,
            acceptedAnswersRaw = grammarChoice.answer,
            reading = grammarChoice.answer,
            meaning = grammarChoice.meaning,
            promptFurigana = null,
            choicesRaw = (grammarChoice.choices + grammarChoice.answer).distinct().joinToString("|"),
            difficulty = difficulty,
            templateId = "tmpl_${trackId}_g_${level}_1"
        )
        cards += CardEntity(
            cardId = "${trackId}_c_${level}_1",
            type = CardType.GRAMMAR_CLOZE_WRITE,
            prompt = grammarCloze.prompt,
            canonicalAnswer = grammarCloze.answer,
            acceptedAnswersRaw = (grammarCloze.accepted + grammarCloze.answer).distinct().joinToString("|"),
            reading = null,
            meaning = grammarCloze.meaning,
            promptFurigana = null,
            choicesRaw = null,
            difficulty = difficulty,
            templateId = "tmpl_${trackId}_c_${level}_1"
        )
        cards += CardEntity(
            cardId = "${trackId}_s_${level}_1",
            type = CardType.SENTENCE_COMPREHENSION,
            prompt = sentence.prompt,
            canonicalAnswer = sentence.answer,
            acceptedAnswersRaw = sentence.answer,
            reading = null,
            meaning = sentence.meaning,
            promptFurigana = null,
            choicesRaw = (sentence.choices + sentence.answer).distinct().joinToString("|"),
            difficulty = difficulty,
            templateId = "tmpl_${trackId}_s_${level}_1"
        )
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
