package com.dante.workcycle.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.dante.workcycle.R
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CyclePresetProvider
import com.dante.workcycle.domain.schedule.sanitizeLabel
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.children
import androidx.core.view.isVisible
import com.dante.workcycle.data.prefs.TemplatePrefs
import com.dante.workcycle.domain.schedule.CyclePreset
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.domain.template.ScheduleTemplateProvider

fun HomeFragment.validateCycleInput(): Boolean {
    val raw = cycleDaysEdit.text?.toString().orEmpty().trim()
    val parts = raw.split(",").map { it.trim() }

    if (raw.isBlank() || parts.all { it.isEmpty() }) {
        cycleDaysInputLayout.error = getString(R.string.error_cycle_empty)
        return false
    }

    if (parts.any { it.isEmpty() }) {
        cycleDaysInputLayout.error = getString(R.string.error_cycle_empty_item)
        return false
    }

    if (parts.size > HomeFragment.MAX_CYCLE_ITEMS) {
        cycleDaysInputLayout.error = getString(
            R.string.error_cycle_too_many,
            parts.size,
            HomeFragment.MAX_CYCLE_ITEMS
        )
        return false
    }

    if (parts.any { it.length > HomeFragment.MAX_LABEL_LENGTH }) {
        cycleDaysInputLayout.error = resources.getQuantityString(
            R.plurals.error_label_too_long,
            HomeFragment.MAX_LABEL_LENGTH,
            HomeFragment.MAX_LABEL_LENGTH
        )
        return false
    }

    val distinct = parts.map { it.lowercase(Locale.getDefault()) }.toSet()
    if (distinct.size != parts.size) {
        cycleDaysInputLayout.error = getString(R.string.error_cycle_duplicates)
        return false
    }

    cycleDaysInputLayout.error = null
    return true
}

private fun HomeFragment.createNoFilterAdapter(items: List<String>): ArrayAdapter<String> {
    return object : ArrayAdapter<String>(
        requireContext(),
        android.R.layout.simple_list_item_1,
        items.toMutableList()
    ), Filterable {

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    return FilterResults().apply {
                        values = items
                        count = items.size
                    }
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    clear()
                    addAll(items)
                    notifyDataSetChanged()
                }

                override fun convertResultToString(resultValue: Any?): CharSequence {
                    return resultValue as? String ?: ""
                }
            }
        }
    }
}
private fun HomeFragment.showClearTemplateDialog() {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.template_clear_dialog_title)
        .setMessage(R.string.template_clear_dialog_message)
        .setNegativeButton(android.R.string.cancel) { dialog, _ ->
            val activeTemplate = TemplateManager.getActiveTemplate(requireContext())
            val selectedText = activeTemplate?.let { getString(it.titleRes) }
                ?: getString(R.string.template_none)

            presetDropdown.setText(selectedText, false)
            dialog.dismiss()
        }
        .setPositiveButton(R.string.template_clear_dialog_confirm) { dialog, _ ->
            TemplateManager.clearTemplate(requireContext())

            runWithoutChangeTracking {
                loadSettings()
                updateTemplateUiState()
                clearUnsavedChanges()
            }

            Toast.makeText(
                requireContext(),
                getString(R.string.template_removed_message),
                Toast.LENGTH_SHORT
            ).show()

            updateTodayStatus()
            updateCyclePreview()
            dialog.dismiss()
        }
        .show()
}
private fun MaterialAutoCompleteTextView.showDropdownIfPossible() {
    val adapterCount = adapter?.count ?: 0
    if (adapterCount > 0) {
        post { showDropDown() }
    }
}

private fun HomeFragment.configureAsSelectBox(dropdown: MaterialAutoCompleteTextView) {
    dropdown.keyListener = null
    dropdown.isCursorVisible = false
    dropdown.threshold = 0

    dropdown.setOnClickListener {
        dropdown.showDropdownIfPossible()
    }

    dropdown.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            dropdown.showDropdownIfPossible()
        }
    }
}

