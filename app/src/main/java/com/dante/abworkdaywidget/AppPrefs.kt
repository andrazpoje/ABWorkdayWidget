package com.dante.abworkdaywidget

object AppPrefs {

    const val NAME = "ab_prefs"

    const val KEY_LAST_SEEN_WHATS_NEW_VERSION = "last_seen_whats_new_version"

    const val KEY_APP_LANGUAGE = "app_language"
    const val APP_LANGUAGE_SYSTEM = "system"

    // legacy start-date based A/B settings
    const val KEY_START_YEAR = "startYear"
    const val KEY_START_MONTH = "startMonth"
    const val KEY_START_DAY = "startDay"
    const val KEY_START_IS_A = "startIsA"
    const val KEY_LABEL_A = "labelA"
    const val KEY_LABEL_B = "labelB"
    const val KEY_CYCLE_SHIFT = "cycleShift"

    // cycle configuration
    const val KEY_FIRST_CYCLE_DAY = "firstCycleDay"

    // rules
    const val KEY_SKIP_SATURDAYS = "skipSaturdays"
    const val KEY_SKIP_SUNDAYS = "skipSundays"
    const val KEY_SKIP_HOLIDAYS = "skipHolidays"

    // labels / display
    const val KEY_PREFIX_TEXT = "prefixText"
    const val KEY_OVERRIDE_SKIPPED = "overrideSkippedDays"
    const val KEY_SKIPPED_LABEL = "skippedDayLabel"
    const val DEFAULT_SKIPPED_LABEL = "Prosto"

    // themes
    const val KEY_CYCLE_THEME = "cycle_theme"
    const val KEY_APP_THEME = "app_theme"

    // holiday country
    const val KEY_HOLIDAY_COUNTRY = "holidayCountry"
    const val KEY_COUNTRY_MANUAL = "holidayCountryManual"
}