package com.kitsune.kanji.japanese.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import com.kitsune.kanji.japanese.flashcards.ui.KitsuneRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val deepLinkThemeId = parseDeepLinkThemeId()
        setContent {
            KitsuneRoot(deepLinkThemeId = deepLinkThemeId)
        }
    }

    private fun parseDeepLinkThemeId(): String? {
        val uri = intent?.data ?: return null
        // kitsune://theme/{themeId}
        if (uri.scheme == "kitsune" && uri.host == "theme") {
            return uri.pathSegments?.firstOrNull()
        }
        return null
    }
}
