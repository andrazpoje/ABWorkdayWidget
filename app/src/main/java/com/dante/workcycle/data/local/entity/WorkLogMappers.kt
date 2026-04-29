package com.dante.workcycle.data.local.entity

import com.dante.workcycle.domain.model.WorkLog

/**
 * Maps legacy aggregate rows to the domain model without applying event-based
 * session rules.
 */
fun WorkLogEntity.toDomain(): WorkLog {
    return WorkLog(
        id = id,
        date = date,
        startTime = startTime,
        endTime = endTime,
        breakMinutes = breakMinutes,
        note = note,
        primaryLabel = primaryLabel,
        secondaryLabel = secondaryLabel,
        statusLabel = statusLabel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Persists the legacy aggregate model as-is; event audit data lives on
 * WorkEvent/WorkEventEntity instead.
 */
fun WorkLog.toEntity(): WorkLogEntity {
    return WorkLogEntity(
        id = id,
        date = date,
        startTime = startTime,
        endTime = endTime,
        breakMinutes = breakMinutes,
        note = note,
        primaryLabel = primaryLabel,
        secondaryLabel = secondaryLabel,
        statusLabel = statusLabel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
