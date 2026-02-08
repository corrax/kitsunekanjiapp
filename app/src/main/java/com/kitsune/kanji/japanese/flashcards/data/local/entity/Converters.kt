package com.kitsune.kanji.japanese.flashcards.data.local.entity

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun cardTypeToString(value: CardType): String = value.name

    @TypeConverter
    fun stringToCardType(value: String): CardType = CardType.valueOf(value)

    @TypeConverter
    fun packStatusToString(value: PackProgressStatus): String = value.name

    @TypeConverter
    fun stringToPackStatus(value: String): PackProgressStatus = PackProgressStatus.valueOf(value)

    @TypeConverter
    fun deckTypeToString(value: DeckType): String = value.name

    @TypeConverter
    fun stringToDeckType(value: String): DeckType = DeckType.valueOf(value)
}
