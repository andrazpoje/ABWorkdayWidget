package com.dante.workcycle.ui.worklog

import com.dante.workcycle.R
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogDaySessionMode
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkLogEventValidatorTest {

    @Test
    fun defaultApiUsesSingleSessionBehavior() {
        val result = validateDefault(
            listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
            )
        )

        assertEquals(R.string.work_log_edit_validation_clock_in, result)
    }

    @Test
    fun singleSessionStartFinishStartIsInvalid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            sessionMode = WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY
        )

        assertEquals(R.string.work_log_edit_validation_clock_in, result)
    }

    @Test
    fun multiSessionStartFinishStartIsValid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertNull(result)
    }

    @Test
    fun multiSessionStartFinishStartFinishIsValid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 16, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertNull(result)
    }

    @Test
    fun multiSessionDuplicateClockInWhileActiveIsInvalid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertEquals(R.string.work_log_edit_validation_clock_in, result)
    }

    @Test
    fun multiSessionBreakStartBeforeClockInIsInvalid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.BREAK_START)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertEquals(R.string.work_log_edit_validation_break_start, result)
    }

    @Test
    fun multiSessionBreakEndWithoutActiveBreakIsInvalid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.BREAK_END)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertEquals(R.string.work_log_edit_validation_break_end, result)
    }

    @Test
    fun multiSessionClockOutWithoutActiveSessionIsInvalid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertEquals(R.string.work_log_edit_validation_clock_out, result)
    }

    @Test
    fun multiSessionBreakInSecondSessionIsValid() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 12, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 13, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 14, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 5, hour = 14, minute = 30, type = WorkEventType.BREAK_END),
                event(id = 6, hour = 16, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertNull(result)
    }

    @Test
    fun mealRemainsPermissive() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 12, minute = 0, type = WorkEventType.MEAL)
            ),
            sessionMode = WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY
        )

        assertNull(result)
    }

    @Test
    fun noteRemainsPermissive() {
        val result = validate(
            events = listOf(
                event(id = 1, hour = 12, minute = 0, type = WorkEventType.NOTE)
            ),
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        assertNull(result)
    }

    private fun validateDefault(events: List<WorkEvent>): Int? {
        val originalEvent = events.first()
        return WorkLogEventValidator.validateEditedEvent(
            originalEvent = originalEvent,
            updatedEvent = originalEvent,
            originalDateEvents = events,
            updatedDateEvents = events
        )
    }

    private fun validate(
        events: List<WorkEvent>,
        sessionMode: WorkLogDaySessionMode
    ): Int? {
        val originalEvent = events.first()
        return WorkLogEventValidator.validateEditedEvent(
            originalEvent = originalEvent,
            updatedEvent = originalEvent,
            originalDateEvents = events,
            updatedDateEvents = events,
            sessionMode = sessionMode
        )
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
            time = LocalTime.of(hour, minute),
            type = type,
            createdAt = id
        )
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 4, 29)
    }
}
