package com.dante.workcycle.domain.schedule

import android.content.Context
import android.graphics.Color
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.model.AssignmentCycleAdvanceMode
import com.dante.workcycle.domain.model.CycleMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AssignmentScheduleResolver(
    private val context: Context
) {

    private val cyclePrefs = AssignmentCyclePrefs(context)
    private val labelsPrefs = AssignmentLabelsPrefs(context)
    private val appPrefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    data class SecondaryResolvedDay(
        val label: String?,
        val color: Int?,
        val index: Int?,
        val isEnabled: Boolean
    )

    fun resolve(date: LocalDate): SecondaryResolvedDay {
        if (!cyclePrefs.isEnabled()) {
            return SecondaryResolvedDay(
                label = null,
                color = null,
                index = null,
                isEnabled = false
            )
        }

        if (cyclePrefs.getMode() != CycleMode.CYCLIC) {
            return SecondaryResolvedDay(
                label = null,
                color = null,
                index = null,
                isEnabled = true
            )
        }

        val cycleLabels = cyclePrefs.getCycle()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cycleLabels.isEmpty()) {
            return SecondaryResolvedDay(
                label = null,
                color = null,
                index = null,
                isEnabled = true
            )
        }

        val startDate = cyclePrefs.getStartDate()
        val advanceMode = cyclePrefs.getAdvanceMode()

        val resolvedIndex = when (advanceMode) {
            AssignmentCycleAdvanceMode.ALL_DAYS -> {
                val dayOffset = ChronoUnit.DAYS.between(startDate, date).toInt()
                Math.floorMod(dayOffset, cycleLabels.size)
            }

            AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY -> {
                val workingOffset = countWorkingCycleSteps(startDate, date)
                Math.floorMod(workingOffset, cycleLabels.size)
            }
        }

        val resolvedLabel = cycleLabels[resolvedIndex]
        val labelConfig = labelsPrefs.getLabelByName(resolvedLabel)

        return SecondaryResolvedDay(
            label = resolvedLabel,
            color = labelConfig?.color ?: Color.WHITE,
            index = resolvedIndex,
            isEnabled = true
        )
    }

    private fun countWorkingCycleSteps(startDate: LocalDate, targetDate: LocalDate): Int {
        if (targetDate == startDate) {
            return 0
        }

        return if (targetDate.isAfter(startDate)) {
            countForwardWorkingSteps(startDate, targetDate)
        } else {
            -countBackwardWorkingSteps(startDate, targetDate)
        }
    }

    private fun countForwardWorkingSteps(startDate: LocalDate, targetDate: LocalDate): Int {
        var current = startDate
        var count = 0

        while (current.isBefore(targetDate)) {
            if (shouldAdvanceOnDate(current)) {
                count++
            }
            current = current.plusDays(1)
        }

        return count
    }

    private fun countBackwardWorkingSteps(startDate: LocalDate, targetDate: LocalDate): Int {
        var current = startDate.minusDays(1)
        var count = 0

        while (!current.isBefore(targetDate)) {
            if (shouldAdvanceOnDate(current)) {
                count++
            }
            current = current.minusDays(1)
        }

        return count
    }

    private fun shouldAdvanceOnDate(date: LocalDate): Boolean {
        val skipSaturdays = appPrefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, false)
        val skipSundays = appPrefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, false)
        val skipHolidays = appPrefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, false)

        if (skipSaturdays && date.dayOfWeek == DayOfWeek.SATURDAY) return false
        if (skipSundays && date.dayOfWeek == DayOfWeek.SUNDAY) return false
        if (skipHolidays && HolidayManager.isHoliday(context, date)) return false

        return true
    }
}