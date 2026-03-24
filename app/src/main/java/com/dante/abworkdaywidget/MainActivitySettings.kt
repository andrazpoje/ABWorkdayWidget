package com.dante.abworkdaywidget

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit
import com.dante.abworkdaywidget.data.Prefs
import com.dante.abworkdaywidget.util.parseCycleInput
import com.dante.abworkdaywidget.util.sanitizeLabel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.widget.addTextChangedListener

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
    val prefs = getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    val cycle = CycleManager.loadCycle(this).ifEmpty { listOf("A", "B") }
    val cycleStartDate = CycleManager.loadStartDate(this)

    selectedDate = cycleStartDate

    cycleDaysEdit.setText(cycle.joinToString(", "))

    val savedFirstDayRaw = prefs.getString(
        AppPrefs.KEY_FIRST_CYCLE_DAY,
        cycle.firstOrNull() ?: "A"
    ) ?: (cycle.firstOrNull() ?: "A")

    val savedFirstDay = sanitizeLabel(
        savedFirstDayRaw,
        cycle.firstOrNull() ?: "A"
    )

    refreshFirstCycleDayDropdown(savedFirstDay)

    switchSaturdays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
    switchSundays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
    switchHolidays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)

    val overrideSkippedDays = prefs.getBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
    val skippedDayLabel = prefs.getString(
        AppPrefs.KEY_SKIPPED_LABEL,
        AppPrefs.DEFAULT_SKIPPED_LABEL
    ) ?: AppPrefs.DEFAULT_SKIPPED_LABEL

    switchOverrideSkippedDays.isChecked = overrideSkippedDays
    skippedDayLabelEdit.setText(skippedDayLabel)
    skippedDayLabelEdit.isEnabled = overrideSkippedDays
    skippedDayLabelEdit.alpha = if (overrideSkippedDays) 1f else 0.5f

    val selectedCountryCode = HolidayManager.getSelectedCountry(this)

    val selectedCountry = supportedCountries.firstOrNull { it.code == selectedCountryCode }
        ?: supportedCountries.first()

    val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

    val displayText =
        if (!isManual && selectedCountry.code == HolidayManager.getSelectedCountry(this)) {
            "${selectedCountry.displayName} (auto-detected)"
        } else {
            selectedCountry.displayName
        }

    holidayCountryDropdown.setText(displayText, false)

    prefixEdit.setText(prefs.getString(AppPrefs.KEY_PREFIX_TEXT, "") ?: "")

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
    val prefs = getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

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

    prefs.edit {
        putInt(AppPrefs.KEY_START_YEAR, selectedDate.year)
        putInt(AppPrefs.KEY_START_MONTH, selectedDate.monthValue)
        putInt(AppPrefs.KEY_START_DAY, selectedDate.dayOfMonth)
        putString(AppPrefs.KEY_FIRST_CYCLE_DAY, selectedFirstDay)
        putString(AppPrefs.KEY_PREFIX_TEXT, prefixEdit.text.toString().trim())
        putBoolean(AppPrefs.KEY_SKIP_SATURDAYS, switchSaturdays.isChecked)
        putBoolean(AppPrefs.KEY_SKIP_SUNDAYS, switchSundays.isChecked)
        putBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, switchHolidays.isChecked)
        putBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, switchOverrideSkippedDays.isChecked)
        putString(
            AppPrefs.KEY_SKIPPED_LABEL,
            sanitizeLabel(skippedDayLabelEdit.text.toString(), AppPrefs.DEFAULT_SKIPPED_LABEL)
        )
    }

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

    uiPrefs.edit {
        putString(Prefs.KEY_WIDGET_STYLE, selectedWidgetStyle)
        putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, notificationsEnabled)
        putBoolean(Prefs.KEY_SILENT_NOTIFICATION, silentEnabled)
    }
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

    val skippedOverrideLabel = sanitizeLabel(
        skippedDayLabelEdit.text.toString(),
        AppPrefs.DEFAULT_SKIPPED_LABEL
    )
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

