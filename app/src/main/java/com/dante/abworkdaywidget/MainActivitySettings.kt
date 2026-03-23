package com.dante.abworkdaywidget

import android.content.Context
import androidx.activity.OnBackPressedCallback
import com.dante.abworkdaywidget.data.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

fun MainActivity.setupBackHandling() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!hasUnsavedChanges) {
                finish()
                return
            }

            showUnsavedChangesDialog(
                onSave = {
                    val saved = saveChangesAndRefresh()
                    if (saved) finish()
                },
                onDiscard = {
                    finish()
                }
            )
        }
    })
}

fun MainActivity.showUnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.unsaved_changes_title)
        .setMessage(R.string.unsaved_changes_message)
        .setPositiveButton(R.string.save) { _, _ ->
            onSave()
        }
        .setNegativeButton(R.string.discard) { _, _ ->
            onDiscard()
        }
        .setNeutralButton(R.string.cancel, null)
        .show()
}

fun MainActivity.saveChangesAndRefresh(): Boolean {
    val validatedCycle = validateAndBuildCycle() ?: return false

    saveSettings(validatedCycle)
    refreshWidget()
    updateTodayStatus()
    updateCyclePreview()
    clearUnsavedChanges()

    android.widget.Toast.makeText(
        this,
        getString(R.string.settings_saved),
        android.widget.Toast.LENGTH_SHORT
    ).show()

    return true
}

fun MainActivity.loadSettings() {
    val prefs = getSharedPreferences("abprefs", Context.MODE_PRIVATE)

    val cycle = CycleManager.loadCycle(this).ifEmpty { listOf("A", "B") }
    val cycleStartDate = CycleManager.loadStartDate(this)

    selectedDate = cycleStartDate

    cycleDaysEdit.setText(cycle.joinToString(", "))

    val savedFirstDayRaw = prefs.getString(
        MainActivity.KEY_FIRST_CYCLE_DAY,
        cycle.firstOrNull() ?: "A"
    ) ?: (cycle.firstOrNull() ?: "A")

    val savedFirstDay = sanitizeLabel(
        savedFirstDayRaw,
        cycle.firstOrNull() ?: "A"
    )

    refreshFirstCycleDayDropdown(savedFirstDay)

    switchSaturdays.isChecked = prefs.getBoolean("skipSaturdays", true)
    switchSundays.isChecked = prefs.getBoolean("skipSundays", true)
    switchHolidays.isChecked = prefs.getBoolean("skipHolidays", true)

    val overrideSkippedDays = prefs.getBoolean("overrideSkippedDays", true)
    val skippedDayLabel = prefs.getString("skippedDayLabel", "Prosto") ?: "Prosto"

    switchOverrideSkippedDays.isChecked = overrideSkippedDays
    skippedDayLabelEdit.setText(skippedDayLabel)
    skippedDayLabelEdit.isEnabled = overrideSkippedDays
    skippedDayLabelEdit.alpha = if (overrideSkippedDays) 1f else 0.5f

    val selectedCountryCode = HolidayManager.getSelectedCountry(this)

    val selectedCountry = supportedCountries.firstOrNull { it.code == selectedCountryCode }
        ?: supportedCountries.first()

    val isManual = prefs.getBoolean(HolidayManager.KEY_COUNTRY_MANUAL, false)

    val displayText =
        if (!isManual && selectedCountry.code == HolidayManager.getSelectedCountry(this)) {
            "${selectedCountry.displayName} (auto-detected)"
        } else {
            selectedCountry.displayName
        }

    holidayCountryDropdown.setText(displayText, false)

    prefixEdit.setText(prefs.getString("prefixText", "") ?: "")

    when (CycleThemeManager.loadTheme(this)) {
        CycleThemeManager.THEME_PASTEL -> themePastel.isChecked = true
        CycleThemeManager.THEME_DARK -> themeDark.isChecked = true
        else -> themeClassic.isChecked = true
    }

    when (AppThemeManager.loadTheme(this)) {
        AppThemeManager.THEME_LIGHT -> appThemeLight.isChecked = true
        AppThemeManager.THEME_DARK -> appThemeDark.isChecked = true
        else -> appThemeSystem.isChecked = true
    }

    val uiPrefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
    val widgetStyle = uiPrefs.getString(
        Prefs.KEY_WIDGET_STYLE,
        Prefs.WIDGET_STYLE_CLASSIC
    ) ?: Prefs.WIDGET_STYLE_CLASSIC

    findViewById<android.widget.RadioButton>(R.id.radioClassic).isChecked =
        widgetStyle == Prefs.WIDGET_STYLE_CLASSIC

    findViewById<android.widget.RadioButton>(R.id.radioMinimal).isChecked =
        widgetStyle == Prefs.WIDGET_STYLE_MINIMAL

    val enabledSwitch = findViewById<SwitchMaterial?>(R.id.switchNotificationsEnabled)
    val silentSwitch = findViewById<SwitchMaterial?>(R.id.switchSilentNotification)

    val notificationsEnabled = uiPrefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
    val silentEnabled = uiPrefs.getBoolean(Prefs.KEY_SILENT_NOTIFICATION, false)

    enabledSwitch?.isChecked = notificationsEnabled
    silentSwitch?.isChecked = notificationsEnabled && silentEnabled
    silentSwitch?.isEnabled = notificationsEnabled

    updateDateText()
    updatePresetSelectionState()
}

