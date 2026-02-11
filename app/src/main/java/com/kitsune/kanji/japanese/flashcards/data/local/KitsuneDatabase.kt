package com.kitsune.kanji.japanese.flashcards.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kitsune.kanji.japanese.flashcards.data.local.dao.KitsuneDao
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardAttemptEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.Converters
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckRunCardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckRunEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackCardCrossRef
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.SrsStateEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.StreakStateEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.TrackAbilityEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.TrackEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.UserPackProgressEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.WritingTemplateEntity

@Database(
    entities = [
        TrackEntity::class,
        PackEntity::class,
        CardEntity::class,
        WritingTemplateEntity::class,
        PackCardCrossRef::class,
        UserPackProgressEntity::class,
        DeckRunEntity::class,
        DeckRunCardEntity::class,
        CardAttemptEntity::class,
        SrsStateEntity::class,
        StreakStateEntity::class,
        TrackAbilityEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KitsuneDatabase : RoomDatabase() {
    abstract fun kitsuneDao(): KitsuneDao
}
