package com.dante.workcycle.domain.worklog

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WorkLogEventTimelineOrderingTest {

    @Test
    fun midnightRolloverPreservesNormalTimeAndIdOrdering() {
        val events = listOf(
            event(id = 4, hour = 10, type = WorkEventType.CLOCK_OUT),
            event(id = 2, hour = 8, type = WorkEventType.CLOCK_IN),
            event(id = 3, hour = 8, type = WorkEventType.BREAK_START)
        )

        val ordered = order(events, rollover = LocalTime.MIDNIGHT)

        assertEquals(listOf(2L, 3L, 4L), ordered.map { it.id })
    }

    @Test
    fun sameTimeEventsAreOrderedById() {
        val events = listOf(
            event(id = 7, hour = 8, type = WorkEventType.BREAK_START),
            event(id = 5, hour = 8, type = WorkEventType.CLOCK_IN),
            event(id = 6, hour = 8, type = WorkEventType.NOTE)
        )

        val ordered = order(events, rollover = LocalTime.of(4, 0))

        assertEquals(listOf(5L, 6L, 7L), ordered.map { it.id })
    }

    @Test
    fun rolloverOrdersNightStartBeforeAfterMidnightFinish() {
        val events = listOf(
            event(id = 2, hour = 2, type = WorkEventType.CLOCK_OUT),
            event(id = 1, hour = 22, type = WorkEventType.CLOCK_IN)
        )

        val ordered = order(events, rollover = LocalTime.of(4, 0))

        assertEquals(listOf(1L, 2L), ordered.map { it.id })
    }

    @Test
    fun rolloverOrdersAfterMidnightEventsByTimelineDateTime() {
        val events = listOf(
            event(id = 3, hour = 2, type = WorkEventType.CLOCK_OUT),
            event(id = 1, hour = 22, type = WorkEventType.CLOCK_IN),
            event(id = 2, hour = 1, type = WorkEventType.NOTE)
        )

        val ordered = order(events, rollover = LocalTime.of(4, 0))

        assertEquals(listOf(1L, 2L, 3L), ordered.map { it.id })
    }

    @Test
    fun rolloverTreatsRolloverTimeAsWorkDateTime() {
        val events = listOf(
            event(id = 2, hour = 3, minute = 59, type = WorkEventType.CLOCK_OUT),
            event(id = 1, hour = 4, minute = 0, type = WorkEventType.CLOCK_IN)
        )

        val ordered = order(events, rollover = LocalTime.of(4, 0))

        assertEquals(listOf(1L, 2L), ordered.map { it.id })
    }

    @Test
    fun unorderedInputReturnsDeterministicOutput() {
        val events = listOf(
            event(id = 5, hour = 2, minute = 30, type = WorkEventType.CLOCK_OUT),
            event(id = 2, hour = 22, minute = 0, type = WorkEventType.CLOCK_IN),
            event(id = 4, hour = 1, minute = 30, type = WorkEventType.BREAK_END),
            event(id = 3, hour = 1, minute = 0, type = WorkEventType.BREAK_START)
        )

        val ordered = order(events, rollover = LocalTime.of(4, 0))

        assertEquals(listOf(2L, 3L, 4L, 5L), ordered.map { it.id })
    }

    @Test
    fun emptyListReturnsEmptyList() {
        val ordered = order(emptyList(), rollover = LocalTime.of(4, 0))

        assertTrue(ordered.isEmpty())
    }

    private fun order(
        events: List<WorkEvent>,
        rollover: LocalTime
    ): List<WorkEvent> {
        return WorkLogEventTimelineOrdering.orderedForWorkDateTimeline(
            events = events,
            workDate = workDate,
            rollover = rollover
        )
    }

    private fun event(
        id: Long,
        hour: Int,
        minute: Int = 0,
        type: WorkEventType
    ): WorkEvent {
        return WorkEvent(
            id = id,
            date = workDate,
            time = LocalTime.of(hour, minute),
            type = type
        )
    }

    private companion object {
        val workDate: LocalDate = LocalDate.of(2026, 5, 1)
    }
}
