package com.dante.workcycle.domain.worklog

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WorkdayRolloverCalculatorTest {

    @Test
    fun effectiveWorkDateWithMidnightRolloverReturnsCalendarDate() {
        val result = WorkdayRolloverCalculator.effectiveWorkDate(
            now = LocalDateTime.of(2026, 5, 2, 2, 0),
            rollover = LocalTime.MIDNIGHT
        )

        assertEquals(LocalDate.of(2026, 5, 2), result)
    }

    @Test
    fun effectiveWorkDateBeforeRolloverReturnsPreviousDate() {
        val result = WorkdayRolloverCalculator.effectiveWorkDate(
            now = LocalDateTime.of(2026, 5, 2, 2, 0),
            rollover = LocalTime.of(4, 0)
        )

        assertEquals(LocalDate.of(2026, 5, 1), result)
    }

    @Test
    fun effectiveWorkDateAtRolloverReturnsCurrentDate() {
        val result = WorkdayRolloverCalculator.effectiveWorkDate(
            now = LocalDateTime.of(2026, 5, 2, 4, 0),
            rollover = LocalTime.of(4, 0)
        )

        assertEquals(LocalDate.of(2026, 5, 2), result)
    }

    @Test
    fun effectiveWorkDateAfterRolloverReturnsCurrentDate() {
        val result = WorkdayRolloverCalculator.effectiveWorkDate(
            now = LocalDateTime.of(2026, 5, 2, 5, 0),
            rollover = LocalTime.of(4, 0)
        )

        assertEquals(LocalDate.of(2026, 5, 2), result)
    }

    @Test
    fun effectiveWorkDateBeforeLateRolloverReturnsPreviousDate() {
        val result = WorkdayRolloverCalculator.effectiveWorkDate(
            now = LocalDateTime.of(2026, 5, 2, 22, 59),
            rollover = LocalTime.of(23, 0)
        )

        assertEquals(LocalDate.of(2026, 5, 1), result)
    }

    @Test
    fun timelineDateTimeWithMidnightRolloverMapsDirectly() {
        val result = WorkdayRolloverCalculator.timelineDateTime(
            workDate = workDate,
            eventTime = LocalTime.of(2, 0),
            rollover = LocalTime.MIDNIGHT
        )

        assertEquals(LocalDateTime.of(2026, 5, 1, 2, 0), result)
    }

    @Test
    fun timelineDateTimeBeforeMidnightStaysOnWorkDate() {
        val result = WorkdayRolloverCalculator.timelineDateTime(
            workDate = workDate,
            eventTime = LocalTime.of(22, 0),
            rollover = LocalTime.of(4, 0)
        )

        assertEquals(LocalDateTime.of(2026, 5, 1, 22, 0), result)
    }

    @Test
    fun timelineDateTimeAfterMidnightBeforeRolloverMovesToNextCalendarDay() {
        val result = WorkdayRolloverCalculator.timelineDateTime(
            workDate = workDate,
            eventTime = LocalTime.of(2, 0),
            rollover = LocalTime.of(4, 0)
        )

        assertEquals(LocalDateTime.of(2026, 5, 2, 2, 0), result)
    }

    @Test
    fun timelineDateTimeAtRolloverStaysOnWorkDate() {
        val result = WorkdayRolloverCalculator.timelineDateTime(
            workDate = workDate,
            eventTime = LocalTime.of(4, 0),
            rollover = LocalTime.of(4, 0)
        )

        assertEquals(LocalDateTime.of(2026, 5, 1, 4, 0), result)
    }

    @Test
    fun sortingByTimelineDateTimeOrdersNightShiftEvents() {
        val ordered = listOf(
            LocalTime.of(2, 0),
            LocalTime.of(22, 0)
        ).sortedBy { eventTime ->
            WorkdayRolloverCalculator.timelineDateTime(
                workDate = workDate,
                eventTime = eventTime,
                rollover = LocalTime.of(4, 0)
            )
        }

        assertEquals(listOf(LocalTime.of(22, 0), LocalTime.of(2, 0)), ordered)
    }

    @Test
    fun sortingByTimelineDateTimeWithMidnightRolloverKeepsSameDayOrder() {
        val ordered = listOf(
            LocalTime.of(22, 0),
            LocalTime.of(2, 0)
        ).sortedBy { eventTime ->
            WorkdayRolloverCalculator.timelineDateTime(
                workDate = workDate,
                eventTime = eventTime,
                rollover = LocalTime.MIDNIGHT
            )
        }

        assertEquals(listOf(LocalTime.of(2, 0), LocalTime.of(22, 0)), ordered)
    }

    private companion object {
        val workDate: LocalDate = LocalDate.of(2026, 5, 1)
    }
}