fun HomeFragment.buildCountryDisplayList(
    detectedCode: String,
    isManual: Boolean
): List<String> {
    return supportedCountries.map {
        HolidayManager.getCountryDisplayNameWithAutoDetected(
            context = requireContext(),
            countryCode = it.code,
            isAutoDetected = !isManual && it.code == detectedCode
        )
    }
}

fun parseCycleLabels(
    raw: String,
    fallbackLabelProvider: (Int) -> String = { index -> "Day $index" }
): List<String> {
    return raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { index, label ->
            sanitizeLabel(
                label,
                fallbackLabelProvider(index + 1)
            ).take(HomeFragment.MAX_LABEL_LENGTH)
        }
        .take(HomeFragment.MAX_CYCLE_ITEMS)
}

fun HomeFragment.getCurrentCycleLabelsFromInput(): List<String> {
    return parseCycleLabels(
        cycleDaysEdit.text?.toString().orEmpty()
    ) { dayNumber ->
        getString(R.string.cycle_day_fallback, dayNumber)
    }
}

fun HomeFragment.getCurrentCycleInputState(): Pair<List<String>, String> {
    val currentCycle = parseCycleLabels(
        cycleDaysEdit.text?.toString().orEmpty()
    ) { dayNumber ->
        getString(R.string.cycle_day_fallback, dayNumber)
    }
    val currentFirstDay = firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
    return currentCycle to currentFirstDay
}

fun HomeFragment.wouldPresetChangeCurrentState(preset: CyclePreset): Boolean {
    val (currentCycle, currentFirstDay) = getCurrentCycleInputState()

    val normalizedPresetCycle = preset.cycleDaysProvider(requireContext()).map {
        sanitizeLabel(it, "").take(HomeFragment.MAX_LABEL_LENGTH)
    }

    val normalizedPresetFirstDay = sanitizeLabel(
        preset.defaultFirstDayProvider(requireContext()),
        normalizedPresetCycle.firstOrNull() ?: ""
    ).take(HomeFragment.MAX_LABEL_LENGTH)

    return currentCycle != normalizedPresetCycle || currentFirstDay != normalizedPresetFirstDay
}

fun HomeFragment.applyPreset(preset: CyclePreset) {
    val labels = preset.cycleDaysProvider(requireContext())
    val firstDay = preset.defaultFirstDayProvider(requireContext())

    cycleDaysEdit.setText(labels.joinToString(", "))
    refreshFirstCycleDayDropdown(firstDay)
    clearDateCheckResult()
    validateCycleInput()
    updatePresetSelectionState(markAsChanged = true)
    updateTodayStatus()
    updateCyclePreview()

    Toast.makeText(
        requireContext(),
        getString(R.string.preset_applied, getString(preset.nameRes)),
        Toast.LENGTH_SHORT
    ).show()
}

private fun HomeFragment.getChipColorForLabel(
    label: String,
    cycleLabels: List<String>
): Int {
    return try {
        CycleColorHelper.getBackgroundColor(requireContext(), label, cycleLabels)
    } catch (_: Exception) {
        ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
    }
}

private fun HomeFragment.renderFirstCycleDayChips(
    cycleLabels: List<String>,
    selectedValue: String
) {
    firstCycleDayChipGroup.removeAllViews()

    if (cycleLabels.isEmpty()) {
        firstCycleDayChipGroup.visibility = View.GONE
        return
    }

    firstCycleDayChipGroup.visibility = View.VISIBLE

    cycleLabels.forEach { label ->
        val fillColor = getChipColorForLabel(label, cycleLabels)
        val textColor = CycleColorHelper.getTextColorForBackground(fillColor)
        val unselectedBackground = ColorUtils.setAlphaComponent(fillColor, 40)
        val unselectedStroke = ColorUtils.setAlphaComponent(fillColor, 120)

        val backgroundStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                fillColor,
                unselectedBackground
            )
        )

        val textStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                textColor,
                fillColor
            )
        )

        val strokeStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                fillColor,
                unselectedStroke
            )
        )

        val chip = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = label
            isCheckable = true
            isClickable = true
            isCheckedIconVisible = false
            setEnsureMinTouchTargetSize(false)
            isChecked = label == selectedValue

            chipBackgroundColor = backgroundStates
            setTextColor(textStates)
            chipStrokeColor = strokeStates
            chipStrokeWidth = resources.displayMetrics.density * 1f

            setOnClickListener {
                firstCycleDayDropdown.setText(label, false)
                clearDateCheckResult()
                updatePresetSelectionState(markAsChanged = true)
                updateTodayStatus()
                updateCyclePreview()
            }
        }

        firstCycleDayChipGroup.addView(chip)
    }
}

