package com.dante.workcycle.domain.model

data class ResolvedDay(
    val baseCycleLabel: String,
    val cycleOverrideLabel: String?,
    val effectiveCycleLabel: String,

    val secondaryBaseLabel: String?,
    val secondaryOverrideLabel: String?,
    val secondaryEffectiveLabel: String?,

    val daySchedule: DaySchedule? = null,
    val isAssignmentFeatureEnabled: Boolean = false
) {
    val hasCycleOverride: Boolean
        get() = !cycleOverrideLabel.isNullOrBlank()

    val hasSecondaryBase: Boolean
        get() = !secondaryBaseLabel.isNullOrBlank()

    val hasSecondaryOverride: Boolean
        get() = !secondaryOverrideLabel.isNullOrBlank()

    val hasSecondaryEffective: Boolean
        get() = !secondaryEffectiveLabel.isNullOrBlank()

    val assignmentLabel: String?
        get() = secondaryEffectiveLabel

    val isAssignmentOverridden: Boolean
        get() = hasSecondaryOverride
}