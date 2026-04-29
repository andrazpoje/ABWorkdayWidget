package com.dante.workcycle.domain.worklog

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkLogSessionStateResolverTest {

    @Test
    fun emptyDayResolvesToNotStarted() {
        val state = WorkLogSessionStateResolver.resolve(emptyList(), now = time(12, 0))

        assertEquals(WorkLogSessionStatus.NOT_STARTED, state.status)
        assertTrue(state.canStart)
        assertFalse(state.canFinish)
        assertFalse(state.canStartBreak)
        assertFalse(state.canEndBreak)
        assertFalse(state.requiresLiveTick)
    }

    @Test
    fun startOnlyResolvesToWorking() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)),
            now = time(10, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertFalse(state.canStart)
        assertTrue(state.canFinish)
        assertTrue(state.canStartBreak)
        assertTrue(state.canLogMeal)
        assertTrue(state.requiresLiveTick)
    }

    @Test
    fun startThenBreakStartResolvesToOnBreak() {
        val breakStart = event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START)
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                breakStart
            ),
            now = time(10, 30)
        )

        assertEquals(WorkLogSessionStatus.ON_BREAK, state.status)
        assertEquals(breakStart, state.activeBreakStart)
        assertNotNull(state.activeBreakStart)
        assertTrue(state.canEndBreak)
        assertFalse(state.canStartBreak)
        assertTrue(state.requiresLiveTick)
    }

    @Test
    fun startThenBreakStartThenBreakEndResolvesToWorking() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 3, hour = 10, minute = 30, type = WorkEventType.BREAK_END)
            ),
            now = time(11, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertNull(state.activeBreakStart)
        assertTrue(state.canFinish)
    }

    @Test
    fun startThenFinishResolvesToFinished() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 16, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(17, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertFalse(state.canStart)
        assertFalse(state.canFinish)
        assertFalse(state.requiresLiveTick)
    }

    @Test
    fun startThenBreakStartThenFinishResolvesToFinished() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 3, hour = 16, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(17, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertNull(state.activeBreakStart)
        assertFalse(state.requiresLiveTick)
    }

    @Test
    fun startThenMealMarksMealLogged() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.MEAL)
            ),
            now = time(13, 0)
        )

        assertTrue(state.mealLogged)
        assertFalse(state.canLogMeal)
    }

    @Test
    fun sameTimeEventsAreOrderedByTimeThenId() {
        val first = event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)
        val second = event(id = 2, hour = 8, minute = 0, type = WorkEventType.NOTE)
        val third = event(id = 3, hour = 8, minute = 0, type = WorkEventType.MEAL)

        val state = WorkLogSessionStateResolver.resolve(
            listOf(third, first, second),
            now = time(9, 0)
        )

        assertEquals(listOf(first, second, third), state.orderedEvents)
    }

    @Test
    fun malformedStartFinishStartRemainsFinished() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(14, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertFalse(state.canStart)
    }

    @Test
    fun malformedClockOutBeforeStartStaysNotStarted() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_OUT)),
            now = time(9, 0)
        )

        assertEquals(WorkLogSessionStatus.NOT_STARTED, state.status)
    }

    @Test
    fun malformedBreakBeforeStartStaysNotStarted() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(event(id = 1, hour = 8, minute = 0, type = WorkEventType.BREAK_START)),
            now = time(9, 0)
        )

        assertEquals(WorkLogSessionStatus.NOT_STARTED, state.status)
    }

    @Test
    fun startOnlyWorkedMinutesIncludeTimeUntilNow() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)),
            now = time(10, 30)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertEquals(150L, state.workedMinutes)
    }

    @Test
    fun startThenBreakStartWorkedMinutesDoNotIncludeBreakTime() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START)
            ),
            now = time(10, 30)
        )

        assertEquals(WorkLogSessionStatus.ON_BREAK, state.status)
        assertEquals(120L, state.workedMinutes)
    }

    @Test
    fun startThenBreakThenResumeWorkedMinutesIncludeResumedWorkUntilNow() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 3, hour = 10, minute = 30, type = WorkEventType.BREAK_END)
            ),
            now = time(11, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertEquals(150L, state.workedMinutes)
    }

    @Test
    fun startThenFinishWorkedMinutesDoNotKeepIncreasingAfterFinish() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(13, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertEquals(240L, state.workedMinutes)
    }

    @Test
    fun startThenBreakStartThenFinishDoesNotCountBreakToFinishAsWork() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(13, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertEquals(120L, state.workedMinutes)
    }

    @Test
    fun startThenBreakThenResumeThenFinishWorkedMinutesExcludeBreak() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 3, hour = 10, minute = 30, type = WorkEventType.BREAK_END),
                event(id = 4, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(13, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertEquals(210L, state.workedMinutes)
    }

    @Test
    fun malformedBreakEndBeforeClockInCountsOnlyValidWorkAfterClockIn() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.BREAK_END),
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(10, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertEquals(60L, state.workedMinutes)
    }

    @Test
    fun malformedClockOutBeforeClockInCountsOnlyValidWorkAfterClockIn() {
        val state = WorkLogSessionStateResolver.resolve(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(10, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertEquals(60L, state.workedMinutes)
    }

    private fun event(
        id: Long,
        hour: Int,
        minute: Int,
        type: WorkEventType
    ): WorkEvent {
        return WorkEvent(
            id = id,
            date = DATE,
            time = time(hour, minute),
            type = type,
            createdAt = id
        )
    }

    private fun time(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour, minute)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 4, 29)
    }
}
