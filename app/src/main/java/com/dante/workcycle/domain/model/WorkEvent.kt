package com.dante.workcycle.domain.model

import java.time.LocalDate
import java.time.LocalTime

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

data class WorkEvent(
    val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val type: WorkEventType,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val editAudit: WorkEventEditAudit? = null
)
