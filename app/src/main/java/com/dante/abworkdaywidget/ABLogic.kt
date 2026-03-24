package com.dante.abworkdaywidget

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

object ABLogic {

    fun getTodayLetter(context: Context): String {
        return getLetterForDate(context, LocalDate.now())
    }

    fun getLetterForDate(context: Context, date: LocalDate): String {

        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        val startYear = prefs.getInt(AppPrefs.KEY_START_YEAR, 2026)
        val startMonth = prefs.getInt(AppPrefs.KEY_START_MONTH, 3)
        val startDay = prefs.getInt(AppPrefs.KEY_START_DAY, 2)

        val startIsA = prefs.getBoolean(AppPrefs.KEY_START_IS_A, true)

        val skipSaturdays = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
        val skipSundays = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
        val skipHolidays = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)

        val startDate = LocalDate.of(startYear, startMonth, startDay)

        if (shouldSkipDay(date, skipSaturdays, skipSundays, skipHolidays)) {
            return "X"
        }

        val shift = prefs.getInt(AppPrefs.KEY_CYCLE_SHIFT, 0)

        val workDays = countWorkDays(
            startDate,
            date,
            skipSaturdays,
            skipSundays,
            skipHolidays
        ) + shift

        val isA = if (startIsA) {
            workDays % 2 == 0
        } else {
            workDays % 2 != 0
        }

        return if (isA) "A" else "B"
    }

    private fun shouldSkipDay(
        date: LocalDate,
        skipSaturdays: Boolean,
        skipSundays: Boolean,
        skipHolidays: Boolean
    ): Boolean {
        if (skipSaturdays && date.dayOfWeek == DayOfWeek.SATURDAY) return true
        if (skipSundays && date.dayOfWeek == DayOfWeek.SUNDAY) return true
        if (skipHolidays && WorkdayUtil.isWorkFreeDay(date)) return true
        return false
    }

    private fun countWorkDays(
        start: LocalDate,
        end: LocalDate,
        skipSaturdays: Boolean,
        skipSundays: Boolean,
        skipHolidays: Boolean
    ): Int {

        var count = 0
        var d = start

        while (d.isBefore(end)) {
            d = d.plusDays(1)

            if (shouldSkipDay(d, skipSaturdays, skipSundays, skipHolidays)) continue

            count++
        }

        return count
    }
}