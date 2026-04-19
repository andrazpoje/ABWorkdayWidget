package com.dante.workcycle.data.local.entity

import com.dante.workcycle.domain.model.WorkEvent

fun WorkEventEntity.toDomain(): WorkEvent {
    return WorkEvent(
        id = id,
        date = date,
        time = time,
        type = type,
        note = note,
        createdAt = createdAt
    )
}

fun WorkEvent.toEntity(): WorkEventEntity {
    return WorkEventEntity(
        id = id,
        date = date,
        time = time,
        type = type,
        note = note,
        createdAt = createdAt
    )
}