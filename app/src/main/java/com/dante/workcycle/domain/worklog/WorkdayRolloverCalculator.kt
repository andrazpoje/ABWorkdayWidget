package com.dante.workcycle.domain.worklog

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure date/time helper for future overnight shift and workday rollover support.
 *
 * This foundation does not change runtime behavior by itself. If rollover is
 * later wired into Work Log runtime, `WorkEvent.date` can be interpreted as the
 * work date while [timelineDateTime] gives events a stable ordering position
 * inside that work date. A future schema decision may still split stored
 * workDate from actualDateTime for stronger evidence and restore/import safety.
 */
object WorkdayRolloverCalculator {

    fun effectiveWorkDate(now: LocalDateTime, rollover: LocalTime): LocalDate {
        val calendarDate = now.toLocalDate()
        if (rollover == LocalTime.MIDNIGHT) return calendarDate

        return if (now.toLocalTime().isBefore(rollover)) {
            calendarDate.minusDays(1)
        } else {
            calendarDate
        }
    }

    fun timelineDateTime(
        workDate: LocalDate,
        eventTime: LocalTime,
        rollover: LocalTime
    ): LocalDateTime {
        val base = LocalDateTime.of(workDate, eventTime)
        if (rollover == LocalTime.MIDNIGHT) return base

        return if (eventTime.isBefore(rollover)) {
            base.plusDays(1)
        } else {
            base
        }
    }
}