fun MainActivity.migrateLegacySettingsIfNeeded() {
    val cyclePrefs = getSharedPreferences(CycleManager.PREFS_NAME, Context.MODE_PRIVATE)
    val hasCycle = cyclePrefs.contains(CycleManager.KEY_CYCLE_DAYS)
    val hasStartDate = cyclePrefs.contains(CycleManager.KEY_CYCLE_START_DATE)

    val prefs = getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    if (!hasCycle || !hasStartDate) {
        val year = prefs.getInt(AppPrefs.KEY_START_YEAR, 2026)
        val month = prefs.getInt(AppPrefs.KEY_START_MONTH, 3)
        val day = prefs.getInt(AppPrefs.KEY_START_DAY, 2)
        val startIsA = prefs.getBoolean(AppPrefs.KEY_START_IS_A, true)

        val labelA = sanitizeLabel(prefs.getString(AppPrefs.KEY_LABEL_A, "A") ?: "A", "A")
        val labelB = sanitizeLabel(prefs.getString(AppPrefs.KEY_LABEL_B, "B") ?: "B", "B")

        val selectedLegacyDate = java.time.LocalDate.of(year, month, day)
        val cycleStartDate = if (startIsA) selectedLegacyDate else selectedLegacyDate.minusDays(1)
        val legacyFirstDay = if (startIsA) labelA else labelB

        CycleManager.saveCycle(this, listOf(labelA, labelB))
        CycleManager.saveStartDate(this, cycleStartDate)

        prefs.edit {
            putString(AppPrefs.KEY_FIRST_CYCLE_DAY, legacyFirstDay)
        }
    }

    if (!prefs.contains(AppPrefs.KEY_OVERRIDE_SKIPPED)) {
        prefs.edit {
            putBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
        }
    }

    HolidayManager.ensureCountrySelected(this)
}

fun MainActivity.setupWidgetStyleSettings() {
    val prefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

    val radioClassic = findViewById<android.widget.RadioButton>(R.id.radioClassic)
    val radioMinimal = findViewById<android.widget.RadioButton>(R.id.radioMinimal)

    val currentStyle = prefs.getString(
        Prefs.KEY_WIDGET_STYLE,
        Prefs.WIDGET_STYLE_CLASSIC
    ) ?: Prefs.WIDGET_STYLE_CLASSIC

    radioClassic.isChecked = currentStyle == Prefs.WIDGET_STYLE_CLASSIC
    radioMinimal.isChecked = currentStyle == Prefs.WIDGET_STYLE_MINIMAL
}

fun MainActivity.setupNotificationSettings() {
    val prefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

    val enabledSwitch = findViewById<SwitchMaterial?>(R.id.switchNotificationsEnabled) ?: return
    val silentSwitch = findViewById<SwitchMaterial?>(R.id.switchSilentNotification) ?: return

    val notificationsEnabled = prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
    val silentEnabled = prefs.getBoolean(Prefs.KEY_SILENT_NOTIFICATION, false)

    enabledSwitch.isChecked = notificationsEnabled
    silentSwitch.isChecked = notificationsEnabled && silentEnabled
    silentSwitch.isEnabled = notificationsEnabled

    enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
        silentSwitch.isEnabled = isChecked

        if (!isChecked) {
            silentSwitch.isChecked = false
        } else {
            requestNotificationPermissionIfNeeded()
        }

        markUnsavedChanges()
    }

    silentSwitch.setOnCheckedChangeListener { _, _ ->
        markUnsavedChanges()
    }
}

fun MainActivity.setupChangeListeners() {
    switchSaturdays.setOnCheckedChangeListener { _, _ ->
        clearDateCheckResult()
        markUnsavedChanges()
    }

    switchSundays.setOnCheckedChangeListener { _, _ ->
        clearDateCheckResult()
        markUnsavedChanges()
    }

    switchHolidays.setOnCheckedChangeListener { _, _ ->
        clearDateCheckResult()
        markUnsavedChanges()
    }

    switchOverrideSkippedDays.setOnCheckedChangeListener { _, isChecked ->
        skippedDayLabelEdit.isEnabled = isChecked
        skippedDayLabelEdit.alpha = if (isChecked) 1f else 0.5f
        clearDateCheckResult()
        markUnsavedChanges()
    }

    holidayCountryDropdown.setOnItemClickListener { _, _, _, _ ->
        clearDateCheckResult()
        markUnsavedChanges()
    }

    themeClassic.setOnClickListener { markUnsavedChanges() }
    themePastel.setOnClickListener { markUnsavedChanges() }
    themeDark.setOnClickListener { markUnsavedChanges() }

    appThemeSystem.setOnClickListener { markUnsavedChanges() }
    appThemeLight.setOnClickListener { markUnsavedChanges() }
    appThemeDark.setOnClickListener { markUnsavedChanges() }

    prefixEdit.addTextChangedListener {
        markUnsavedChanges()
    }

    cycleDaysEdit.addTextChangedListener {
        clearDateCheckResult()
        refreshFirstCycleDayDropdown()
        validateCycleInput()
        updatePresetSelectionState(markAsChanged = true)
    }

    skippedDayLabelEdit.addTextChangedListener {
        clearDateCheckResult()
        markUnsavedChanges()
    }

    findViewById<android.widget.RadioGroup>(R.id.widgetStyleRadioGroup)
        .setOnCheckedChangeListener { _, _ -> markUnsavedChanges() }
}

fun MainActivity.showError(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
}