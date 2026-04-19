package com.dante.workcycle.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class WorkEvent(
    val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val type: WorkEventType,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)