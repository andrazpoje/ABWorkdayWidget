package com.dante.abworkdaywidget

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

object ABLogic {

    fun getTodayLetter(context: Context): String {
        return getLetterForDate(context, LocalDate.now())
    }

    fun getLetterForDate(context: Context, date: LocalDate): String {

        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)

        val startYear = prefs.getInt("startYear", 2026)
        val startMonth = prefs.getInt("startMonth", 3)
        val startDay = prefs.getInt("startDay", 2)

        val startIsA = prefs.getBoolean("startIsA", true)

        val skipSaturdays = prefs.getBoolean("skipSaturdays", true)
        val skipSundays = prefs.getBoolean("skipSundays", true)
        val skipHolidays = prefs.getBoolean("skipHolidays", true)

        val startDate = LocalDate.of(startYear, startMonth, startDay)

        if (shouldSkipDay(date, skipSaturdays, skipSundays, skipHolidays)) {
            return "X"
        }

        val shift = prefs.getInt("cycleShift", 0)

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

    private fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY ||
            date.dayOfWeek == DayOfWeek.SUNDAY
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