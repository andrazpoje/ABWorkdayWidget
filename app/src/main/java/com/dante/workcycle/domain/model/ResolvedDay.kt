package com.dante.workcycle.domain.model

data class ResolvedDay(
    val baseCycleLabel: String,
    val cycleOverrideLabel: String?,
    val effectiveCycleLabel: String,
    val assignmentLabel: String?,
    val isAssignmentOverridden: Boolean = false
) {
    val hasCycleOverride: Boolean
        get() = !cycleOverrideLabel.isNullOrBlank()

    val hasAssignment: Boolean
        get() = !assignmentLabel.isNullOrBlank()
}