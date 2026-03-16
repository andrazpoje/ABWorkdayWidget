package com.dante.abworkdaywidget

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeParseException

object CycleManager {

    private const val PREFS_NAME = "ab_cycle_prefs"
    private const val KEY_CYCLE_DAYS = "cycle_days"
    private const val KEY_START_DATE = "cycle_start_date"

    private const val RULES_PREFS_NAME = "abprefs"
    private const val KEY_SKIP_SATURDAYS = "skipSaturdays"
    private const val KEY_SKIP_SUNDAYS = "skipSundays"
    private const val KEY_SKIP_HOLIDAYS = "skipHolidays"

    private val DEFAULT_CYCLE = listOf("A", "B")
    private const val OFF_LABEL = "Prosto"

    fun saveCycle(context: Context, days: List<String>) {
        val cleanedDays = days
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val finalDays = if (cleanedDays.isEmpty()) DEFAULT_CYCLE else cleanedDays

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CYCLE_DAYS, finalDays.joinToString(","))
            .apply()
    }

    fun loadCycle(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val stored = prefs.getString(KEY_CYCLE_DAYS, null)

        val cycle = stored
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: DEFAULT_CYCLE

        return if (cycle.isEmpty()) DEFAULT_CYCLE else cycle
    }

    fun saveStartDate(context: Context, date: LocalDate) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_START_DATE, date.toString())
            .apply()
    }

    fun loadStartDate(context: Context): LocalDate {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_START_DATE, null)

        return try {
            if (stored.isNullOrBlank()) {
                LocalDate.now()
            } else {
                LocalDate.parse(stored)
            }
        } catch (_: DateTimeParseException) {
            LocalDate.now()
        }
    }

    fun getCycleDayForDate(context: Context, date: LocalDate): String {
        val cycle = loadCycle(context)
        val startDate = loadStartDate(context)

        if (cycle.isEmpty()) return DEFAULT_CYCLE.first()

        if (!isCycleActiveOnDate(context, date)) {
            return OFF_LABEL
        }

        val index = getCycleIndexForDate(context, startDate, date, cycle.size)
        return cycle[index]
    }

    fun getTodayCycleDay(context: Context): String {
        return getCycleDayForDate(context, LocalDate.now())
    }

    private fun getCycleIndexForDate(
        context: Context,
        startDate: LocalDate,
        targetDate: LocalDate,
        cycleSize: Int
    ): Int {
        if (cycleSize <= 0) return 0

        if (targetDate == startDate) return 0

        return if (targetDate.isAfter(startDate)) {
            val advancedSteps = countActiveDaysBetweenExclusiveStart(
                context = context,
                startExclusive = startDate,
                endInclusive = targetDate
            )
            advancedSteps % cycleSize
        } else {
            val backwardsSteps = countActiveDaysBetweenInclusiveStartExclusiveEnd(
                context = context,
                startInclusive = targetDate,
                endExclusive = startDate
            )
            val rawIndex = ((-backwardsSteps) % cycleSize + cycleSize) % cycleSize
            rawIndex
        }
    }

    private fun countActiveDaysBetweenExclusiveStart(
        context: Context,
        startExclusive: LocalDate,
        endInclusive: LocalDate
    ): Int {
        var count = 0
        var current = startExclusive.plusDays(1)

        while (!current.isAfter(endInclusive)) {
            if (isCycleActiveOnDate(context, current)) {
                count++
            }
            current = current.plusDays(1)
        }

        return count
    }

    private fun countActiveDaysBetweenInclusiveStartExclusiveEnd(
        context: Context,
        startInclusive: LocalDate,
        endExclusive: LocalDate
    ): Int {
        var count = 0
        var current = startInclusive

        while (current.isBefore(endExclusive)) {
            if (isCycleActiveOnDate(context, current)) {
                count++
            }
            current = current.plusDays(1)
        }

        return count
    }

    private fun isCycleActiveOnDate(context: Context, date: LocalDate): Boolean {
        val prefs = context.getSharedPreferences(RULES_PREFS_NAME, Context.MODE_PRIVATE)

        val skipSaturdays = prefs.getBoolean(KEY_SKIP_SATURDAYS, true)
        val skipSundays = prefs.getBoolean(KEY_SKIP_SUNDAYS, true)
        val skipHolidays = prefs.getBoolean(KEY_SKIP_HOLIDAYS, true)

        if (skipSaturdays && date.dayOfWeek == DayOfWeek.SATURDAY) return false
        if (skipSundays && date.dayOfWeek == DayOfWeek.SUNDAY) return false
        if (skipHolidays && isSlovenianPublicHoliday(date)) return false

        return true
    }

    private fun isSlovenianPublicHoliday(date: LocalDate): Boolean {
        val fixedHolidays = setOf(
            LocalDate.of(date.year, Month.JANUARY, 1),
            LocalDate.of(date.year, Month.JANUARY, 2),
            LocalDate.of(date.year, Month.FEBRUARY, 8),
            LocalDate.of(date.year, Month.APRIL, 27),
            LocalDate.of(date.year, Month.MAY, 1),
            LocalDate.of(date.year, Month.MAY, 2),
            LocalDate.of(date.year, Month.JUNE, 25),
            LocalDate.of(date.year, Month.AUGUST, 15),
            LocalDate.of(date.year, Month.OCTOBER, 31),
            LocalDate.of(date.year, Month.NOVEMBER, 1),
            LocalDate.of(date.year, Month.DECEMBER, 25),
            LocalDate.of(date.year, Month.DECEMBER, 26)
        )

        if (date in fixedHolidays) return true

        val easterSunday = getEasterSunday(date.year)
        val easterMonday = easterSunday.plusDays(1)
        val pentecostSunday = easterSunday.plusDays(49)

        return date == easterMonday || date == pentecostSunday
    }

    private fun getEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return LocalDate.of(year, month, day)
    }
}