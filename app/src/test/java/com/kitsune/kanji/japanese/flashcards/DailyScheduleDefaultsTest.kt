package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.domain.time.DailySchedule
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyScheduleDefaultsTest {
    @Test
    fun defaultForLocale_japan_usesEveningDefaults() {
        val schedule = DailySchedule.defaultForLocale(Locale.JAPAN)
        assertEquals("18:00", schedule.resetTime.toString())
        assertEquals("20:00", schedule.reminderTime.toString())
    }

    @Test
    fun defaultForLocale_us_usesMiddayReset() {
        val schedule = DailySchedule.defaultForLocale(Locale.US)
        assertEquals("12:00", schedule.resetTime.toString())
        assertEquals("18:00", schedule.reminderTime.toString())
    }
}