fun HomeFragment.refreshFirstCycleDayDropdown(preferredValue: String? = null) {
    val cycleLabels = getCurrentCycleLabelsFromInput()

    val adapter = createNoFilterAdapter(cycleLabels)
    firstCycleDayDropdown.setAdapter(adapter)

    if (cycleLabels.isEmpty()) {
        firstCycleDayDropdown.setText("", false)
        firstCycleDayDropdown.isEnabled = false
        renderFirstCycleDayChips(emptyList(), "")
        return
    }

    firstCycleDayDropdown.isEnabled = true

    val currentValue = preferredValue
        ?: firstCycleDayDropdown.text?.toString()?.trim().orEmpty()

    val finalValue = when {
        currentValue in cycleLabels -> currentValue
        else -> cycleLabels.first()
    }

    firstCycleDayDropdown.setText(finalValue, false)
    renderFirstCycleDayChips(cycleLabels, finalValue)
}

fun HomeFragment.setupFirstCycleDayDropdown() {
    configureAsSelectBox(firstCycleDayDropdown)

    firstCycleDayDropdown.setOnItemClickListener { _, _, _, _ ->
        val selected = firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
        refreshFirstCycleDayDropdown(selected)
        clearDateCheckResult()
        updateUnsavedChangesState()
        updateTodayStatus()
        updateCyclePreview()
    }
}

fun HomeFragment.setupPresetDropdown() {
    val templateItems = buildList {
        add(getString(R.string.template_none))
        addAll(
            ScheduleTemplateProvider.getAll().map { template ->
                getString(template.titleRes)
            }
        )
    }

    val adapter = createNoFilterAdapter(templateItems)

    presetDropdown.setAdapter(adapter)
    configureAsSelectBox(presetDropdown)

    val activeTemplate = TemplateManager.getActiveTemplate(requireContext())
    val selectedText = activeTemplate?.let { getString(it.titleRes) }
        ?: getString(R.string.template_none)

    presetDropdown.setText(selectedText, false)

    presetDropdown.setOnItemClickListener { _, _, position, _ ->
        val selectedTextFromList = templateItems[position]

        if (selectedTextFromList == getString(R.string.template_none)) {
            if (TemplateManager.isTemplateActive(requireContext())) {
                showClearTemplateDialog()
            } else {
                presetDropdown.setText(getString(R.string.template_none), false)
            }
        } else {
            val selectedTemplate = ScheduleTemplateProvider.getAll().firstOrNull {
                getString(it.titleRes) == selectedTextFromList
            } ?: return@setOnItemClickListener

            showApplyTemplateDialog(
                templateId = selectedTemplate.id,
                templateTitle = getString(selectedTemplate.titleRes),
                templateDescription = getString(selectedTemplate.descriptionRes)
            )
        }
    }
}

fun HomeFragment.updatePresetSelectionState(markAsChanged: Boolean = false) {
    val matchedPreset = CyclePresetProvider.getPresets().firstOrNull { preset ->
        !wouldPresetChangeCurrentState(preset)
    }

    val presetText = matchedPreset
        ?.let { getString(it.nameRes) }
        ?: getString(R.string.preset_custom)

    if (presetDropdown.text?.toString() != presetText) {
        presetDropdown.setText(presetText, false)
    }

    if (markAsChanged) {
        updateUnsavedChangesState()
    }
}

fun HomeFragment.setupHolidayCountryDropdown() {
    supportedCountries = HolidayManager.supportedCountries

    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
    val detectedCode = HolidayManager.getSelectedCountry(requireContext())
    val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

    val displayItems = buildCountryDisplayList(detectedCode, isManual)

    val adapter = createNoFilterAdapter(displayItems)

    holidayCountryDropdown.setAdapter(adapter)
    configureAsSelectBox(holidayCountryDropdown)

    holidayCountryDropdown.setOnItemClickListener { _, _, _, _ ->
        clearDateCheckResult()
        updateUnsavedChangesState()
    }
}

