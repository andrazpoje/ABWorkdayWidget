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
    fun multiSessionStartFinishStartFinishReturnsTwoSessions() {
        val firstStart = event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)
        val firstFinish = event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT)
        val secondStart = event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
        val secondFinish = event(id = 4, hour = 16, minute = 0, type = WorkEventType.CLOCK_OUT)

        val state = resolveMultiple(
            listOf(firstStart, firstFinish, secondStart, secondFinish),
            now = time(17, 0)
        )

        assertEquals(WorkLogSessionStatus.FINISHED, state.status)
        assertEquals(2, state.sessions.size)
        assertEquals(1, state.sessions[0].index)
        assertEquals(2, state.sessions[1].index)
        assertEquals(firstStart, state.sessions[0].clockIn)
        assertEquals(firstFinish, state.sessions[0].clockOut)
        assertEquals(secondStart, state.sessions[1].clockIn)
        assertEquals(secondFinish, state.sessions[1].clockOut)
    }

    @Test
    fun multiSessionActiveSecondSessionResolvesToWorking() {
        val state = resolveMultiple(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(14, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertEquals(2, state.sessions.size)
        assertEquals(300L, state.workedMinutes)
        assertTrue(state.canFinish)
        assertTrue(state.canStartBreak)
        assertFalse(state.canStart)
    }

    @Test
    fun multiSessionSecondSessionBreakResolvesToOnBreak() {
        val breakStart = event(id = 4, hour = 14, minute = 0, type = WorkEventType.BREAK_START)
        val state = resolveMultiple(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN),
                breakStart
            ),
            now = time(14, 30)
        )

        assertEquals(WorkLogSessionStatus.ON_BREAK, state.status)
        assertEquals(breakStart, state.activeBreakStart)
        assertEquals(breakStart, state.sessions[1].activeBreakStart)
        assertEquals(300L, state.workedMinutes)
        assertTrue(state.canEndBreak)
        assertFalse(state.canStartBreak)
        assertTrue(state.requiresLiveTick)
    }

    @Test
    fun multiSessionWorkedMinutesSumAcrossSessions() {
        val state = resolveMultiple(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 16, minute = 30, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(17, 0)
        )

        assertEquals(330L, state.workedMinutes)
        assertEquals(120L, state.sessions[0].workedMinutes)
        assertEquals(210L, state.sessions[1].workedMinutes)
    }

    @Test
    fun multiSessionFirstClockInAndLastClockOutSpanValidSessions() {
        val firstStart = event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)
        val secondFinish = event(id = 4, hour = 16, minute = 0, type = WorkEventType.CLOCK_OUT)
        val state = resolveMultiple(
            listOf(
                firstStart,
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN),
                secondFinish
            ),
            now = time(17, 0)
        )

        assertEquals(firstStart, state.firstClockIn)
        assertEquals(secondFinish, state.lastClockOut)
    }

    @Test
    fun multiSessionMealLoggedRemainsDayLevel() {
        val state = resolveMultiple(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 11, minute = 0, type = WorkEventType.MEAL),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 4, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(14, 0)
        )

        assertTrue(state.mealLogged)
        assertFalse(state.canLogMeal)
    }

    @Test
    fun multiSessionCanStartAfterFinishedOnlyInMultipleSessionMode() {
        val events = listOf(
            event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
            event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT)
        )

        val singleSessionState = WorkLogSessionStateResolver.resolve(events, now = time(13, 0))
        val multiSessionState = resolveMultiple(events, now = time(13, 0))

        assertEquals(WorkLogSessionStatus.FINISHED, singleSessionState.status)
        assertEquals(WorkLogSessionStatus.FINISHED, multiSessionState.status)
        assertFalse(singleSessionState.canStart)
        assertTrue(multiSessionState.canStart)
    }

    @Test
    fun multiSessionDuplicateClockInWhileActiveIsIgnored() {
        val duplicateStart = event(id = 2, hour = 9, minute = 0, type = WorkEventType.CLOCK_IN)
        val state = resolveMultiple(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                duplicateStart,
                event(id = 3, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            now = time(11, 0)
        )

        assertEquals(1, state.sessions.size)
        assertEquals(120L, state.workedMinutes)
        assertFalse(state.sessions.first().events.contains(duplicateStart))
    }

    @Test
    fun multiSessionBreakStartOutsideActiveSessionIsIgnored() {
        val invalidBreakStart = event(id = 1, hour = 8, minute = 0, type = WorkEventType.BREAK_START)
        val state = resolveMultiple(
            listOf(
                invalidBreakStart,
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(10, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertEquals(60L, state.workedMinutes)
        assertFalse(state.sessions.first().events.contains(invalidBreakStart))
    }

    @Test
    fun multiSessionClockOutWithoutActiveSessionIsIgnored() {
        val invalidClockOut = event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_OUT)
        val state = resolveMultiple(
            listOf(
                invalidClockOut,
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            now = time(10, 0)
        )

        assertEquals(WorkLogSessionStatus.WORKING, state.status)
        assertNull(state.lastClockOut)
        assertEquals(60L, state.workedMinutes)
        assertFalse(state.sessions.first().events.contains(invalidClockOut))
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

    private fun resolveMultiple(
        events: List<WorkEvent>,
        now: LocalTime
    ): WorkLogSessionState {
        return WorkLogSessionStateResolver.resolve(
            events = events,
            now = now,
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 4, 29)
    }
}
