package com.dante.workcycle.domain.model

data class ResolvedDay(
    val baseCycleLabel: String,
    val cycleOverrideLabel: String?,
    val effectiveCycleLabel: String,

    val secondaryBaseLabel: String?,
    val secondaryOverrideLabel: String?,
    val secondaryEffectiveLabel: String?,

    val statusLabel: String?,

    val daySchedule: DaySchedule? = null,
    val isAssignmentFeatureEnabled: Boolean = false
) {

    val hasSecondaryBase: Boolean
        get() = !secondaryBaseLabel.isNullOrBlank()

    val hasSecondaryOverride: Boolean
        get() = !secondaryOverrideLabel.isNullOrBlank()

    val secondaryLabel: String?
        get() = secondaryEffectiveLabel

    val isSecondaryOverridden: Boolean
        get() = hasSecondaryOverride && hasSecondaryBase
}