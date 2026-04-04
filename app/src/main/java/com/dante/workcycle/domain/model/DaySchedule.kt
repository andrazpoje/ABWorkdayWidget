package com.dante.workcycle.domain.model

import java.time.LocalDate

data class DaySchedule(
    val date: LocalDate,
    val cycles: List<CycleResult> = emptyList(),
    val isHoliday: Boolean = false,
    val isSkippedByRules: Boolean = false,
    val hasManualOverride: Boolean = false
) {

    val primaryCycleLabel: String?
        get() = cycles.firstOrNull { it.layer == CycleLayer.PRIMARY }?.label

    val secondaryCycleLabel: String?
        get() = cycles.firstOrNull { it.layer == CycleLayer.SECONDARY }?.label

    /**
     * Backward compatibility:
     * existing UI/widget/statistics can still use finalLabel for now.
     */
    val finalLabel: String
        get() = primaryCycleLabel.orEmpty()

    val displayLabels: List<String>
        get() = cycles.map { it.label }.filter { it.isNotBlank() }

    val combinedDisplayLabel: String
        get() = displayLabels.joinToString(" · ")
}