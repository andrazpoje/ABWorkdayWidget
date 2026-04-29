package com.dante.workcycle.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Legacy daily aggregate Work Log model.
 *
 * New dashboard and widget behavior is primarily event-based via [WorkEvent],
 * but this aggregate model remains part of the storage/repository surface for
 * compatibility with older Work Log screens.
 */
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
