package com.kitsune.kanji.japanese.flashcards.ui.deckbrowser

import androidx.annotation.DrawableRes
import com.kitsune.kanji.japanese.flashcards.R

data class DeckThemeOption(
    val id: String,
    val title: String,
    val difficulty: String,
    val category: String,
    @DrawableRes val heroRes: Int,
    val contentTrackId: String?,
    val levels: List<DeckLevelOption>
)

data class DeckLevelOption(
    val level: Int,
    val name: String
)

val deckThemeCatalog = listOf(
    DeckThemeOption(
        "foundations",
        "Foundations",
        "Pre-N5",
        "Beginner",
        R.drawable.hero_spring,
        contentTrackId = "foundations",
        levels = listOf(
            DeckLevelOption(1, "Greetings & Basics"),
            DeckLevelOption(2, "Numbers & Counting"),
            DeckLevelOption(3, "Self & Others"),
            DeckLevelOption(4, "Places & Things")
        )
    ),
    DeckThemeOption(
        "jlpt_n5",
        "JLPT N5 Core",
        "Beginner",
        "JLPT",
        R.drawable.hero_spring,
        contentTrackId = "jlpt_n5_core",
        levels = listOf(
            DeckLevelOption(1, "Basics"),
            DeckLevelOption(2, "Everyday Kanji"),
            DeckLevelOption(3, "Core Vocabulary"),
            DeckLevelOption(4, "Sentences"),
            DeckLevelOption(5, "Exam Prep")
        )
    ),
    DeckThemeOption(
        "jlpt_n4",
        "JLPT N4 Builder",
        "Beginner+",
        "JLPT",
        R.drawable.hero_summer,
        contentTrackId = "jlpt_n4_core",
        levels = listOf(
            DeckLevelOption(1, "N4 Foundation"),
            DeckLevelOption(2, "Common Patterns"),
            DeckLevelOption(3, "Grammar Jump"),
            DeckLevelOption(4, "Reading Practice"),
            DeckLevelOption(5, "N4 Exam")
        )
    ),
    DeckThemeOption(
        "jlpt_n3",
        "JLPT N3 Bridge",
        "Intermediate",
        "JLPT",
        R.drawable.hero_autumn,
        contentTrackId = "jlpt_n3_core",
        levels = listOf(
            DeckLevelOption(1, "Kanji Expansion"),
            DeckLevelOption(2, "Mixed Readings"),
            DeckLevelOption(3, "Context Training"),
            DeckLevelOption(4, "Complex Sentences"),
            DeckLevelOption(5, "N3 Exam")
        )
    ),
    DeckThemeOption(
        "konbini",
        "Konbini & Labels",
        "Beginner",
        "Theme",
        R.drawable.pack_scene_food,
        contentTrackId = "konbini_core",
        levels = listOf(
            DeckLevelOption(1, "Drinks & Snacks"),
            DeckLevelOption(2, "Checkout Phrases"),
            DeckLevelOption(3, "Deals & Labels"),
            DeckLevelOption(4, "Allergy & Ingredients"),
            DeckLevelOption(5, "Konbini Conversations")
        )
    ),
    DeckThemeOption(
        "signs",
        "Signs You See Everywhere",
        "Beginner",
        "Theme",
        R.drawable.pack_scene_city,
        contentTrackId = "signs_core",
        levels = listOf(
            DeckLevelOption(1, "Push or Pull"),
            DeckLevelOption(2, "Open or Closed"),
            DeckLevelOption(3, "Don't Do That"),
            DeckLevelOption(4, "Useful Signs"),
            DeckLevelOption(5, "Reading Real Signs")
        )
    ),
    DeckThemeOption(
        "adulting",
        "Mail & Adulting",
        "Beginner+",
        "Theme",
        R.drawable.pack_scene_city,
        contentTrackId = "adulting_core",
        levels = listOf(
            DeckLevelOption(1, "Delivery Basics"),
            DeckLevelOption(2, "Forms & Addresses"),
            DeckLevelOption(3, "Receipts & Fees"),
            DeckLevelOption(4, "Deadlines & Submissions"),
            DeckLevelOption(5, "Office & Reception")
        )
    ),
    DeckThemeOption(
        "daily_life",
        "Daily Life",
        "Beginner",
        "Theme",
        R.drawable.pack_scene_city,
        contentTrackId = "daily_life_core",
        levels = listOf(
            DeckLevelOption(1, "Home"),
            DeckLevelOption(2, "School"),
            DeckLevelOption(3, "Work"),
            DeckLevelOption(4, "Social"),
            DeckLevelOption(5, "Real Dialogues")
        )
    ),
    DeckThemeOption(
        "food",
        "Food & Ordering",
        "Beginner+",
        "Theme",
        R.drawable.pack_scene_food,
        contentTrackId = "food_core",
        levels = listOf(
            DeckLevelOption(1, "What's on the Menu"),
            DeckLevelOption(2, "Protein Roulette"),
            DeckLevelOption(3, "Sizing & Sides"),
            DeckLevelOption(4, "Takeout & Sold Out"),
            DeckLevelOption(5, "Restaurant Dialogues")
        )
    ),
    DeckThemeOption(
        "transport",
        "Trains & Getting Around",
        "Beginner+",
        "Theme",
        R.drawable.pack_scene_mountain,
        contentTrackId = "transport_core",
        levels = listOf(
            DeckLevelOption(1, "Finding Your Way"),
            DeckLevelOption(2, "Express or Local"),
            DeckLevelOption(3, "Transfers & Connections"),
            DeckLevelOption(4, "Service Alerts"),
            DeckLevelOption(5, "Station Dialogues")
        )
    ),
    DeckThemeOption(
        "shopping",
        "Shopping & Prices",
        "Beginner+",
        "Theme",
        R.drawable.pack_scene_temple,
        contentTrackId = "shopping_core",
        levels = listOf(
            DeckLevelOption(1, "Numbers"),
            DeckLevelOption(2, "Items"),
            DeckLevelOption(3, "Payments"),
            DeckLevelOption(4, "Comparisons"),
            DeckLevelOption(5, "Conversations")
        )
    ),
    DeckThemeOption(
        "school",
        "School Situations",
        "Beginner",
        "Theme",
        R.drawable.pack_scene_temple,
        contentTrackId = "school",
        levels = listOf(
            DeckLevelOption(1, "Classroom"),
            DeckLevelOption(2, "Homework"),
            DeckLevelOption(3, "Rules & Requests"),
            DeckLevelOption(4, "Clubs & Plans"),
            DeckLevelOption(5, "Dialogs")
        )
    ),
    DeckThemeOption(
        "work",
        "Work Situations",
        "Beginner+",
        "Theme",
        R.drawable.pack_scene_city,
        contentTrackId = "work",
        levels = listOf(
            DeckLevelOption(1, "Office Basics"),
            DeckLevelOption(2, "Meetings"),
            DeckLevelOption(3, "Requests"),
            DeckLevelOption(4, "Deadlines"),
            DeckLevelOption(5, "Dialogs")
        )
    ),
    DeckThemeOption(
        "conversation",
        "Everyday Conversation",
        "Beginner",
        "Theme",
        R.drawable.pack_scene_mountain,
        contentTrackId = "conversation",
        levels = listOf(
            DeckLevelOption(1, "Greetings"),
            DeckLevelOption(2, "Help & Directions"),
            DeckLevelOption(3, "Plans"),
            DeckLevelOption(4, "Common Patterns"),
            DeckLevelOption(5, "Dialogs")
        )
    )
)

fun deckThemeById(id: String?): DeckThemeOption {
    return deckThemeCatalog.firstOrNull { it.id == id }
        ?: deckThemeCatalog.firstOrNull()
        ?: DeckThemeOption(
            id = "fallback",
            title = "Default Theme",
            difficulty = "Beginner",
            category = "System",
            heroRes = R.drawable.hero_spring,
            contentTrackId = "jlpt_n5_core",
            levels = emptyList()
        )
}
