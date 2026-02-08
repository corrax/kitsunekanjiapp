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
            description = "Writing-first starter track with 240 core kanji.",
            accentColor = "#C65D3B",
            displayOrder = 1
        )

        val packs = (1..(totalKanji / packSize)).map { level ->
            PackEntity(
                packId = "n5_pack_$level",
                trackId = trackId,
                level = level,
                title = "Pack $level",
                minTotalScore = 80,
                minHandwritingScore = 70,
                cardCount = packSize,
                displayOrder = level
            )
        }

        val cards = kanji.mapIndexed { index, symbol ->
            val id = "n5_${index + 1}"
            val englishPrompt = englishPromptFor(index)
            CardEntity(
                cardId = id,
                type = CardType.KANJI,
                prompt = englishPrompt,
                canonicalAnswer = symbol,
                acceptedAnswersRaw = symbol,
                reading = null,
                meaning = englishPrompt,
                difficulty = (index / packSize) + 1,
                templateId = "tmpl_$id"
            )
        }

        val templates = cards.map { card ->
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

        val packCards = cards.mapIndexed { index, card ->
            val packNumber = (index / packSize) + 1
            PackCardCrossRef(
                packId = "n5_pack_$packNumber",
                cardId = card.cardId,
                position = (index % packSize) + 1
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

        return SeedBundle(
            tracks = listOf(track),
            packs = packs,
            cards = cards,
            templates = templates,
            packCards = packCards,
            progress = progress
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
