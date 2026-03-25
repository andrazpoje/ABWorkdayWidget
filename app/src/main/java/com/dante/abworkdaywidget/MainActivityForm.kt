package com.dante.abworkdaywidget

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Toast
import com.dante.abworkdaywidget.util.sanitizeLabel
import java.util.Locale
import android.widget.Filter
import android.widget.Filterable

fun MainActivity.validateCycleInput(): Boolean {
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

    if (parts.size > MainActivity.MAX_CYCLE_ITEMS) {
        cycleDaysInputLayout.error = getString(
            R.string.error_cycle_too_many,
            parts.size,
            MainActivity.MAX_CYCLE_ITEMS
        )
        return false
    }

    if (parts.any { it.length > MainActivity.MAX_LABEL_LENGTH }) {
        cycleDaysInputLayout.error = resources.getQuantityString(
            R.plurals.error_label_too_long,
            MainActivity.MAX_LABEL_LENGTH,
            MainActivity.MAX_LABEL_LENGTH
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

private fun MainActivity.createNoFilterAdapter(items: List<String>): ArrayAdapter<String> {
    return object : ArrayAdapter<String>(
        this,
        android.R.layout.simple_list_item_1,
        items
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
                    return resultValue as String
                }
            }
        }
    }
}

fun MainActivity.buildCountryDisplayList(
    detectedCode: String,
    isManual: Boolean
): List<String> {
    return supportedCountries.map {
        if (!isManual && it.code == detectedCode) {
            "${it.displayName} (auto-detected)"
        } else {
            it.displayName
        }
    }
}

fun parseCycleLabels(raw: String): List<String> {
    return raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { index, label ->
            sanitizeLabel(label, "Day ${index + 1}").take(MainActivity.MAX_LABEL_LENGTH)
        }
        .take(MainActivity.MAX_CYCLE_ITEMS)
}

fun MainActivity.getCurrentCycleLabelsFromInput(): List<String> {
    return parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty())
}

fun MainActivity.getCurrentCycleInputState(): Pair<List<String>, String> {
    val currentCycle = parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty())
    val currentFirstDay = firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
    return currentCycle to currentFirstDay
}

fun MainActivity.wouldPresetChangeCurrentState(preset: CyclePreset): Boolean {
    val (currentCycle, currentFirstDay) = getCurrentCycleInputState()

    val normalizedPresetCycle = preset.cycleDaysProvider(this).map {
        sanitizeLabel(it, "").take(MainActivity.MAX_LABEL_LENGTH)
    }

    val normalizedPresetFirstDay = sanitizeLabel(
        preset.defaultFirstDayProvider(this),
        normalizedPresetCycle.firstOrNull() ?: ""
    ).take(MainActivity.MAX_LABEL_LENGTH)

    return currentCycle != normalizedPresetCycle || currentFirstDay != normalizedPresetFirstDay
}

fun MainActivity.applyPreset(preset: CyclePreset) {
    val labels = preset.cycleDaysProvider(this)
    val firstDay = preset.defaultFirstDayProvider(this)

    cycleDaysEdit.setText(labels.joinToString(", "))
    refreshFirstCycleDayDropdown(firstDay)
    updatePresetSelectionState()
    clearDateCheckResult()
    validateCycleInput()
    markUnsavedChanges()

    Toast.makeText(
        this,
        getString(R.string.preset_applied, getString(preset.nameRes)),
        Toast.LENGTH_SHORT
    ).show()
}

fun MainActivity.showApplyPresetDialog(
    preset: CyclePreset,
    onConfirm: () -> Unit
) {
    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle(R.string.apply_preset_title)
        .setMessage(
            getString(
                R.string.apply_preset_message,
                getString(preset.nameRes)
            )
        )
        .setPositiveButton(R.string.apply_preset) { _: android.content.DialogInterface, _: Int ->
            onConfirm()
        }
        .setNegativeButton(R.string.cancel) { dialog: android.content.DialogInterface, _: Int ->
            dialog.dismiss()
        }
        .show()
}

fun MainActivity.refreshFirstCycleDayDropdown(preferredValue: String? = null) {
    val cycleLabels = getCurrentCycleLabelsFromInput()

    val adapter = createNoFilterAdapter(cycleLabels)
    firstCycleDayDropdown.setAdapter(adapter)

    if (cycleLabels.isEmpty()) {
        firstCycleDayDropdown.setText("", false)
        firstCycleDayDropdown.isEnabled = false
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
}

fun MainActivity.setupFirstCycleDayDropdown() {
    firstCycleDayDropdown.keyListener = null
    firstCycleDayDropdown.setOnClickListener {
        if (firstCycleDayDropdown.adapter != null && firstCycleDayDropdown.adapter.count > 0) {
            firstCycleDayDropdown.showDropDown()
        }
    }

    firstCycleDayDropdown.setOnItemClickListener { _, _, _, _ ->
        clearDateCheckResult()
        updatePresetSelectionState(markAsChanged = true)
    }
}

fun MainActivity.setupPresetDropdown() {
    val presets = CyclePresetProvider.getPresets()
    val names = presets.map { getString(it.nameRes) } + getString(R.string.preset_custom)

    val adapter = createNoFilterAdapter(names)

    presetDropdown.setAdapter(adapter)
    presetDropdown.keyListener = null

    presetDropdown.setOnClickListener {
        presetDropdown.showDropDown()
    }

    presetDropdown.setOnItemClickListener { _, _, _, _ ->
        val selectedName = presetDropdown.text?.toString()?.trim().orEmpty()
        val isCustom = selectedName == getString(R.string.preset_custom)
        applyPresetButton.isEnabled = !isCustom
        applyPresetButton.alpha = if (isCustom) 0.5f else 1f
    }
}

fun MainActivity.updatePresetSelectionState(markAsChanged: Boolean = false) {
    val matchedPreset = CyclePresetProvider.getPresets().firstOrNull { preset ->
        !wouldPresetChangeCurrentState(preset)
    }

    val presetText = matchedPreset
        ?.let { getString(it.nameRes) }
        ?: getString(R.string.preset_custom)

    if (presetDropdown.text?.toString() != presetText) {
        presetDropdown.setText(presetText, false)
    }

    applyPresetButton.isEnabled = matchedPreset == null
    applyPresetButton.alpha = if (matchedPreset == null) 1f else 0.5f

    if (markAsChanged) {
        markUnsavedChanges()
    }
}

fun MainActivity.setupHolidayCountryDropdown() {
    supportedCountries = HolidayManager.supportedCountries

    val prefs = getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
    val detectedCode = HolidayManager.getSelectedCountry(this)
    val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

    val displayItems = buildCountryDisplayList(detectedCode, isManual)

    val adapter = createNoFilterAdapter(displayItems)

    holidayCountryDropdown.setAdapter(adapter)
    holidayCountryDropdown.keyListener = null
    holidayCountryDropdown.setOnClickListener {
        holidayCountryDropdown.showDropDown()
    }
}