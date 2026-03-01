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
            panelBorder = Color(0xFFBDD3EF),
            accent = Color(0xFF2E7BC9),
            accentMuted = Color(0xFFCCDEF5)
        )

        "jlpt_n3" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFF2EAFF),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F8F2FF),
            panelColor = Color(0xEFFFFFFD),
            panelBorder = Color(0xFFD6C8F1),
            accent = Color(0xFF7150A8),
            accentMuted = Color(0xFFE3D8F7)
        )

        "daily_life" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFEFF7F2),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F8FBFF),
            panelColor = Color(0xEFFFFFFC),
            panelBorder = Color(0xFFBCDCC7),
            accent = Color(0xFF3E8360),
            accentMuted = Color(0xFFD2E9DA)
        )

        "food" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF3E8),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF8F0),
            panelColor = Color(0xEFFFFFFA),
            panelBorder = Color(0xFFFFC4A1),
            accent = Color(0xFFB86A3C),
            accentMuted = Color(0xFFFFDFC7)
        )

        "transport" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFEAF2FF),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2F0F7FF),
            panelColor = Color(0xEFFFFFFD),
            panelBorder = Color(0xFFBDD3EF),
            accent = Color(0xFF5A7897),
            accentMuted = Color(0xFFD4E3F2)
        )

        "shopping" -> DeckThemeDrawnVisuals(
            imageRes = theme.heroRes,
            baseColor = Color(0xFFFFF2EA),
            overlayTop = Color(0xCCFFFFFF),
            overlayBottom = Color(0xF2FFF8F3),
            panelColor = Color(0xEFFFFFFB),
            panelBorder = Color(0xFFFFD0B2),
            accent = Color(0xFF8B6B55),
            accentMuted = Color(0xFFE8D7CA)
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
