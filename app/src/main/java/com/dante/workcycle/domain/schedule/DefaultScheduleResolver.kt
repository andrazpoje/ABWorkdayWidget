package com.dante.workcycle.domain.schedule

import android.content.Context
import com.dante.workcycle.domain.model.CycleLayer
import com.dante.workcycle.domain.model.CycleMode
import com.dante.workcycle.domain.model.CycleResult
import com.dante.workcycle.domain.model.DaySchedule
import com.dante.workcycle.domain.model.ResolvedDay
import java.time.LocalDate

class DefaultScheduleResolver(
    private val context: Context
) {

    private val manualScheduleRepository = ManualScheduleRepository(context)
    private val secondaryResolver = SecondaryScheduleResolver(context)
    private val cycleOverrideRepository = CycleOverrideRepository(context)
    private val statusRepository = StatusRepository(context)

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

        val primaryResult = CycleResult(
            layer = CycleLayer.PRIMARY,
            label = effectiveCycleLabel,
            source = when {
                cycleOverrideLabel != null -> "primary_override"
                skippedOverrideLabel != null -> "primary_skipped_override"
                else -> "primary_base"
            },
            isOverride = cycleOverrideLabel != null
        )

        val secondaryResolved = secondaryResolver.resolve(date)

        val secondaryBaseLabel = secondaryResolved.baseLabel
            ?.trim()
            ?.ifBlank { null }

        val rawManualLabel = manualScheduleRepository
            .getSecondaryManualLabel(date)
            ?.trim()
            ?.ifBlank { null }

        val isManualMode = secondaryResolved.mode != CycleMode.CYCLIC

        val secondaryOverrideLabel = if (isManualMode) {
            null
        } else {
            rawManualLabel
        }

        val secondaryEffectiveLabel = if (isManualMode) {
            rawManualLabel
        } else {
            secondaryOverrideLabel ?: secondaryBaseLabel
        }

        val secondaryResult = secondaryEffectiveLabel?.let { label ->
            CycleResult(
                layer = CycleLayer.SECONDARY,
                label = label,
                source = when {
                    secondaryOverrideLabel != null -> "secondary_override"
                    secondaryBaseLabel != null -> "secondary_base"
                    isManualMode && rawManualLabel != null -> "secondary_manual"
                    else -> "secondary_manual_only"
                },
                isOverride = secondaryOverrideLabel != null
            )
        }

        val statusLabel = statusRepository.getStatusLabel(date)
            ?.trim()
            ?.ifBlank { null }

        val daySchedule = DaySchedule(
            date = date,
            cycles = listOfNotNull(primaryResult, secondaryResult),
            hasManualOverride = cycleOverrideLabel != null || secondaryOverrideLabel != null
        )

        return ResolvedDay(
            baseCycleLabel = baseCycleLabel,
            cycleOverrideLabel = cycleOverrideLabel,
            effectiveCycleLabel = effectiveCycleLabel,
            secondaryBaseLabel = secondaryBaseLabel,
            secondaryOverrideLabel = secondaryOverrideLabel,
            secondaryEffectiveLabel = secondaryEffectiveLabel,
            statusLabel = statusLabel,
            daySchedule = daySchedule,
            isAssignmentFeatureEnabled = secondaryResolved.isEnabled
        )
    }
}