private fun View.setEnabledWithAlpha(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1f else 0.5f
}

private fun HomeFragment.setChipGroupEnabled(enabled: Boolean) {
    firstCycleDayChipGroup.isEnabled = enabled
    firstCycleDayChipGroup.alpha = if (enabled) 1f else 0.5f

    firstCycleDayChipGroup.children.forEach { chip ->
        chip.isEnabled = enabled
        chip.alpha = if (enabled) 1f else 0.6f
    }
}

private fun HomeFragment.buildTemplateDescriptionText(
    template: com.dante.workcycle.domain.template.ScheduleTemplate
): String {
    val dateFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(
        java.time.format.FormatStyle.MEDIUM
    ).withLocale(java.util.Locale.getDefault())

    val lockedItems = mutableListOf<String>()

    if (template.locksCycleEditing) {
        lockedItems += getString(R.string.template_locked_item_cycle)
    }

    if (template.locksRulesEditing) {
        lockedItems += getString(R.string.template_locked_item_rules)
    }

    if (!template.allowsStartDateEditing) {
        lockedItems += getString(R.string.template_locked_item_start_date)
    }

    if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) {
        lockedItems += getString(R.string.template_locked_item_assignment_mode)
    }

    val lockedSummary = if (lockedItems.isNotEmpty()) {
        getString(
            R.string.template_locked_summary_format,
            lockedItems.joinToString(", ")
        )
    } else {
        ""
    }

    val parts = listOfNotNull(
        getString(template.descriptionRes),
        getString(
            R.string.template_reference_date_format,
            template.fixedStartDate.format(dateFormatter),
            template.fixedFirstCycleDay
        ),
        lockedSummary.takeIf { it.isNotBlank() }
    )

    return parts.joinToString("\n\n")
}
fun HomeFragment.updateTemplateUiState() {
    val template = TemplateManager.getActiveTemplate(requireContext())
    val hasTemplate = template != null

    activeTemplateCard.isVisible = hasTemplate
    templateLockedMessage.isVisible = hasTemplate

    if (template != null) {
        activeTemplateTitle.text =
            getString(R.string.template_active_title) + ": " + getString(template.titleRes)

        activeTemplateDescription.text = buildTemplateDescriptionText(template)
        presetDropdown.setText(getString(template.titleRes), false)
    } else {
        activeTemplateTitle.text = getString(R.string.template_active_title)
        activeTemplateDescription.text = ""
        presetDropdown.setText(getString(R.string.template_none), false)
    }

    val cycleLocked = TemplateManager.isCycleEditingLocked(requireContext())
    val rulesLocked = TemplateManager.isRulesEditingLocked(requireContext())
    val canEditStartDate = TemplateManager.canEditStartDate(requireContext())

    cycleDaysEdit.setEnabledWithAlpha(!cycleLocked)
    firstCycleDayDropdown.setEnabledWithAlpha(!cycleLocked)
    setChipGroupEnabled(!cycleLocked)

    pickDateButton.setEnabledWithAlpha(canEditStartDate)

    switchSaturdays.setEnabledWithAlpha(!rulesLocked)
    switchSundays.setEnabledWithAlpha(!rulesLocked)
    switchHolidays.setEnabledWithAlpha(!rulesLocked)
    holidayCountryDropdown.setEnabledWithAlpha(!rulesLocked)
}

private fun HomeFragment.showApplyTemplateDialog(
    templateId: String,
    templateTitle: String,
    templateDescription: String
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(templateTitle)
        .setMessage(
            templateDescription + "\n\n" +
                    getString(R.string.template_apply_confirm_message)
        )
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.apply) { _, _ ->
            TemplateManager.applyTemplate(requireContext(), templateId)

            runWithoutChangeTracking {
                loadSettings()
                updateTemplateUiState()
                clearUnsavedChanges()
            }

            updateTodayStatus()
            updateCyclePreview()
        }
        .show()
}