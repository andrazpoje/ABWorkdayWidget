package com.dante.workcycle.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class WorkLog(
    val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val breakMinutes: Int = 0,
    val note: String? = null,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    val statusLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)