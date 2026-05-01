package com.dante.workcycle.domain.worklog

import com.dante.workcycle.domain.model.WorkEvent
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure ordering helper for future overnight shift and workday rollover support.
 *
 * This helper does not change runtime behavior until it is explicitly used by
 * the resolver, validator, accounting, dashboard, or widget layers. For now,
 * event.date is assumed to represent the work date. A future schema decision
 * may still split stored workDate from actualDateTime for stronger evidence
 * and restore/import safety.
 */
object WorkLogEventTimelineOrdering {

    fun orderedForWorkDateTimeline(
        events: List<WorkEvent>,
        workDate: LocalDate,
        rollover: LocalTime
    ): List<WorkEvent> {
        return events.sortedWith(
            compareBy<WorkEvent> { event ->
                WorkdayRolloverCalculator.timelineDateTime(
                    workDate = workDate,
                    eventTime = event.time,
                    rollover = rollover
                )
            }.thenBy { it.id }
        )
    }
}
