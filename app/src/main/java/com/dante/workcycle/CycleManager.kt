package com.dante.workcycle

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.util.sanitizeLabel
import java.time.LocalDate

object CycleManager {

    const val PREFS_NAME = "ab_cycle_prefs"
    const val KEY_CYCLE_DAYS = "cycle_days"
    const val KEY_CYCLE_START_DATE = "cycle_start_date"

    fun saveCycle(context: Context, cycle: List<String>) {
        val cleaned = cycle
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val finalCycle = cleaned.ifEmpty { listOf("A", "B") }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_CYCLE_DAYS, finalCycle.joinToString("|"))
            }
    }

    fun loadCycle(context: Context): List<String> {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CYCLE_DAYS, null)

        if (stored.isNullOrBlank()) return listOf("A", "B")

        return stored.split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("A", "B") }
    }

    fun saveStartDate(context: Context, date: LocalDate) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_CYCLE_START_DATE, date.toString())
            }
    }

    fun loadStartDate(context: Context): LocalDate {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CYCLE_START_DATE, null)

        return try {
            if (stored.isNullOrBlank()) LocalDate.now() else LocalDate.parse(stored)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }

    fun getCycleDayForDate(context: Context, date: LocalDate): String {
        val overrideLabel = getSkippedDayOverrideLabelOrNull(context, date)
        if (overrideLabel != null) {
            return overrideLabel
        }

        val cycle = loadCycle(context)
        if (cycle.isEmpty()) return "A"

        val startDate = loadStartDate(context)
        val startIndex = loadFirstCycleDayIndex(context, cycle)

        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val skipSaturdays = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
        val skipSundays = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
        val skipHolidays = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)

        return if (date == startDate) {
            cycle[startIndex]
        } else if (date.isAfter(startDate)) {
            val stepsForward = countIncludedDaysForward(
                context = context,
                fromExclusive = startDate,
                toInclusive = date,
                skipSaturdays = skipSaturdays,
                skipSundays = skipSundays,
                skipHolidays = skipHolidays
            )
            cycle[positiveModulo(startIndex + stepsForward, cycle.size)]
        } else {
            val stepsBack = countIncludedDaysBackward(
                context = context,
                fromInclusive = date,
                toExclusive = startDate,
                skipSaturdays = skipSaturdays,
                skipSundays = skipSundays,
                skipHolidays = skipHolidays
            )
            cycle[positiveModulo(startIndex - stepsBack, cycle.size)]
        }
    }

    private fun loadFirstCycleDayIndex(context: Context, cycle: List<String>): Int {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        val savedFirstDayRaw = prefs.getString(
            AppPrefs.KEY_FIRST_CYCLE_DAY,
            cycle.firstOrNull() ?: "A"
        ) ?: (cycle.firstOrNull() ?: "A")

        val savedFirstDay = sanitizeLabel(
            savedFirstDayRaw,
            cycle.firstOrNull() ?: "A"
        )

        val index = cycle.indexOfFirst { it.equals(savedFirstDay, ignoreCase = true) }

        return if (index >= 0) index else 0
    }

    fun getSkippedDayOverrideLabelOrNull(context: Context, date: LocalDate): String? {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        val overrideEnabled = prefs.getBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
        if (!overrideEnabled) return null

        val skipSaturdays = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
        val skipSundays = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
        val skipHolidays = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)

        val isSkipped = isSkippedDay(
            context = context,
            date = date,
            skipSaturdays = skipSaturdays,
            skipSundays = skipSundays,
            skipHolidays = skipHolidays
        )

        if (!isSkipped) return null

        val label = prefs.getString(AppPrefs.KEY_SKIPPED_LABEL, AppPrefs.DEFAULT_SKIPPED_LABEL)
            ?.trim()
            .orEmpty()
            .ifBlank { AppPrefs.DEFAULT_SKIPPED_LABEL }

        return label
    }

    private fun countIncludedDaysForward(
        context: Context,
        fromExclusive: LocalDate,
        toInclusive: LocalDate,
        skipSaturdays: Boolean,
        skipSundays: Boolean,
        skipHolidays: Boolean
    ): Int {
        var count = 0
        var current = fromExclusive.plusDays(1)

        while (!current.isAfter(toInclusive)) {
            if (isCountedDay(context, current, skipSaturdays, skipSundays, skipHolidays)) {
                count++
            }
            current = current.plusDays(1)
        }

        return count
    }

    private fun countIncludedDaysBackward(
        context: Context,
        fromInclusive: LocalDate,
        toExclusive: LocalDate,
        skipSaturdays: Boolean,
        skipSundays: Boolean,
        skipHolidays: Boolean
    ): Int {
        var count = 0
        var current = fromInclusive

        while (current.isBefore(toExclusive)) {
            if (isCountedDay(context, current, skipSaturdays, skipSundays, skipHolidays)) {
                count++
            }
            current = current.plusDays(1)
        }

        return count
    }

    private fun isCountedDay(
        context: Context,
        date: LocalDate,
        skipSaturdays: Boolean,
        skipSundays: Boolean,
        skipHolidays: Boolean
    ): Boolean {
        return !isSkippedDay(
            context = context,
            date = date,
            skipSaturdays = skipSaturdays,
            skipSundays = skipSundays,
            skipHolidays = skipHolidays
        )
    }

    private fun isSkippedDay(
        context: Context,
        date: LocalDate,
        skipSaturdays: Boolean,
        skipSundays: Boolean,
        skipHolidays: Boolean
    ): Boolean {
        val dayOfWeek = date.dayOfWeek.value

        if (skipSaturdays && dayOfWeek == 6) return true
        if (skipSundays && dayOfWeek == 7) return true
        if (skipHolidays && HolidayManager.isHoliday(context, date)) return true

        return false
    }

    private fun positiveModulo(value: Int, mod: Int): Int {
        return ((value % mod) + mod) % mod
    }
}