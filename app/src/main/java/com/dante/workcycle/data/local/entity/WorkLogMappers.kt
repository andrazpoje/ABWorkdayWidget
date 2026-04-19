package com.dante.workcycle.data.local.entity

import com.dante.workcycle.domain.model.WorkLog

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