fun MainActivity.saveSettings(normalizedCycle: List<String>) {
    val prefs = getSharedPreferences("abprefs", Context.MODE_PRIVATE)

    CycleManager.saveCycle(this, normalizedCycle)
    CycleManager.saveStartDate(this, selectedDate)

    when {
        themePastel.isChecked ->
            CycleThemeManager.saveTheme(this, CycleThemeManager.THEME_PASTEL)

        themeDark.isChecked ->
            CycleThemeManager.saveTheme(this, CycleThemeManager.THEME_DARK)

        else ->
            CycleThemeManager.saveTheme(this, CycleThemeManager.THEME_CLASSIC)
    }

    val selectedAppTheme = when {
        appThemeLight.isChecked -> AppThemeManager.THEME_LIGHT
        appThemeDark.isChecked -> AppThemeManager.THEME_DARK
        else -> AppThemeManager.THEME_SYSTEM
    }
    AppThemeManager.saveTheme(this, selectedAppTheme)
    AppThemeManager.apply(selectedAppTheme)

    val selectedFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        normalizedCycle.firstOrNull() ?: "A"
    )

    firstCycleDayDropdown.setText(selectedFirstDay, false)
    cycleDaysEdit.setText(normalizedCycle.joinToString(", "))

    refreshFirstCycleDayDropdown(selectedFirstDay)

    val selectedCountryCode = supportedCountries
        .firstOrNull {
            val selectedText = holidayCountryDropdown.text?.toString()?.trim().orEmpty()
            selectedText == it.displayName || selectedText.startsWith("${it.displayName} (")
        }
        ?.code
        ?: HolidayManager.DEFAULT_COUNTRY

    prefs.edit()
        .putInt("startYear", selectedDate.year)
        .putInt("startMonth", selectedDate.monthValue)
        .putInt("startDay", selectedDate.dayOfMonth)
        .putString(MainActivity.KEY_FIRST_CYCLE_DAY, selectedFirstDay)
        .putString("prefixText", prefixEdit.text.toString().trim())
        .putBoolean("skipSaturdays", switchSaturdays.isChecked)
        .putBoolean("skipSundays", switchSundays.isChecked)
        .putBoolean("skipHolidays", switchHolidays.isChecked)
        .putBoolean("overrideSkippedDays", switchOverrideSkippedDays.isChecked)
        .putString("skippedDayLabel", sanitizeLabel(skippedDayLabelEdit.text.toString(), "Prosto"))
        .apply()

    HolidayManager.saveSelectedCountry(this, selectedCountryCode)

    val uiPrefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

    val selectedWidgetStyle =
        if (findViewById<android.widget.RadioButton>(R.id.radioMinimal).isChecked) {
            Prefs.WIDGET_STYLE_MINIMAL
        } else {
            Prefs.WIDGET_STYLE_CLASSIC
        }

    val enabledSwitch = findViewById<SwitchMaterial?>(R.id.switchNotificationsEnabled)
    val silentSwitch = findViewById<SwitchMaterial?>(R.id.switchSilentNotification)

    val notificationsEnabled = enabledSwitch?.isChecked ?: false
    val silentEnabled = if (notificationsEnabled) {
        silentSwitch?.isChecked ?: false
    } else {
        false
    }

    uiPrefs.edit()
        .putString(Prefs.KEY_WIDGET_STYLE, selectedWidgetStyle)
        .putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, notificationsEnabled)
        .putBoolean(Prefs.KEY_SILENT_NOTIFICATION, silentEnabled)
        .apply()
}

