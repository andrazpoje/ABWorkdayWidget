package com.dante.abworkdaywidget

import android.content.Context
import java.time.LocalDate
import java.time.MonthDay

object HolidayManager {

    const val COUNTRY_SLOVENIA = "SI"
    const val DEFAULT_COUNTRY = COUNTRY_SLOVENIA
    const val KEY_HOLIDAY_COUNTRY = "holidayCountry"

    private val fixedSlovenianHolidays = setOf(
        MonthDay.of(1, 1),
        MonthDay.of(1, 2),
        MonthDay.of(2, 8),
        MonthDay.of(4, 27),
        MonthDay.of(5, 1),
        MonthDay.of(5, 2),
        MonthDay.of(6, 25),
        MonthDay.of(8, 15),
        MonthDay.of(10, 31),
        MonthDay.of(11, 1),
        MonthDay.of(12, 25),
        MonthDay.of(12, 26)
    )

    fun getSelectedCountry(context: Context): String {
        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOLIDAY_COUNTRY, DEFAULT_COUNTRY) ?: DEFAULT_COUNTRY
    }

    fun isHoliday(context: Context, date: LocalDate): Boolean {
        return when (getSelectedCountry(context)) {
            COUNTRY_SLOVENIA -> isSlovenianHoliday(date)
            else -> isSlovenianHoliday(date)
        }
    }

    fun getSupportedCountries(context: Context): List<HolidayCountryItem> {
        return listOf(
            HolidayCountryItem(
                code = COUNTRY_SLOVENIA,
                displayName = context.getString(R.string.holiday_country_slovenia)
            )
        )
    }

    private fun isSlovenianHoliday(date: LocalDate): Boolean {
        if (fixedSlovenianHolidays.contains(MonthDay.from(date))) return true

        val easterSunday = calculateEasterSunday(date.year)
        val easterMonday = easterSunday.plusDays(1)

        return date == easterMonday
    }

    private fun calculateEasterSunday(year: Int): LocalDate {
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

    data class HolidayCountryItem(
        val code: String,
        val displayName: String
    )
}