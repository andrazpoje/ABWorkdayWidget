package com.dante.abworkdaywidget

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

object ABLogic {

    fun getTodayLetter(context: Context): String {

        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)

        val startYear = prefs.getInt("startYear", 2026)
        val startMonth = prefs.getInt("startMonth", 3)
        val startDay = prefs.getInt("startDay", 2)

        val startIsA = prefs.getBoolean("startIsA", true)

        val skipWeekends = prefs.getBoolean("skipWeekends", true)
        val skipHolidays = prefs.getBoolean("skipHolidays", true)

        val startDate = LocalDate.of(startYear, startMonth, startDay)
        val today = LocalDate.now()

        if (skipWeekends && isWeekend(today)) {
            return "X"
        }

        if (skipHolidays && WorkdayUtil.isWorkFreeDay(today)) {
            return "X"
        }

        val shift = prefs.getInt("cycleShift", 0)

        val workDays = countWorkDays(
            startDate,
            today,
            skipWeekends,
            skipHolidays
        ) + shift

        val isA = if (startIsA) {
            workDays % 2 == 0
        } else {
            workDays % 2 != 0
        }

        return if (isA) "A" else "B"
    }

    private fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY ||
            date.dayOfWeek == DayOfWeek.SUNDAY
    }

    private fun countWorkDays(
        start: LocalDate,
        end: LocalDate,
        skipWeekends: Boolean,
        skipHolidays: Boolean
    ): Int {

        var count = 0
        var d = start

        while (d.isBefore(end)) {
            d = d.plusDays(1)

            if (skipWeekends && isWeekend(d)) continue
            if (skipHolidays && WorkdayUtil.isWorkFreeDay(d)) continue

            count++
        }

        return count
    }
}