fun MainActivity.validateAndBuildCycle(): List<String>? {
    val rawInput = cycleDaysEdit.text.toString().trim()

    if (rawInput.isBlank()) {
        showError(getString(R.string.error_cycle_empty))
        return null
    }

    if (rawInput.contains(",,") ||
        rawInput.startsWith(",") ||
        rawInput.endsWith(",")
    ) {
        showError(getString(R.string.error_cycle_invalid_format_detailed, rawInput))
        return null
    }

    val cycle = parseCycleInput(rawInput)

    if (cycle.isEmpty()) {
        showError(getString(R.string.error_cycle_empty))
        return null
    }

    if (cycle.size > MainActivity.MAX_CYCLE_ITEMS) {
        showError(
            getString(
                R.string.error_cycle_too_many,
                cycle.size,
                MainActivity.MAX_CYCLE_ITEMS
            )
        )
        return null
    }

    val duplicateExists = cycle
        .map { it.lowercase() }
        .toSet()
        .size != cycle.size

    if (duplicateExists) {
        showError(getString(R.string.error_cycle_duplicates))
        return null
    }

    val tooLongLabel = cycle.firstOrNull { it.length > MainActivity.MAX_LABEL_LENGTH }
    if (tooLongLabel != null) {
        showError(
            getString(
                R.string.error_label_too_long_detailed,
                tooLongLabel,
                MainActivity.MAX_LABEL_LENGTH
            )
        )
        return null
    }

    val selectedFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        cycle.firstOrNull() ?: "A"
    )

    if (selectedFirstDay.length > MainActivity.MAX_LABEL_LENGTH) {
        showError(
            getString(
                R.string.error_label_too_long_detailed,
                selectedFirstDay,
                MainActivity.MAX_LABEL_LENGTH
            )
        )
        return null
    }

    if (!cycle.any { it.equals(selectedFirstDay, ignoreCase = true) }) {
        showError(getString(R.string.error_first_day_not_in_cycle, selectedFirstDay))
        return null
    }

    val skippedOverrideLabel = sanitizeLabel(skippedDayLabelEdit.text.toString(), "Prosto")
    if (skippedOverrideLabel.length > MainActivity.MAX_LABEL_LENGTH) {
        showError(
            getString(
                R.string.error_label_too_long_detailed,
                skippedOverrideLabel,
                MainActivity.MAX_LABEL_LENGTH
            )
        )
        return null
    }

    return cycle
}

fun MainActivity.parseCycleInput(rawInput: String): List<String> {
    return rawInput
        .split(",")
        .map { sanitizeLabel(it, "") }
        .filter { it.isNotBlank() }
}

fun MainActivity.sanitizeLabel(text: String, fallback: String): String {
    val cleaned = text.trim().replace(Regex("\\s+"), " ")
    return if (cleaned.isEmpty()) fallback else cleaned
}

fun MainActivity.showError(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
}