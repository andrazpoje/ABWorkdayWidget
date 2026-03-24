package com.dante.abworkdaywidget

import java.time.LocalDate

object WorkdayUtil {

    fun isWorkFreeDay(date: LocalDate): Boolean {
        val y = date.year
        val easterMonday = easterDate(y).plusDays(1)

        val holidays = setOf(
            LocalDate.of(y, 1, 1),
            LocalDate.of(y, 1, 2),
            LocalDate.of(y, 2, 8),
            LocalDate.of(y, 4, 27),
            LocalDate.of(y, 5, 1),
            LocalDate.of(y, 5, 2),
            LocalDate.of(y, 6, 25),
            LocalDate.of(y, 8, 15),
            LocalDate.of(y, 10, 31),
            LocalDate.of(y, 11, 1),
            LocalDate.of(y, 12, 25),
            LocalDate.of(y, 12, 26),
            easterMonday
        )

        return date in holidays
    }

    private fun easterDate(year:Int): LocalDate {

        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19*a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2*e + 2*i - h - k) % 7
        val m = (a + 11*h + 22*l) / 451
        val month = (h + l - 7*m + 114) / 31
        val day = ((h + l - 7*m + 114) % 31) + 1

        return LocalDate.of(year,month,day)
    }
}