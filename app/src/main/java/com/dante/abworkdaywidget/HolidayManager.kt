package com.dante.abworkdaywidget

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.edit
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object HolidayManager {

    const val DEFAULT_COUNTRY = "SI"

    val supportedCountries = listOf(
        HolidayCountry("SI", R.string.country_slovenia),
        HolidayCountry("AT", R.string.country_austria),
        HolidayCountry("HR", R.string.country_croatia),
        HolidayCountry("IT", R.string.country_italy),
        HolidayCountry("HU", R.string.country_hungary)
    )

    private val supportedCountryCodes = supportedCountries.map { it.code }.toSet()

    /**
     * Cache:
     * countryCode -> year -> set of holiday dates
     */
    private val holidayCache = ConcurrentHashMap<String, ConcurrentHashMap<Int, Set<LocalDate>>>()

    fun ensureCountrySelected(context: Context) {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)
        if (isManual) return

        if (prefs.contains(AppPrefs.KEY_HOLIDAY_COUNTRY)) return

        val detected = detectInitialCountry(context)

        prefs.edit {
            putString(AppPrefs.KEY_HOLIDAY_COUNTRY, detected)
        }
    }

    fun getSelectedCountry(context: Context): String {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        return prefs.getString(AppPrefs.KEY_HOLIDAY_COUNTRY, null)
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it in supportedCountryCodes }
            ?: DEFAULT_COUNTRY
    }

    fun saveSelectedCountry(context: Context, countryCode: String) {
        val normalized = countryCode.uppercase(Locale.ROOT)
            .takeIf { it in supportedCountryCodes }
            ?: DEFAULT_COUNTRY

        context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit {
                putString(AppPrefs.KEY_HOLIDAY_COUNTRY, normalized)
                putBoolean(AppPrefs.KEY_COUNTRY_MANUAL, true)
            }
    }

    fun getCountryDisplayName(context: Context, countryCode: String): String {
        val country = supportedCountries.firstOrNull {
            it.code.equals(countryCode, ignoreCase = true)
        } ?: supportedCountries.first()

        return context.getString(country.nameResId)
    }

    fun getCountryDisplayNameWithAutoDetected(
        context: Context,
        countryCode: String,
        isAutoDetected: Boolean
    ): String {
        val baseName = getCountryDisplayNameWithFlag(context, countryCode)
        return if (isAutoDetected) {
            context.getString(R.string.country_auto_detected_format, baseName)
        } else {
            baseName
        }
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

    fun getCountryFlag(countryCode: String): String {
        return when (countryCode.uppercase(Locale.ROOT)) {
            "SI" -> "\uD83C\uDDF8\uD83C\uDDEE"
            "AT" -> "\uD83C\uDDE6\uD83C\uDDF9"
            "HR" -> "\uD83C\uDDED\uD83C\uDDF7"
            "IT" -> "\uD83C\uDDEE\uD83C\uDDF9"
            "HU" -> "\uD83C\uDDED\uD83C\uDDFA"
            else -> ""
        }
    }

    fun getCountryDisplayNameWithFlag(context: Context, countryCode: String): String {
        val flag = getCountryFlag(countryCode)
        val name = getCountryDisplayName(context, countryCode)
        return if (flag.isNotBlank()) "$flag $name" else name
    }

    private fun buildHolidaySet(country: String, year: Int): Set<LocalDate> {
        return when (country) {
            "AT" -> buildAustriaHolidays(year)
            "HR" -> buildCroatiaHolidays(year)
            "IT" -> buildItalyHolidays(year)
            "HU" -> buildHungaryHolidays(year)
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
    // 🇮🇹 ITALY
    // ========================

    private fun buildItalyHolidays(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)

        return setOf(
            LocalDate.of(year, 1, 1),   // New Year's Day
            LocalDate.of(year, 1, 6),   // Epiphany
            LocalDate.of(year, 4, 25),  // Liberation Day
            LocalDate.of(year, 5, 1),   // Labour Day
            LocalDate.of(year, 6, 2),   // Republic Day
            LocalDate.of(year, 8, 15),  // Assumption Day
            LocalDate.of(year, 11, 1),  // All Saints' Day
            LocalDate.of(year, 12, 8),  // Immaculate Conception
            LocalDate.of(year, 12, 25), // Christmas Day
            LocalDate.of(year, 12, 26), // St. Stephen's Day
            easter.plusDays(1)          // Easter Monday
        )
    }

    // ========================
    // 🇭🇺 HUNGARY
    // ========================

    private fun buildHungaryHolidays(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)

        return setOf(
            LocalDate.of(year, 1, 1),   // New Year's Day
            LocalDate.of(year, 3, 15),  // National Day
            LocalDate.of(year, 5, 1),   // Labour Day
            LocalDate.of(year, 8, 20),  // State Foundation Day
            LocalDate.of(year, 10, 23), // National Day
            LocalDate.of(year, 11, 1),  // All Saints' Day
            LocalDate.of(year, 12, 25), // Christmas Day
            LocalDate.of(year, 12, 26), // Second Day of Christmas
            easter.plusDays(1),         // Easter Monday
            easter.plusDays(50)         // Whit Monday
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