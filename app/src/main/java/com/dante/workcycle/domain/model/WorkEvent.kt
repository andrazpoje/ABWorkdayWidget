package com.dante.workcycle.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Audit metadata for a manual correction of a Work Log event time.
 *
 * Corrections must stay traceable because Work Log data can be used as work
 * time evidence. The current v2.x model stores the latest manual correction on
 * the event; a full edit history is a future extension point.
 */
data class WorkEventEditAudit(
    val oldDate: LocalDate,
    val oldTime: LocalTime,
    val newDate: LocalDate,
    val newTime: LocalTime,
    val editedAt: Long,
    val wasFutureTime: Boolean,
    val source: String = SOURCE_MANUAL_EDIT
) {
    companion object {
        const val SOURCE_MANUAL_EDIT = "manual_edit"
    }
}

/**
 * Event-based Work Log record used by the dashboard, recent events, widgets,
 * and manual edit flow.
 *
 * The current implementation models one completed work session per day. A
 * CLOCK_OUT after the last CLOCK_IN marks the day as finished, and the UI must
 * not offer Start Work again until multi-session support is introduced.
 */
data class WorkEvent(
    val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val type: WorkEventType,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val editAudit: WorkEventEditAudit? = null
)
