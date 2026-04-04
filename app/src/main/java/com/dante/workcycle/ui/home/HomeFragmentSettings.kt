package com.dante.workcycle.ui.home

import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.parseCycleInput
import com.dante.workcycle.domain.schedule.sanitizeLabel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import com.dante.workcycle.domain.template.TemplateManager

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

private data class HomeFormState(
    val cycle: List<String>,
    val selectedDate: LocalDate,
    val firstDay: String,
    val skipSaturdays: Boolean,
    val skipSundays: Boolean,
    val skipHolidays: Boolean,
    val countryCode: String
)

fun HomeFragment.updateUnsavedChangesState() {
    val currentState = buildCurrentFormState()
    val savedState = buildSavedFormState()

    if (currentState == savedState) {
        clearUnsavedChanges()
    } else {
        markUnsavedChanges()
    }
}

private fun HomeFragment.buildCurrentFormState(): HomeFormState {
    val currentCycle = parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty())
    val currentFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        currentCycle.firstOrNull() ?: "A"
    )

    return HomeFormState(
        cycle = currentCycle,
        selectedDate = selectedDate,
        firstDay = currentFirstDay,
        skipSaturdays = switchSaturdays.isChecked,
        skipSundays = switchSundays.isChecked,
        skipHolidays = switchHolidays.isChecked,
        countryCode = resolveSelectedCountryCodeFromUi()
    )
}

private fun HomeFragment.buildSavedFormState(): HomeFormState {
    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
    val savedCycle = CycleManager.loadCycle(requireContext()).ifEmpty { listOf("A", "B") }
    val savedDate = CycleManager.loadStartDate(requireContext())
    val savedFirstDay = sanitizeLabel(
        prefs.getString(AppPrefs.KEY_FIRST_CYCLE_DAY, savedCycle.firstOrNull() ?: "A").orEmpty(),
        savedCycle.firstOrNull() ?: "A"
    )

    return HomeFormState(
        cycle = savedCycle,
        selectedDate = savedDate,
        firstDay = savedFirstDay,
        skipSaturdays = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true),
        skipSundays = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true),
        skipHolidays = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true),
        countryCode = HolidayManager.getSelectedCountry(requireContext())
    )
}

private fun HomeFragment.resolveSelectedCountryCodeFromUi(): String {
    val selectedText = holidayCountryDropdown.text?.toString()?.trim().orEmpty()

    return supportedCountries
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
}

fun HomeFragment.loadSettings() {
    runWithoutChangeTracking {
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

        val selectedCountryCode = HolidayManager.getSelectedCountry(requireContext())
        val selectedCountry = supportedCountries.firstOrNull { it.code == selectedCountryCode }
            ?: supportedCountries.first()

        val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

        val displayText = HolidayManager.getCountryDisplayNameWithAutoDetected(
            context = requireContext(),
            countryCode = selectedCountry.code,
            isAutoDetected = !isManual &&
                    selectedCountry.code == HolidayManager.getSelectedCountry(requireContext())
        )

        holidayCountryDropdown.setText(displayText, false)

        updateDateText()
        updateTemplateUiState()
    }
}

fun HomeFragment.saveSettings(normalizedCycle: List<String>) {
    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    val context = requireContext()

    val isCycleLocked = TemplateManager.isCycleEditingLocked(context)
    val isRulesLocked = TemplateManager.isRulesEditingLocked(context)
    val canEditStartDate = TemplateManager.canEditStartDate(context)

    if (canEditStartDate) {
        CycleManager.saveStartDate(requireContext(), selectedDate)
    }

    val selectedFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        normalizedCycle.firstOrNull() ?: "A"
    )

    if (!isCycleLocked) {
        CycleManager.saveCycle(requireContext(), normalizedCycle)

        runWithoutChangeTracking {
            firstCycleDayDropdown.setText(selectedFirstDay, false)
            cycleDaysEdit.setText(normalizedCycle.joinToString(", "))
            refreshFirstCycleDayDropdown(selectedFirstDay)
        }
    }

    val selectedCountryCode = resolveSelectedCountryCodeFromUi()

    prefs.edit {
        if (canEditStartDate) {
            putInt(AppPrefs.KEY_START_YEAR, selectedDate.year)
            putInt(AppPrefs.KEY_START_MONTH, selectedDate.monthValue)
            putInt(AppPrefs.KEY_START_DAY, selectedDate.dayOfMonth)
        }

        if (!isCycleLocked) {
            putString(AppPrefs.KEY_FIRST_CYCLE_DAY, selectedFirstDay)
        }

        if (!isRulesLocked) {
            putBoolean(AppPrefs.KEY_SKIP_SATURDAYS, switchSaturdays.isChecked)
            putBoolean(AppPrefs.KEY_SKIP_SUNDAYS, switchSundays.isChecked)
            putBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, switchHolidays.isChecked)
        }
    }

    if (!isRulesLocked) {
        HolidayManager.saveSelectedCountry(requireContext(), selectedCountryCode)
    }
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

        val selectedLegacyDate = LocalDate.of(year, month, day)
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
        if (isInitializing) return@setOnCheckedChangeListener

        clearDateCheckResult()
        updateUnsavedChangesState()
    }

    switchSundays.setOnCheckedChangeListener { _, _ ->
        if (isInitializing) return@setOnCheckedChangeListener

        clearDateCheckResult()
        updateUnsavedChangesState()
    }

    switchHolidays.setOnCheckedChangeListener { _, _ ->
        if (isInitializing) return@setOnCheckedChangeListener

        clearDateCheckResult()
        updateUnsavedChangesState()
    }

    cycleDaysEdit.addTextChangedListener {
        if (isInitializing) return@addTextChangedListener

        clearDateCheckResult()
        refreshFirstCycleDayDropdown()
        validateCycleInput()
        updateUnsavedChangesState()
        updateTodayStatus()
        updateCyclePreview()
    }
}

fun HomeFragment.showError(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}