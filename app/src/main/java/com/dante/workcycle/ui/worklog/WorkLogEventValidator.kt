package com.dante.workcycle.ui.worklog

import com.dante.workcycle.R
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogDaySessionMode
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver

/**
 * Validates manual Work Log event edits against the current single-session
 * timeline rules.
 *
 * This prevents corrections from creating impossible sequences while preserving
 * auditability. Future multi-session support should update this validator in
 * lockstep with dashboard state, totals, widgets, and recent events.
 */
object WorkLogEventValidator {

    /**
     * Checks both affected dates when an event is moved across days.
     *
     * Returns a string resource id for the first validation error, or null when
     * the edited timeline remains valid.
     */
    fun validateEditedEvent(
        originalEvent: WorkEvent,
        updatedEvent: WorkEvent,
        originalDateEvents: List<WorkEvent>,
        updatedDateEvents: List<WorkEvent>
    ): Int? {
        return validateEditedEvent(
            originalEvent = originalEvent,
            updatedEvent = updatedEvent,
            originalDateEvents = originalDateEvents,
            updatedDateEvents = updatedDateEvents,
            sessionMode = WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY
        )
    }

    fun validateEditedEvent(
        originalEvent: WorkEvent,
        updatedEvent: WorkEvent,
        originalDateEvents: List<WorkEvent>,
        updatedDateEvents: List<WorkEvent>,
        sessionMode: WorkLogDaySessionMode
    ): Int? {
        val originalDayCandidate = buildOriginalDayCandidate(
            originalEvent = originalEvent,
            updatedEvent = updatedEvent,
            originalDateEvents = originalDateEvents
        )

        validateTimeline(originalDayCandidate, sessionMode)?.let { return it }

        val updatedDayCandidate = buildUpdatedDayCandidate(
            originalEvent = originalEvent,
            updatedEvent = updatedEvent,
            updatedDateEvents = updatedDateEvents
        )

        return validateTimeline(updatedDayCandidate, sessionMode)
    }

    private fun buildOriginalDayCandidate(
        originalEvent: WorkEvent,
        updatedEvent: WorkEvent,
        originalDateEvents: List<WorkEvent>
    ): List<WorkEvent> {
        val withoutOriginal = originalDateEvents.filterNot { it.id == originalEvent.id }

        return if (originalEvent.date == updatedEvent.date) {
            withoutOriginal + updatedEvent
        } else {
            withoutOriginal
        }
    }

    private fun buildUpdatedDayCandidate(
        originalEvent: WorkEvent,
        updatedEvent: WorkEvent,
        updatedDateEvents: List<WorkEvent>
    ): List<WorkEvent> {
        val withoutOriginal = updatedDateEvents.filterNot { it.id == originalEvent.id }
        return withoutOriginal + updatedEvent
    }

    private fun validateTimeline(
        events: List<WorkEvent>,
        sessionMode: WorkLogDaySessionMode
    ): Int? {
        var isWorking = false
        var isOnBreak = false
        var hasFinishedSession = false

        for (event in WorkLogSessionStateResolver.ordered(events)) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> {
                    if (
                        isWorking ||
                        (
                            sessionMode == WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY &&
                                hasFinishedSession
                            )
                    ) {
                        return R.string.work_log_edit_validation_clock_in
                    }
                    isWorking = true
                    isOnBreak = false
                }

                WorkEventType.BREAK_START -> {
                    if (!isWorking || isOnBreak) {
                        return R.string.work_log_edit_validation_break_start
                    }
                    isOnBreak = true
                }

                WorkEventType.BREAK_END -> {
                    if (!isWorking || !isOnBreak) {
                        return R.string.work_log_edit_validation_break_end
                    }
                    isOnBreak = false
                }

                WorkEventType.CLOCK_OUT -> {
                    if (!isWorking) {
                        return R.string.work_log_edit_validation_clock_out
                    }
                    isWorking = false
                    isOnBreak = false
                    hasFinishedSession = true
                }

                WorkEventType.MEAL,
                WorkEventType.NOTE -> Unit
            }
        }

        return null
    }
}
