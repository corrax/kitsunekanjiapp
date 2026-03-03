
fun main() {
    val inputs = listOf(
        "taberu",
        "たべる",
        "shimbun",
        "shinbun",
        "しんぶん",
        "mashou",
        "ましょう",
        "masyou",
        "si",
        "shi",
        "tsu",
        "tu",
        "chi",
        "ti",
        "konpyuutaa",
        "konpyu-ta-",
        "コンピューター"
    )

    inputs.forEach { input ->
        println("Input: $input")
        val forms = comparableAnswerForms(input)
        println("Forms: $forms")
        println("---")
    }
}

// COPIED FROM DeckViewModel.kt (with minimal modification to compile standalone)

private fun normalizeAnswer(raw: String): String {
    return raw.trim()
        .replace(Regex("""[\s\u3000]+"""), "")
        .replace("’", "'")
        .replace("・", "")
        .replace("。", "")
        .replace("、", "")
        .replace(",", "")
        .lowercase()
}

private fun comparableAnswerForms(raw: String): Set<String> {
    val base = normalizeAnswer(raw)
    if (base.isBlank()) return emptySet()
    val forms = linkedSetOf(base)
    val hira = katakanaToHiragana(base)
    if (hira.isNotBlank()) {
        forms += hira
        if (isKanaOnly(hira)) {
            forms += normalizeAnswer(hiraganaToRomaji(hira))
        }
    }
    if (containsLatinLetters(base)) {
        val romajiHira = romanizedToHiragana(base)
        if (romajiHira.isNotBlank()) {
            forms += romajiHira
            forms += normalizeAnswer(hiraganaToRomaji(romajiHira))
        }
    }
    return forms.filter { it.isNotBlank() }.toSet()
}

private fun katakanaToHiragana(text: String): String {
    if (text.isBlank()) return text
    val builder = StringBuilder(text.length)
    text.forEach { ch ->
        builder.append(
            when (ch) {
                in '\u30A1'..'\u30F6' -> (ch.code - 0x60).toChar()
                '\u30FC' -> ch
                else -> ch
            }
        )
    }
    return builder.toString()
}

private fun isKanaOnly(text: String): Boolean {
    return text.all { ch ->
        ch in '\u3040'..'\u309F' || ch == '\u30FC'
    }
}

private fun containsLatinLetters(text: String): Boolean {
    return text.any { it in 'a'..'z' || it in 'A'..'Z' }
}

private fun romanizedToHiragana(romaji: String): String {
    val normalized = romaji
        .lowercase()
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

private fun hiraganaToRomaji(hiragana: String): String {
    val normalized = katakanaToHiragana(hiragana)
    if (normalized.isBlank()) return ""
    val result = StringBuilder()
    var index = 0
    while (index < normalized.length) {
        val current = normalized[index]
        if (current == 'っ') {
            val nextPair = normalized.substring(index + 1).take(2)
            val nextSingle = normalized.getOrNull(index + 1)?.toString().orEmpty()
            val nextRomaji = HIRAGANA_TO_ROMAJI[nextPair] ?: HIRAGANA_TO_ROMAJI[nextSingle]
            if (!nextRomaji.isNullOrBlank()) {
                result.append(nextRomaji.first())
            }
            index += 1
            continue
        }
        val pair = normalized.substring(index).take(2)
        val pairRomaji = HIRAGANA_TO_ROMAJI[pair]
        if (pairRomaji != null) {
            result.append(pairRomaji)
            index += 2
            continue
        }
        val single = HIRAGANA_TO_ROMAJI[current.toString()]
        if (single != null) {
            result.append(single)
        } else if (current != 'ー') {
            result.append(current)
        }
        index += 1
    }
    return result.toString()
}

// Data structures
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
    "ta" to "た", "te" to "て", "to" to "to",
    "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
    "ha" to "は", "hi" to "ひ", "he" to "へ", "ho" to "ほ",
    "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "mo",
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

private val HIRAGANA_TO_ROMAJI = mapOf(
    "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
    "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
    "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
    "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
    "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
    "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
    "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
    "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
    "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
    "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
    "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
    "し" to "shi", "ち" to "chi", "つ" to "tsu", "ふ" to "fu",
    "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
    "さ" to "sa", "す" to "su", "せ" to "se", "そ" to "so",
    "ta" to "ta", "te" to "te", "to" to "to",
    "na" to "na", "ni" to "ni", "nu" to "ぬ", "ne" to "ね", "no" to "の",
    "ha" to "ha", "ひ" to "hi", "へ" to "he", "ほ" to "ho",
    "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
    "や" to "ya", "ゆ" to "yu", "よ" to "yo",
    "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
    "わ" to "wa", "を" to "o", "ん" to "n",
    "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
    "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
    "だ" to "da", "で" to "de", "ど" to "do",
    "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "bo" to "ぼ",
    "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
    "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "o"
)
