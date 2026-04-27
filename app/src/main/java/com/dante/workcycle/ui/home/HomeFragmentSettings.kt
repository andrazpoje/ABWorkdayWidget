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
import com.dante.workcycle.domain.template.TemplateManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import com.dante.workcycle.domain.schedule.CyclePresetProvider

fun HomeFragment.showUnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.unsaved_changes_title)
        .setMessage(R.string.unsaved_changes_message)
        .setPositiveButton(R.string.save) { _, _ -> onSave() }
        .setNegativeButton(R.string.discard) { _, _ -> onDiscard() }
        .setNeutralButton(R.string.cancel, null)
        .show()
}

fun HomeFragment.saveChangesAndRefresh(): Boolean {
    val validatedCycle = validateAndBuildCycle() ?: return false

    saveSettings(validatedCycle)
    refreshWidget()
    updateTodayStatus()
    updateCyclePreview()
    updateUpcomingEvents()
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
    val firstDayIndex: Int,
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

    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
    val currentFirstDayIndex = if (TemplateManager.isTemplateActive(requireContext())) {
        draftFirstCycleDayIndex ?: prefs.getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1)
    } else {
        -1
    }

    return HomeFormState(
        cycle = currentCycle,
        selectedDate = selectedDate,
        firstDay = currentFirstDay,
        firstDayIndex = currentFirstDayIndex,
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

    val savedFirstDayIndex = if (TemplateManager.isTemplateActive(requireContext())) {
        prefs.getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1)
    } else {
        -1
    }

    return HomeFormState(
        cycle = savedCycle,
        selectedDate = savedDate,
        firstDay = savedFirstDay,
        firstDayIndex = savedFirstDayIndex,
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

        val cycle = CycleManager.loadCycle(requireContext()).ifEmpty {
            listOf("Dopoldan", "Popoldan", "Nočna")
        }
        val cycleStartDate = CycleManager.loadStartDate(requireContext())

        selectedDate = cycleStartDate
        cycleDaysEdit.setText(cycle.joinToString(", "))

        val savedFirstDayRaw = prefs.getString(
            AppPrefs.KEY_FIRST_CYCLE_DAY,
            cycle.firstOrNull() ?: "Dopoldan"
        ) ?: (cycle.firstOrNull() ?: "Dopoldan")

        val savedFirstDay = sanitizeLabel(
            savedFirstDayRaw,
            cycle.firstOrNull() ?: "Dopoldan"
        )

        if (!TemplateManager.isTemplateActive(requireContext())) {
            prefs.edit { remove(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX) }
        }

        refreshFirstCycleDayDropdown(savedFirstDay)
        draftFirstCycleDayIndex = null

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

    val selectedFirstDay = sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        normalizedCycle.firstOrNull() ?: "A"
    )

    val newCountryCode = resolveSelectedCountryCodeFromUi()

    val savedState = buildSavedFormState()
    val selectedFirstDayIndex = if (TemplateManager.isTemplateActive(context)) {
        draftFirstCycleDayIndex
            ?: context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
                .getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1)
    } else {
        -1
    }

    val currentState = HomeFormState(
        cycle = normalizedCycle,
        selectedDate = selectedDate,
        firstDay = selectedFirstDay,
        firstDayIndex = selectedFirstDayIndex,
        skipSaturdays = switchSaturdays.isChecked,
        skipSundays = switchSundays.isChecked,
        skipHolidays = switchHolidays.isChecked,
        countryCode = newCountryCode
    )

    val manualCycleChanged = !isCycleLocked && (
            savedState.cycle != currentState.cycle
            )

    val manualRulesChanged = !isRulesLocked && (
            savedState.skipSaturdays != currentState.skipSaturdays ||
                    savedState.skipSundays != currentState.skipSundays ||
                    savedState.skipHolidays != currentState.skipHolidays ||
                    savedState.countryCode != currentState.countryCode
            )

    val shouldClearTemplate = TemplateManager.isTemplateActive(context) &&
            (manualCycleChanged || manualRulesChanged)

    if (shouldClearTemplate) {
        TemplateManager.clearTemplate(context)
    }

    if (canEditStartDate) {
        CycleManager.saveStartDate(requireContext(), selectedDate)
    }

    if (!isCycleLocked) {
        CycleManager.saveCycle(requireContext(), normalizedCycle)

        runWithoutChangeTracking {
            firstCycleDayDropdown.setText(selectedFirstDay, false)
            cycleDaysEdit.setText(normalizedCycle.joinToString(", "))
            refreshFirstCycleDayDropdown(selectedFirstDay)
        }
    }

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

        if (TemplateManager.isTemplateActive(context) && selectedFirstDayIndex >= 0) {
            putInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, selectedFirstDayIndex)
        } else {
            remove(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX)
        }
    }

    if (!isRulesLocked) {
        HolidayManager.saveSelectedCountry(requireContext(), newCountryCode)
    }

    updateTemplateUiState()
    updatePresetSelectionState()
    draftFirstCycleDayIndex = null
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

    val duplicatesAllowed = TemplateManager.isTemplateActive(requireContext())

    if (!duplicatesAllowed) {
        val duplicateExists = cycle
            .map { it.lowercase() }
            .toSet()
            .size != cycle.size

        if (duplicateExists) {
            showError(getString(R.string.error_cycle_duplicates))
            return null
        }
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
    val context = requireContext()
    val cyclePrefs = context.getSharedPreferences(CycleManager.PREFS_NAME, Context.MODE_PRIVATE)
    val hasCycle = cyclePrefs.contains(CycleManager.KEY_CYCLE_DAYS)
    val hasStartDate = cyclePrefs.contains(CycleManager.KEY_CYCLE_START_DATE)

    val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    if (!hasCycle || !hasStartDate) {
        val threeShiftPreset = CyclePresetProvider.getPresets().firstOrNull {
            it.id == "three_shift"
        }

        val defaultCycle = threeShiftPreset?.cycleDaysProvider?.invoke(context)
            ?: listOf(
                context.getString(R.string.label_morning),
                context.getString(R.string.label_afternoon),
                context.getString(R.string.label_night)
            )

        val defaultFirstDay = threeShiftPreset?.defaultFirstDayProvider?.invoke(context)
            ?: defaultCycle.first()

        val defaultStartDate = LocalDate.now()

        CycleManager.saveCycle(context, defaultCycle)
        CycleManager.saveStartDate(context, defaultStartDate)

        prefs.edit {
            putInt(AppPrefs.KEY_START_YEAR, defaultStartDate.year)
            putInt(AppPrefs.KEY_START_MONTH, defaultStartDate.monthValue)
            putInt(AppPrefs.KEY_START_DAY, defaultStartDate.dayOfMonth)
            putString(AppPrefs.KEY_FIRST_CYCLE_DAY, defaultFirstDay)
            remove(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX)
        }

        TemplateManager.clearTemplate(context)
    }

    if (!prefs.contains(AppPrefs.KEY_OVERRIDE_SKIPPED)) {
        prefs.edit {
            putBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
        }
    }

    HolidayManager.ensureCountrySelected(context)
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
