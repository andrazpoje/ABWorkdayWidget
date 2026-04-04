package com.dante.workcycle.domain.schedule

import android.content.Context
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.domain.model.CycleMode
import com.dante.workcycle.domain.model.ResolvedDay
import java.time.LocalDate

class DefaultScheduleResolver(
    private val context: Context
) {

    private val assignmentPrefs = AssignmentCyclePrefs(context)
    private val manualScheduleRepository = ManualScheduleRepository(context)
    private val assignmentResolver = AssignmentScheduleResolver(context)
    private val cycleOverrideRepository = CycleOverrideRepository(context)

    fun resolve(date: LocalDate): ResolvedDay {
        val baseCycleLabel = CycleManager.getCycleDayForDate(context, date)

        val skippedOverrideLabel = CycleManager.getSkippedDayOverrideLabelOrNull(context, date)
            ?.trim()
            ?.ifBlank { null }

        val cycleOverrideLabel = cycleOverrideRepository.getOverrideLabel(date)
            ?.trim()
            ?.ifBlank { null }

        val effectiveCycleLabel = when {
            cycleOverrideLabel != null -> cycleOverrideLabel
            skippedOverrideLabel != null -> skippedOverrideLabel
            else -> baseCycleLabel
        }

        if (!assignmentPrefs.isEnabled()) {
            return ResolvedDay(
                baseCycleLabel = baseCycleLabel,
                cycleOverrideLabel = cycleOverrideLabel,
                effectiveCycleLabel = effectiveCycleLabel,
                assignmentLabel = null,
                isAssignmentOverridden = false
            )
        }

        val assignment = resolveAssignment(date)

        return ResolvedDay(
            baseCycleLabel = baseCycleLabel,
            cycleOverrideLabel = cycleOverrideLabel,
            effectiveCycleLabel = effectiveCycleLabel,
            assignmentLabel = assignment.label,
            isAssignmentOverridden = assignment.isOverride
        )
    }

    private fun resolveAssignment(date: LocalDate): AssignmentResult {
        val manual = manualScheduleRepository
            .getSecondaryManualLabel(date)
            ?.trim()
            ?.ifBlank { null }

        if (manual != null) {
            return AssignmentResult(
                label = manual,
                isOverride = true
            )
        }

        return when (assignmentPrefs.getMode()) {
            CycleMode.CYCLIC -> {
                val resolved = assignmentResolver.resolve(date)
                AssignmentResult(
                    label = resolved.label?.trim()?.ifBlank { null },
                    isOverride = false
                )
            }

            CycleMode.MANUAL -> {
                AssignmentResult(
                    label = null,
                    isOverride = false
                )
            }
        }
    }

    private data class AssignmentResult(
        val label: String?,
        val isOverride: Boolean
    )
}