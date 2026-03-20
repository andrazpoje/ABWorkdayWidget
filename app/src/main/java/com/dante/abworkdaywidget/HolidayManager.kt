package com.dante.abworkdaywidget

import android.content.Context
import android.telephony.TelephonyManager
import java.time.LocalDate
import java.time.MonthDay
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object HolidayManager {

    const val KEY_HOLIDAY_COUNTRY = "holidayCountry"
    const val DEFAULT_COUNTRY = "SI"

    const val KEY_COUNTRY_MANUAL = "holidayCountryManual"

    val supportedCountries = listOf(
        HolidayCountry("SI", "Slovenija"),
        HolidayCountry("AT", "Austria"),
        HolidayCountry("HR", "Croatia")
    )

    private val supportedCountryCodes = supportedCountries.map { it.code }.toSet()

    /**
     * Cache:
     * countryCode -> year -> set of holiday dates
     */
    private val holidayCache = ConcurrentHashMap<String, ConcurrentHashMap<Int, Set<LocalDate>>>()

    fun ensureCountrySelected(context: Context) {
        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)

        val isManual = prefs.getBoolean(KEY_COUNTRY_MANUAL, false)
        if (isManual) return

        if (prefs.contains(KEY_HOLIDAY_COUNTRY)) return

        val detected = detectInitialCountry(context)

        prefs.edit()
            .putString(KEY_HOLIDAY_COUNTRY, detected)
            .apply()
    }

    fun getSelectedCountry(context: Context): String {
        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOLIDAY_COUNTRY, null)
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it in supportedCountryCodes }
            ?: DEFAULT_COUNTRY
    }

    fun saveSelectedCountry(context: Context, countryCode: String) {
        val normalized = countryCode.uppercase(Locale.ROOT)
            .takeIf { it in supportedCountryCodes }
            ?: DEFAULT_COUNTRY

        context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOLIDAY_COUNTRY, normalized)
            .putBoolean(KEY_COUNTRY_MANUAL, true) // 🔥 ključni del
            .apply()
    }

    fun isHoliday(context: Context, date: LocalDate): Boolean {
        val country = getSelectedCountry(context)
        val holidaysForYear = getHolidaysForYear(country, date.year)
        return date in holidaysForYear
    }

    private fun getHolidaysForYear(country: String, year: Int): Set<LocalDate> {
        val yearMap = holidayCache.getOrPut(country) { ConcurrentHashMap() }
        return yearMap.getOrPut(year) {
            buildHolidaySet(country, year)
        }
    }

    private fun buildHolidaySet(country: String, year: Int): Set<LocalDate> {
        return when (country) {
            "AT" -> buildAustriaHolidays(year)
            "HR" -> buildCroatiaHolidays(year)
            else -> buildSloveniaHolidays(year)
        }
    }

    private fun detectInitialCountry(context: Context): String {
        val simCountry = detectCountryFromTelephony(context)
        if (simCountry != null) return simCountry

        val localeCountry = Locale.getDefault().country
            .takeIf { it.isNotBlank() }
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it in supportedCountryCodes }

        return localeCountry ?: DEFAULT_COUNTRY
    }

    private fun detectCountryFromTelephony(context: Context): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            val simCountry = tm?.simCountryIso
                ?.uppercase(Locale.ROOT)
                ?.takeIf { it in supportedCountryCodes }

            if (simCountry != null) return simCountry

            tm?.networkCountryIso
                ?.uppercase(Locale.ROOT)
                ?.takeIf { it in supportedCountryCodes }
        } catch (_: Exception) {
            null
        }
    }

    // ========================
    // 🇸🇮 SLOVENIA
    // ========================

    private fun buildSloveniaHolidays(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)

        return setOf(
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 1, 2),
            LocalDate.of(year, 2, 8),
            LocalDate.of(year, 4, 27),
            LocalDate.of(year, 5, 1),
            LocalDate.of(year, 5, 2),
            LocalDate.of(year, 6, 25),
            LocalDate.of(year, 8, 15),
            LocalDate.of(year, 10, 31),
            LocalDate.of(year, 11, 1),
            LocalDate.of(year, 12, 25),
            LocalDate.of(year, 12, 26),
            easter.plusDays(1) // Easter Monday
        )
    }

    // ========================
    // 🇦🇹 AUSTRIA
    // ========================

    private fun buildAustriaHolidays(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)

        return setOf(
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 1, 6),
            LocalDate.of(year, 5, 1),
            LocalDate.of(year, 8, 15),
            LocalDate.of(year, 10, 26),
            LocalDate.of(year, 11, 1),
            LocalDate.of(year, 12, 8),
            LocalDate.of(year, 12, 25),
            LocalDate.of(year, 12, 26),
            easter.plusDays(1),   // Easter Monday
            easter.plusDays(39),  // Ascension
            easter.plusDays(50)   // Whit Monday
        )
    }

    // ========================
    // 🇭🇷 CROATIA
    // ========================

    private fun buildCroatiaHolidays(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)

        return setOf(
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 1, 6),
            LocalDate.of(year, 5, 1),
            LocalDate.of(year, 5, 30),
            LocalDate.of(year, 6, 22),
            LocalDate.of(year, 8, 5),
            LocalDate.of(year, 8, 15),
            LocalDate.of(year, 11, 1),
            LocalDate.of(year, 11, 18),
            LocalDate.of(year, 12, 25),
            LocalDate.of(year, 12, 26),
            easter.plusDays(1) // Easter Monday
        )
    }

    // ========================
    // COMMON
    // ========================

    private fun easterSunday(year: Int): LocalDate {
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