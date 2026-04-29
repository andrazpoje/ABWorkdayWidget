package com.dante.workcycle.data.local.entity

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventEditAudit

/**
 * Converts persisted event rows into domain events while preserving optional
 * manual-edit audit metadata for recent events and future widget consistency.
 */
fun WorkEventEntity.toDomain(): WorkEvent {
    val audit = if (
        editAuditOldTime != null &&
        editAuditNewTime != null &&
        editAuditEditedAt != null &&
        editAuditSource != null
    ) {
        WorkEventEditAudit(
            oldDate = editAuditOldDate ?: date,
            oldTime = editAuditOldTime,
            newDate = editAuditNewDate ?: date,
            newTime = editAuditNewTime,
            editedAt = editAuditEditedAt,
            wasFutureTime = editAuditWasFutureTime,
            source = editAuditSource
        )
    } else {
        null
    }

    return WorkEvent(
        id = id,
        date = date,
        time = time,
        type = type,
        note = note,
        createdAt = createdAt,
        editAudit = audit
    )
}

/**
 * Converts a domain event back to Room without changing its event type or
 * dropping manual correction metadata.
 */
fun WorkEvent.toEntity(): WorkEventEntity {
    val audit = editAudit
    return WorkEventEntity(
        id = id,
        date = date,
        time = time,
        type = type,
        note = note,
        createdAt = createdAt,
        editAuditOldDate = audit?.oldDate,
        editAuditOldTime = audit?.oldTime,
        editAuditNewDate = audit?.newDate,
        editAuditNewTime = audit?.newTime,
        editAuditEditedAt = audit?.editedAt,
        editAuditWasFutureTime = audit?.wasFutureTime ?: false,
        editAuditSource = audit?.source
    )
}
