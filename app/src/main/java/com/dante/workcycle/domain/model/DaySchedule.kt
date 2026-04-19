package com.dante.workcycle.domain.model

import java.time.LocalDate

data class DaySchedule(
    val date: LocalDate,
    val cycles: List<CycleResult> = emptyList(),
    val isHoliday: Boolean = false,
    val isSkippedByRules: Boolean = false,
    val hasManualOverride: Boolean = false
) {
}
