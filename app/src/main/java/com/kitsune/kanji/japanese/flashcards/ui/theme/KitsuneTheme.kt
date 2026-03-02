package com.kitsune.kanji.japanese.flashcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Color(0xFFFF5A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = Color(0xFF3A1500),
    secondary = Color(0xFF2A211B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD04800),
    tertiaryContainer = Color(0xFFFFEDE5),
    onTertiaryContainer = Color(0xFF3A1500),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF201612),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201612),
    surfaceVariant = Color(0xFFFFF2EB),
    onSurfaceVariant = Color(0xFF3A2A23),
    outline = Color(0xFFFFBE9B),
    outlineVariant = Color(0xFFE8D5C8)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB69A),
    onPrimary = Color(0xFF532014),
    primaryContainer = Color(0xFF6E2B14),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B3A1C),
    tertiary = Color(0xFFFFB69A),
    tertiaryContainer = Color(0xFF5C2510),
    onTertiaryContainer = Color(0xFFFFDBCD),
    background = Color(0xFF1A1512),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF2A231E),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF3A312B),
    onSurfaceVariant = Color(0xFFD5C4B8),
    outline = Color(0xFF8A7568),
    outlineVariant = Color(0xFF4A3D34)
)

private val KitsuneTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

@Composable
fun KitsuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = KitsuneTypography,
        content = content
    )
}
