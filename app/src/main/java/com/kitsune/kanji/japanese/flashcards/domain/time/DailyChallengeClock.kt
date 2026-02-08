package com.kitsune.kanji.japanese.flashcards.domain.time

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object DailyChallengeClock {
    private val defaultSchedule = DailySchedule.defaultForLocale()
    private val resetNotificationOffsetMinutes: Long = 5

    fun currentCycleDate(
        now: ZonedDateTime = ZonedDateTime.now(),
        schedule: DailySchedule = defaultSchedule
    ): LocalDate {
        val resetTime = schedule.resetTime
        return if (now.toLocalTime() >= resetTime) {
            now.toLocalDate()
        } else {
            now.toLocalDate().minusDays(1)
        }
    }

    fun nextResetNotificationTime(
        now: ZonedDateTime = ZonedDateTime.now(),
        schedule: DailySchedule = defaultSchedule
    ): ZonedDateTime {
        val resetTime = schedule.resetTime
        val nextResetBoundary = nextBoundaryAt(resetTime, now)
        return nextResetBoundary.plusMinutes(resetNotificationOffsetMinutes)
    }

    fun nextEveningReminderTime(
        now: ZonedDateTime = ZonedDateTime.now(),
        schedule: DailySchedule = defaultSchedule
    ): ZonedDateTime {
        val eveningReminderTime = schedule.reminderTime
        return nextBoundaryAt(eveningReminderTime, now)
    }

    fun now(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        return ZonedDateTime.now(zoneId)
    }

    private fun nextBoundaryAt(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
        val todayAtTime = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (todayAtTime <= now) todayAtTime.plusDays(1) else todayAtTime
    }
}
