package com.kitsune.kanji.japanese.flashcards.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeById

data class DeckThemeDrawnVisuals(
    @param:DrawableRes val imageRes: Int,
    val baseColor: Color,
    val overlayTop: Color,
    val overlayBottom: Color,
    val panelColor: Color,
    val panelBorder: Color,
    val accent: Color,
    val accentMuted: Color
)

data class CardPalette(
    // Card frame
    val banner: Color,
    val bannerText: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val stackShadow1: Color,
    val stackShadow2: Color,
    val promptText: Color,
    val accentText: Color,
    val mutedText: Color,
    // Choice panel
    val choiceBg: Color,
    val choiceSelectedBg: Color,
    val choiceBorder: Color,
    val choiceText: Color,
    // Hint areas
    val hintBg: Color,
    val hintBorder: Color,
    val hintLabel: Color,
    val hintBody: Color,
    // Header pills
    val pillBg: Color,
    val pillBorder: Color,
    val pillIcon: Color,
    val pillText: Color,
    val pillTextSecondary: Color,
    // Ink pad
    val inkStroke: Color,
    val inkGuide: Color,
    val inkBorder: Color,
    val inkBg: Color,
    val inkToolBg: Color,
    val inkToolBorder: Color,
    val inkToolText: Color,
    // Assist footer
    val assistBorderOff: Color,
    val assistBorderOn: Color,
    val assistLabelOff: Color,
    val assistLabelOn: Color,
    val assistBody: Color,
    // Furigana / reading text
    val furiganaText: Color,
    // Expected answer
    val expectedAnswer: Color,
    // Gesture overlay
    val gestureOverlayBg: Color,
    val gestureOverlayBorder: Color,
    val gestureDetailText: Color,
)

fun cardPaletteFor(sourceId: String?): CardPalette {
    val key = sourceId?.lowercase().orEmpty()
    val banner = when {
        key.contains("jlpt_n3") -> Color(0xFF8B5CF6)
        key.contains("jlpt_n4") -> Color(0xFF2E88EE)
        key.contains("jlpt_n5") || key.contains("foundations") -> Color(0xFFFF6B1A)
        key.contains("daily_life") -> Color(0xFF22C55E)
        key.contains("food") -> Color(0xFFE8853A)
        key.contains("transport") -> Color(0xFF5B9BD5)
        key.contains("shopping") -> Color(0xFFE07882)
        else -> Color(0xFFFF6B1A)
    }
    return buildPalette(
        banner = banner,
        accent = Color(0xFFFF5A00),
        light = Color.White,
        medium = Color(0xFFF5EDE5),
        border = Color(0xFFE0D5CC),
        dark = Color(0xFF2D1E14),
        muted = Color(0xFF9E8C7E),
    )
}

private fun buildPalette(
    banner: Color,
    accent: Color,
    light: Color,
    medium: Color,
    border: Color,
    dark: Color,
    muted: Color,
) = CardPalette(
    // Card frame
    banner = banner,
    bannerText = Color.White,
    cardBg = light,
    cardBorder = border,
    stackShadow1 = border.copy(alpha = 0.35f),
    stackShadow2 = medium.copy(alpha = 0.45f),
    promptText = Color(0xFF1A1A1A),
    accentText = accent,
    mutedText = muted,
    // Choice panel
    choiceBg = light,
    choiceSelectedBg = medium,
    choiceBorder = border,
    choiceText = Color(0xFF1A1A1A),
    // Hint areas
    hintBg = light,
    hintBorder = border,
    hintLabel = muted,
    hintBody = dark,
    // Header pills
    pillBg = Color(0xF2FFFFFF),
    pillBorder = border,
    pillIcon = dark,
    pillText = Color(0xFF1A1A1A),
    pillTextSecondary = muted,
    // Ink pad
    inkStroke = dark,
    inkGuide = border.copy(alpha = 0.6f),
    inkBorder = border,
    inkBg = light,
    inkToolBg = medium.copy(alpha = 0.9f),
    inkToolBorder = border,
    inkToolText = dark,
    // Assist footer
    assistBorderOff = border,
    assistBorderOn = accent,
    assistLabelOff = muted,
    assistLabelOn = dark,
    assistBody = dark,
    // Furigana
    furiganaText = muted,
    // Expected answer
    expectedAnswer = dark,
    // Gesture overlay
    gestureOverlayBg = light,
    gestureOverlayBorder = border,
    gestureDetailText = dark,
)

fun deckThemeDrawnVisuals(themeId: String?): DeckThemeDrawnVisuals {
    val theme = deckThemeById(themeId)
    return when (theme.id) {
        "foundations" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF4EA),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF9F2),
            panelColor = Color(0xEFFFFFFB),
            panelBorder = Color(0xFFFFCCAE),
            accent = Color(0xFFFF5A00),
            accentMuted = Color(0xFFFFD5BF)
        )

        "jlpt_n5" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF1E7),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF5ED),
            panelColor = Color(0xEFFFFFFA),
            panelBorder = Color(0xFFFFC8A8),
            accent = Color(0xFFFF5A00),
            accentMuted = Color(0xFFFFD6BF)
        )

        "jlpt_n4" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFE8F7FF),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F2FAFF),
            panelColor = Color(0xEFFFFFFD),
            panelBorder = Color(0xFFAAD0F5),
            accent = Color(0xFF2E88EE),
            accentMuted = Color(0xFFCCDEF5)
        )

        "jlpt_n3" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFF2EAFF),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F8F2FF),
            panelColor = Color(0xEFFFFFFD),
            panelBorder = Color(0xFFCBB8E8),
            accent = Color(0xFF8B5CF6),
            accentMuted = Color(0xFFE3D8F7)
        )

        "daily_life" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFEFF7F2),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F8FBFF),
            panelColor = Color(0xEFFFFFFC),
            panelBorder = Color(0xFF86EFAC),
            accent = Color(0xFF22C55E),
            accentMuted = Color(0xFFBBF7D0)
        )

        "food" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF3E8),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF8F0),
            panelColor = Color(0xEFFFFFFA),
            panelBorder = Color(0xFFFBBF24),
            accent = Color(0xFFE8853A),
            accentMuted = Color(0xFFFDE68A)
        )

        "transport" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFEAF2FF),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F0F7FF),
            panelColor = Color(0xEFFFFFFD),
            panelBorder = Color(0xFFA8CCE8),
            accent = Color(0xFF5B9BD5),
            accentMuted = Color(0xFFD0E4F7)
        )

        "shopping" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF1F2),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF5F5),
            panelColor = Color(0xEFFFFFFD),
            panelBorder = Color(0xFFFBCFE8),
            accent = Color(0xFFE07882),
            accentMuted = Color(0xFFFCE7F3)
        )

        else -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF1E7),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF8F2),
            panelColor = Color(0xEFFFFFFA),
            panelBorder = Color(0xFFFFC8A8),
            accent = Color(0xFFFF5A00),
            accentMuted = Color(0xFFFFD6BF)
        )
    }
}
