package com.kitsune.kanji.japanese.flashcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFFFF5A00),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2A211B),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF201612),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201612),
    outline = Color(0xFFFFBE9B),
    surfaceVariant = Color(0xFFFFF2EB),
    onSurfaceVariant = Color(0xFF3A2A23)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB69A),
    onPrimary = Color(0xFF532014),
    secondary = Color(0xFFA5D6A7),
    background = Color(0xFF1A1512),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF2A231E),
    onSurface = Color(0xFFEDE0D4)
)

@Composable
fun KitsuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
