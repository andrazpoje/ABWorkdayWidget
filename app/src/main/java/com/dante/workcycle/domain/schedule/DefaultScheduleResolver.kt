package com.dante.workcycle.domain.schedule

import android.content.Context
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.domain.model.CycleLayer
import com.dante.workcycle.domain.model.CycleResult
import com.dante.workcycle.domain.model.DaySchedule
import com.dante.workcycle.domain.model.ResolvedDay
import java.time.LocalDate

class DefaultScheduleResolver(
    private val context: Context
) {

    private val assignmentPrefs = AssignmentCyclePrefs(context)
    private val manualScheduleRepository = ManualScheduleRepository(context)

    private val secondaryResolver = SecondaryScheduleResolver(context)
    private val cycleOverrideRepository = CycleOverrideRepository(context)

    fun resolve(date: LocalDate): ResolvedDay {

        // -------------------------
        // PRIMARY LAYER
        // -------------------------

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

        val primaryResult = CycleResult(
            layer = CycleLayer.PRIMARY,
            label = effectiveCycleLabel,
            source = if (cycleOverrideLabel != null) "primary_override" else "primary_base",
            isOverride = cycleOverrideLabel != null
        )

        // -------------------------
        // SECONDARY DISABLED
        // -------------------------

        if (!assignmentPrefs.isEnabled()) {

            val daySchedule = DaySchedule(
                date = date,
                cycles = listOf(primaryResult),
                hasManualOverride = cycleOverrideLabel != null
            )

            return ResolvedDay(
                baseCycleLabel = baseCycleLabel,
                cycleOverrideLabel = cycleOverrideLabel,
                effectiveCycleLabel = effectiveCycleLabel,
                secondaryBaseLabel = null,
                secondaryOverrideLabel = null,
                secondaryEffectiveLabel = null,
                daySchedule = daySchedule,
                isAssignmentFeatureEnabled = false
            )
        }

        // -------------------------
        // SECONDARY BASE (CYCLE)
        // -------------------------

        val secondaryResolved = secondaryResolver.resolve(date)

        val secondaryBaseLabel = secondaryResolved.label
            ?.trim()
            ?.ifBlank { null }

        // -------------------------
        // SECONDARY OVERRIDE
        // -------------------------

        val secondaryOverrideLabel = manualScheduleRepository
            .getSecondaryManualLabel(date)
            ?.trim()
            ?.ifBlank { null }

        val secondaryEffectiveLabel = secondaryOverrideLabel ?: secondaryBaseLabel

        val secondaryResult = secondaryEffectiveLabel?.let { label ->
            CycleResult(
                layer = CycleLayer.SECONDARY,
                label = label,
                source = if (secondaryOverrideLabel != null) {
                    "secondary_override"
                } else {
                    "secondary_base"
                },
                isOverride = secondaryOverrideLabel != null
            )
        }

        // -------------------------
        // COMBINED DAY SCHEDULE
        // -------------------------

        val daySchedule = DaySchedule(
            date = date,
            cycles = listOfNotNull(primaryResult, secondaryResult),
            hasManualOverride = cycleOverrideLabel != null || secondaryOverrideLabel != null
        )

        // -------------------------
        // FINAL RESULT
        // -------------------------

        return ResolvedDay(
            baseCycleLabel = baseCycleLabel,
            cycleOverrideLabel = cycleOverrideLabel,
            effectiveCycleLabel = effectiveCycleLabel,
            secondaryBaseLabel = secondaryBaseLabel,
            secondaryOverrideLabel = secondaryOverrideLabel,
            secondaryEffectiveLabel = secondaryEffectiveLabel,
            daySchedule = daySchedule,
            isAssignmentFeatureEnabled = true
        )
    }
}