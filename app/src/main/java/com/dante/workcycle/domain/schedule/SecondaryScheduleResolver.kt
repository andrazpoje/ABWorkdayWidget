package com.dante.workcycle.domain.schedule

import android.content.Context
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.SecondaryCyclePrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.model.AssignmentCycleAdvanceMode
import com.dante.workcycle.domain.model.CycleMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SecondaryScheduleResolver(
    private val context: Context
) {

    private val cyclePrefs = SecondaryCyclePrefs(context)
    private val appPrefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    data class SecondaryResolvedDay(
        val baseLabel: String?,
        val cycleIndex: Int?,
        val isEnabled: Boolean,
        val mode: CycleMode
    )

    fun resolve(date: LocalDate): SecondaryResolvedDay {
        val mode = cyclePrefs.getMode()

        if (!cyclePrefs.isEnabled()) {
            return SecondaryResolvedDay(
                baseLabel = null,
                cycleIndex = null,
                isEnabled = false,
                mode = mode
            )
        }

        if (mode != CycleMode.CYCLIC) {
            return SecondaryResolvedDay(
                baseLabel = null,
                cycleIndex = null,
                isEnabled = true,
                mode = mode
            )
        }

        val cycleLabels = cyclePrefs.getCycle()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cycleLabels.isEmpty()) {
            return SecondaryResolvedDay(
                baseLabel = null,
                cycleIndex = null,
                isEnabled = true,
                mode = mode
            )
        }

        val startDate = cyclePrefs.getStartDate()
        val firstCycleDay = cyclePrefs.getFirstCycleDay().trim()
        val advanceMode = cyclePrefs.getAdvanceMode()

        val firstDayIndex = cycleLabels.indexOf(firstCycleDay)
            .takeIf { it >= 0 }
            ?: 0

        val stepOffset = when (advanceMode) {
            AssignmentCycleAdvanceMode.ALL_DAYS -> {
                ChronoUnit.DAYS.between(startDate, date).toInt()
            }

            AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY -> {
                countWorkingCycleSteps(startDate, date)
            }
        }

        val resolvedIndex = Math.floorMod(firstDayIndex + stepOffset, cycleLabels.size)

        return SecondaryResolvedDay(
            baseLabel = cycleLabels[resolvedIndex],
            cycleIndex = resolvedIndex,
            isEnabled = true,
            mode = mode
        )
    }

    private fun countWorkingCycleSteps(startDate: LocalDate, targetDate: LocalDate): Int {
        if (targetDate == startDate) return 0

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
            if (shouldAdvanceOnDate(current)) count++
            current = current.plusDays(1)
        }

        return count
    }

    private fun countBackwardWorkingSteps(startDate: LocalDate, targetDate: LocalDate): Int {
        var current = startDate.minusDays(1)
        var count = 0

        while (!current.isBefore(targetDate)) {
            if (shouldAdvanceOnDate(current)) count++
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