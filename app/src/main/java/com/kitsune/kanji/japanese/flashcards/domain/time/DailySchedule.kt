package com.kitsune.kanji.japanese.flashcards.domain.time

import java.time.LocalTime
import java.util.Locale

data class DailySchedule(
    val resetTime: LocalTime,
    val reminderTime: LocalTime
) {
    companion object {
        fun defaultForLocale(locale: Locale = Locale.getDefault()): DailySchedule {
            val eveningResetCountries = setOf("JP", "KR", "TW", "HK", "CN", "SG")
            val country = locale.country.uppercase(Locale.US)
            return if (country in eveningResetCountries) {
                DailySchedule(
                    resetTime = LocalTime.of(18, 0),
                    reminderTime = LocalTime.of(20, 0)
                )
            } else {
                DailySchedule(
                    resetTime = LocalTime.of(12, 0),
                    reminderTime = LocalTime.of(18, 0)
                )
            }
        }
    }
}
