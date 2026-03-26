package com.dante.abworkdaywidget

import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import com.dante.abworkdaywidget.util.parseCycleInput
import com.dante.abworkdaywidget.util.sanitizeLabel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun HomeFragment.showUnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    MaterialAlertDialogBuilder(requireContext())
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

fun HomeFragment.saveChangesAndRefresh(): Boolean {
    val validatedCycle = validateAndBuildCycle() ?: return false

    saveSettings(validatedCycle)
    refreshWidget()
    updateTodayStatus()
    updateCyclePreview()
    clearUnsavedChanges()

    Toast.makeText(
        requireContext(),
        getString(R.string.settings_saved),
        Toast.LENGTH_SHORT
    ).show()

    return true
}

fun HomeFragment.loadSettings() {
    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    val cycle = CycleManager.loadCycle(requireContext()).ifEmpty { listOf("A", "B") }
    val cycleStartDate = CycleManager.loadStartDate(requireContext())

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

    val selectedCountryCode = HolidayManager.getSelectedCountry(requireContext())

    val selectedCountry = supportedCountries.firstOrNull { it.code == selectedCountryCode }
        ?: supportedCountries.first()

    val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

    val displayText = HolidayManager.getCountryDisplayNameWithAutoDetected(
        context = requireContext(),
        countryCode = selectedCountry.code,
        isAutoDetected = !isManual && selectedCountry.code == HolidayManager.getSelectedCountry(requireContext())
    )

    holidayCountryDropdown.setText(displayText, false)

    prefixEdit.setText(prefs.getString(AppPrefs.KEY_PREFIX_TEXT, "") ?: "")

    when (CycleThemeManager.loadTheme(requireContext())) {
        CycleThemeManager.THEME_PASTEL -> themePastel.isChecked = true
        CycleThemeManager.THEME_DARK -> themeDark.isChecked = true
        else -> themeClassic.isChecked = true
    }

    updateDateText()
    updatePresetSelectionState()
}

fun HomeFragment.saveSettings(normalizedCycle: List<String>) {
    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    CycleManager.saveCycle(requireContext(), normalizedCycle)
    CycleManager.saveStartDate(requireContext(), selectedDate)

    when {
        themePastel.isChecked ->
            CycleThemeManager.saveTheme(requireContext(), CycleThemeManager.THEME_PASTEL)

        themeDark.isChecked ->
            CycleThemeManager.saveTheme(requireContext(), CycleThemeManager.THEME_DARK)

        else ->
            CycleThemeManager.saveTheme(requireContext(), CycleThemeManager.THEME_CLASSIC)
    }

    val selectedFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        normalizedCycle.firstOrNull() ?: "A"
    )

    firstCycleDayDropdown.setText(selectedFirstDay, false)
    cycleDaysEdit.setText(normalizedCycle.joinToString(", "))

    refreshFirstCycleDayDropdown(selectedFirstDay)

    val selectedText = holidayCountryDropdown.text?.toString()?.trim().orEmpty()

    val selectedCountryCode = supportedCountries
        .firstOrNull {
            val plainName = HolidayManager.getCountryDisplayName(requireContext(), it.code)
            val flaggedName = HolidayManager.getCountryDisplayNameWithFlag(requireContext(), it.code)
            val autoName = HolidayManager.getCountryDisplayNameWithAutoDetected(
                context = requireContext(),
                countryCode = it.code,
                isAutoDetected = true
            )

            selectedText == plainName ||
                    selectedText == flaggedName ||
                    selectedText == autoName
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

    HolidayManager.saveSelectedCountry(requireContext(), selectedCountryCode)
}

fun HomeFragment.validateAndBuildCycle(): List<String>? {
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

    if (cycle.size > HomeFragment.MAX_CYCLE_ITEMS) {
        showError(
            getString(
                R.string.error_cycle_too_many,
                cycle.size,
                HomeFragment.MAX_CYCLE_ITEMS
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

    val tooLongLabel = cycle.firstOrNull { it.length > HomeFragment.MAX_LABEL_LENGTH }
    if (tooLongLabel != null) {
        showError(
            getString(
                R.string.error_label_too_long_detailed,
                tooLongLabel,
                HomeFragment.MAX_LABEL_LENGTH
            )
        )
        return null
    }

    val selectedFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        cycle.firstOrNull() ?: "A"
    )

    if (selectedFirstDay.length > HomeFragment.MAX_LABEL_LENGTH) {
        showError(
            getString(
                R.string.error_label_too_long_detailed,
                selectedFirstDay,
                HomeFragment.MAX_LABEL_LENGTH
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
    if (skippedOverrideLabel.length > HomeFragment.MAX_LABEL_LENGTH) {
        showError(
            getString(
                R.string.error_label_too_long_detailed,
                skippedOverrideLabel,
                HomeFragment.MAX_LABEL_LENGTH
            )
        )
        return null
    }

    return cycle
}

fun HomeFragment.migrateLegacySettingsIfNeeded() {
    val cyclePrefs = requireContext().getSharedPreferences(CycleManager.PREFS_NAME, Context.MODE_PRIVATE)
    val hasCycle = cyclePrefs.contains(CycleManager.KEY_CYCLE_DAYS)
    val hasStartDate = cyclePrefs.contains(CycleManager.KEY_CYCLE_START_DATE)

    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

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

        CycleManager.saveCycle(requireContext(), listOf(labelA, labelB))
        CycleManager.saveStartDate(requireContext(), cycleStartDate)

        prefs.edit {
            putString(AppPrefs.KEY_FIRST_CYCLE_DAY, legacyFirstDay)
        }
    }

    if (!prefs.contains(AppPrefs.KEY_OVERRIDE_SKIPPED)) {
        prefs.edit {
            putBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
        }
    }

    HolidayManager.ensureCountrySelected(requireContext())
}

fun HomeFragment.setupChangeListeners() {
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

    firstCycleDayDropdown.setOnItemClickListener { _, _, _, _ ->
        clearDateCheckResult()
        markUnsavedChanges()
        updateTodayStatus()
        updateCyclePreview()
    }

    holidayCountryDropdown.setOnItemClickListener { _, _, _, _ ->
        clearDateCheckResult()
        markUnsavedChanges()
    }

    themeClassic.setOnClickListener { markUnsavedChanges() }
    themePastel.setOnClickListener { markUnsavedChanges() }
    themeDark.setOnClickListener { markUnsavedChanges() }

    prefixEdit.addTextChangedListener {
        markUnsavedChanges()
    }

    cycleDaysEdit.addTextChangedListener {
        clearDateCheckResult()
        refreshFirstCycleDayDropdown()
        validateCycleInput()
        updatePresetSelectionState(markAsChanged = true)
        updateTodayStatus()
        updateCyclePreview()
    }

    skippedDayLabelEdit.addTextChangedListener {
        clearDateCheckResult()
        markUnsavedChanges()
    }
}

fun HomeFragment.showError(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}