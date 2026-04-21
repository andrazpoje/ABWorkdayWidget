package com.dante.workcycle.ui.worklog

import com.dante.workcycle.R
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType

object WorkLogEventValidator {

    fun validateEditedEvent(
        originalEvent: WorkEvent,
        updatedEvent: WorkEvent,
        originalDateEvents: List<WorkEvent>,
        updatedDateEvents: List<WorkEvent>
    ): Int? {
        val originalDayCandidate = buildOriginalDayCandidate(
            originalEvent = originalEvent,
            updatedEvent = updatedEvent,
            originalDateEvents = originalDateEvents
        )

        validateTimeline(originalDayCandidate)?.let { return it }

        val updatedDayCandidate = buildUpdatedDayCandidate(
            originalEvent = originalEvent,
            updatedEvent = updatedEvent,
            updatedDateEvents = updatedDateEvents
        )

        return validateTimeline(updatedDayCandidate)
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

    private fun validateTimeline(events: List<WorkEvent>): Int? {
        var isWorking = false
        var isOnBreak = false

        for (event in events.sortedWith(compareBy<WorkEvent> { it.time }.thenBy { it.id })) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> {
                    if (isWorking) {
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
                }

                WorkEventType.MEAL,
                WorkEventType.NOTE -> Unit
            }
        }

        return null
    }
}
