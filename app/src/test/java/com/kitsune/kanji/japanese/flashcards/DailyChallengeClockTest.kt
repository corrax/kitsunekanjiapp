package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.domain.time.DailyChallengeClock
import com.kitsune.kanji.japanese.flashcards.domain.time.DailySchedule
import java.time.ZoneId
import java.time.LocalTime
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyChallengeClockTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val schedule = DailySchedule(
        resetTime = LocalTime.of(18, 0),
        reminderTime = LocalTime.of(20, 0)
    )

    @Test
    fun currentCycleDate_beforeReset_usesPreviousDate() {
        val now = ZonedDateTime.of(2026, 2, 7, 17, 30, 0, 0, zone)
        val cycleDate = DailyChallengeClock.currentCycleDate(now, schedule)
        assertEquals("2026-02-06", cycleDate.toString())
    }

    @Test
    fun currentCycleDate_afterReset_usesSameDate() {
        val now = ZonedDateTime.of(2026, 2, 7, 18, 30, 0, 0, zone)
        val cycleDate = DailyChallengeClock.currentCycleDate(now, schedule)
        assertEquals("2026-02-07", cycleDate.toString())
    }

    @Test
    fun nextResetNotificationTime_rollsForward() {
        val now = ZonedDateTime.of(2026, 2, 7, 19, 0, 0, 0, zone)
        val next = DailyChallengeClock.nextResetNotificationTime(now, schedule)
        assertEquals("2026-02-08T18:05+09:00[Asia/Tokyo]", next.toString())
    }
}
