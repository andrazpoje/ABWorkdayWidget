package com.dante.workcycle.ui.settings

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.dante.workcycle.R
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.domain.holiday.HolidayCountry
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.CycleOverrideRepository
import com.dante.workcycle.domain.schedule.CyclePreset
import com.dante.workcycle.domain.schedule.CyclePresetProvider
import com.dante.workcycle.domain.schedule.parseCycleInput
import com.dante.workcycle.domain.schedule.sanitizeLabel
import com.dante.workcycle.domain.template.ScheduleTemplate
import com.dante.workcycle.domain.template.ScheduleTemplateProvider
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.ui.template.TemplatePickerBottomSheet
import com.dante.workcycle.widget.WidgetRefreshHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class PrimaryCycleSettingsController(
    private val fragment: Fragment,
    root: View,
    private val onSaved: () -> Unit
) {

    companion object {
        const val ARG_SCROLL_TO_CYCLE_SETTINGS = "scrollToCycleSettings"
        private const val SECTION_CYCLE = "settings_cycle"
        private const val SECTION_RULES = "settings_rules"
        private const val MAX_CYCLE_ITEMS = 16
        private const val MAX_LABEL_LENGTH = 24
    }

    private val context: Context get() = fragment.requireContext()

    private val cycleHeader: View = root.findViewById(R.id.cycleHeader)
    private val rulesHeader: View = root.findViewById(R.id.rulesHeader)
    private val cycleSection: View = root.findViewById(R.id.cycleSection)
    private val rulesSection: View = root.findViewById(R.id.rulesSection)
    private val cycleArrow: ImageView = root.findViewById(R.id.cycleArrow)
    private val rulesArrow: ImageView = root.findViewById(R.id.rulesArrow)
    private val activeTemplateCard: View = root.findViewById(R.id.activeTemplateCard)
    private val activeTemplateTitle: TextView = root.findViewById(R.id.activeTemplateTitle)
    private val activeTemplateDescription: TextView = root.findViewById(R.id.activeTemplateDescription)
    private val templateLockedMessage: TextView = root.findViewById(R.id.templateLockedMessage)
    private val presetInputLayout: TextInputLayout = root.findViewById(R.id.presetInputLayout)
    private val presetDropdown: MaterialAutoCompleteTextView = root.findViewById(R.id.presetDropdown)
    private val pickDateButton: MaterialButton = root.findViewById(R.id.pickDateButton)
    private val dateText: TextView = root.findViewById(R.id.dateText)
    private val cycleDaysInputLayout: TextInputLayout = root.findViewById(R.id.cycleDaysInputLayout)
    private val cycleDaysEdit: EditText = root.findViewById(R.id.cycleDaysEdit)
    private val firstCycleDayChipGroup: ChipGroup = root.findViewById(R.id.firstCycleDayChipGroup)
    private val firstCycleDayDropdown: MaterialAutoCompleteTextView = root.findViewById(R.id.firstCycleDayDropdown)
    private val switchSaturdays: SwitchMaterial = root.findViewById(R.id.switchSaturdays)
    private val switchSundays: SwitchMaterial = root.findViewById(R.id.switchSundays)
    private val switchHolidays: SwitchMaterial = root.findViewById(R.id.switchHolidays)
    private val holidayCountryDropdown: MaterialAutoCompleteTextView = root.findViewById(R.id.holidayCountryDropdown)
    private val actions: View = root.findViewById(R.id.cycleSettingsActions)
    private val saveButton: MaterialButton = root.findViewById(R.id.cycleSettingsSaveButton)
    private val revertButton: MaterialButton = root.findViewById(R.id.cycleSettingsRevertButton)

    private var hasUnsavedChanges = false
    private var isInitializing = false
    private var selectedDate: LocalDate = LocalDate.now()
    private var draftFirstCycleDayIndex: Int? = null
    private lateinit var supportedCountries: List<HolidayCountry>

    private val sectionPrefs by lazy {
        context.getSharedPreferences("settings_cycle_sections", Context.MODE_PRIVATE)
    }

    fun initialize(expandCycleSection: Boolean) {
        isInitializing = true
        supportedCountries = HolidayManager.supportedCountries

        setupFirstCycleDayDropdown()
        setupPresetDropdown()
        setupHolidayCountryDropdown()
        setupSection(cycleHeader, cycleSection, cycleArrow, SECTION_CYCLE, expandCycleSection)
        setupSection(rulesHeader, rulesSection, rulesArrow, SECTION_RULES, false)
        loadSettings()
        setupChangeListeners()
        clearUnsavedChanges()

        isInitializing = false
    }

    private fun setupSection(
        header: View,
        content: View,
        arrow: ImageView,
        key: String,
        defaultExpanded: Boolean
    ) {
        val savedExpanded = sectionPrefs.getBoolean(key, defaultExpanded)
        setSectionExpanded(content, arrow, savedExpanded)

        header.setOnClickListener {
            val expanded = !content.isVisible
            setSectionExpanded(content, arrow, expanded)
            sectionPrefs.edit { putBoolean(key, expanded) }
        }
    }

    private fun setSectionExpanded(content: View, arrow: ImageView, expanded: Boolean) {
        content.isVisible = expanded
        arrow.rotation = if (expanded) 180f else 0f
    }

    private fun setupChangeListeners() {
        switchSaturdays.setOnCheckedChangeListener { _, _ ->
            if (!isInitializing) updateUnsavedChangesState()
        }

        switchSundays.setOnCheckedChangeListener { _, _ ->
            if (!isInitializing) updateUnsavedChangesState()
        }

        switchHolidays.setOnCheckedChangeListener { _, _ ->
            if (!isInitializing) updateUnsavedChangesState()
        }

        cycleDaysEdit.addTextChangedListener {
            if (isInitializing) return@addTextChangedListener
            refreshFirstCycleDayDropdown()
            validateCycleInput()
            updateUnsavedChangesState()
        }

        pickDateButton.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            if (!hasUnsavedChanges) return@setOnClickListener
            if (!validateCycleInput()) {
                Toast.makeText(context, fragment.getString(R.string.fix_errors), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveChanges()
        }

        revertButton.setOnClickListener {
            loadSettings()
            clearUnsavedChanges()
            Toast.makeText(context, fragment.getString(R.string.reverted), Toast.LENGTH_SHORT).show()
        }

        cycleDaysInputLayout.setOnClickListener {
            if (TemplateManager.isCycleEditingLocked(context)) showTemplateLockedMessage()
        }

        holidayCountryDropdown.setOnClickListener {
            if (TemplateManager.isRulesEditingLocked(context)) showTemplateLockedMessage()
        }
    }

    private fun saveChanges(): Boolean {
        val validatedCycle = validateAndBuildCycle() ?: return false

        saveSettings(validatedCycle)
        WidgetRefreshHelper.refresh(context)
        clearUnsavedChanges()
        onSaved()

        Toast.makeText(context, fragment.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        return true
    }

    private fun loadSettings() {
        runWithoutChangeTracking {
            val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            val cycle = CycleManager.loadCycle(context).ifEmpty {
                listOf(
                    fragment.getString(R.string.label_morning),
                    fragment.getString(R.string.label_afternoon),
                    fragment.getString(R.string.label_night)
                )
            }
            val cycleStartDate = CycleManager.loadStartDate(context)

            selectedDate = cycleStartDate
            cycleDaysEdit.setText(cycle.joinToString(", "))

            val savedFirstDayRaw = prefs.getString(
                AppPrefs.KEY_FIRST_CYCLE_DAY,
                cycle.firstOrNull() ?: fragment.getString(R.string.label_morning)
            ) ?: (cycle.firstOrNull() ?: fragment.getString(R.string.label_morning))

            val savedFirstDay = sanitizeLabel(
                savedFirstDayRaw,
                cycle.firstOrNull() ?: fragment.getString(R.string.label_morning)
            )

            if (!TemplateManager.isTemplateActive(context)) {
                prefs.edit { remove(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX) }
            }

            refreshFirstCycleDayDropdown(savedFirstDay)
            draftFirstCycleDayIndex = null

            switchSaturdays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
            switchSundays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
            switchHolidays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)

            val selectedCountryCode = HolidayManager.getSelectedCountry(context)
            val selectedCountry = supportedCountries.firstOrNull { it.code == selectedCountryCode }
                ?: supportedCountries.first()
            val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

            holidayCountryDropdown.setText(
                HolidayManager.getCountryDisplayNameWithAutoDetected(
                    context = context,
                    countryCode = selectedCountry.code,
                    isAutoDetected = !isManual && selectedCountry.code == selectedCountryCode
                ),
                false
            )

            updateDateText()
            updateTemplateUiState()
        }
    }

    private fun saveSettings(normalizedCycle: List<String>) {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
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
            draftFirstCycleDayIndex ?: prefs.getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1)
        } else {
            -1
        }

        val currentState = PrimaryCycleFormState(
            cycle = normalizedCycle,
            selectedDate = selectedDate,
            firstDay = selectedFirstDay,
            firstDayIndex = selectedFirstDayIndex,
            skipSaturdays = switchSaturdays.isChecked,
            skipSundays = switchSundays.isChecked,
            skipHolidays = switchHolidays.isChecked,
            countryCode = newCountryCode
        )

        val manualCycleChanged = !isCycleLocked && savedState.cycle != currentState.cycle
        val manualRulesChanged = !isRulesLocked && (
            savedState.skipSaturdays != currentState.skipSaturdays ||
                savedState.skipSundays != currentState.skipSundays ||
                savedState.skipHolidays != currentState.skipHolidays ||
                savedState.countryCode != currentState.countryCode
            )

        if (TemplateManager.isTemplateActive(context) && (manualCycleChanged || manualRulesChanged)) {
            TemplateManager.clearTemplate(context)
        }

        if (canEditStartDate) {
            CycleManager.saveStartDate(context, selectedDate)
        }

        if (!isCycleLocked) {
            CycleManager.saveCycle(context, normalizedCycle)
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
            HolidayManager.saveSelectedCountry(context, newCountryCode)
        }

        updateTemplateUiState()
        updatePresetSelectionState()
        draftFirstCycleDayIndex = null
    }

    private fun validateAndBuildCycle(): List<String>? {
        val rawInput = cycleDaysEdit.text.toString().trim()

        if (rawInput.isBlank()) {
            showError(fragment.getString(R.string.error_cycle_empty))
            return null
        }
        if (rawInput.contains(",,") || rawInput.startsWith(",") || rawInput.endsWith(",")) {
            showError(fragment.getString(R.string.error_cycle_invalid_format_detailed, rawInput))
            return null
        }

        val cycle = parseCycleInput(rawInput)
        if (cycle.isEmpty()) {
            showError(fragment.getString(R.string.error_cycle_empty))
            return null
        }
        if (cycle.size > MAX_CYCLE_ITEMS) {
            showError(fragment.getString(R.string.error_cycle_too_many, cycle.size, MAX_CYCLE_ITEMS))
            return null
        }

        if (!TemplateManager.isTemplateActive(context)) {
            val duplicateExists = cycle.map { it.lowercase() }.toSet().size != cycle.size
            if (duplicateExists) {
                showError(fragment.getString(R.string.error_cycle_duplicates))
                return null
            }
        }

        val tooLongLabel = cycle.firstOrNull { it.length > MAX_LABEL_LENGTH }
        if (tooLongLabel != null) {
            showError(fragment.getString(R.string.error_label_too_long_detailed, tooLongLabel, MAX_LABEL_LENGTH))
            return null
        }

        val selectedFirstDay = sanitizeLabel(
            firstCycleDayDropdown.text?.toString().orEmpty(),
            cycle.firstOrNull() ?: "A"
        )
        if (!cycle.any { it.equals(selectedFirstDay, ignoreCase = true) }) {
            showError(fragment.getString(R.string.error_first_day_not_in_cycle, selectedFirstDay))
            return null
        }

        return cycle
    }

    private fun validateCycleInput(): Boolean {
        val raw = cycleDaysEdit.text?.toString().orEmpty().trim()
        val parts = raw.split(",").map { it.trim() }

        cycleDaysInputLayout.error = when {
            raw.isBlank() || parts.all { it.isEmpty() } -> fragment.getString(R.string.error_cycle_empty)
            parts.any { it.isEmpty() } -> fragment.getString(R.string.error_cycle_empty_item)
            parts.size > MAX_CYCLE_ITEMS -> fragment.getString(R.string.error_cycle_too_many, parts.size, MAX_CYCLE_ITEMS)
            parts.any { it.length > MAX_LABEL_LENGTH } -> fragment.resources.getQuantityString(
                R.plurals.error_label_too_long,
                MAX_LABEL_LENGTH,
                MAX_LABEL_LENGTH
            )
            !TemplateManager.isTemplateActive(context) &&
                parts.map { it.lowercase(Locale.getDefault()) }.toSet().size != parts.size ->
                fragment.getString(R.string.error_cycle_duplicates)
            else -> null
        }

        return cycleDaysInputLayout.error == null
    }

    private fun setupFirstCycleDayDropdown() {
        configureAsSelectBox(firstCycleDayDropdown)
        firstCycleDayDropdown.setOnItemClickListener { _, _, _, _ ->
            val selected = firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
            refreshFirstCycleDayDropdown(selected)
            updateUnsavedChangesState()
        }
    }

    private fun refreshFirstCycleDayDropdown(preferredValue: String? = null) {
        val cycleLabels = getCurrentCycleLabelsFromInput()
        firstCycleDayDropdown.setAdapter(createNoFilterAdapter(cycleLabels))

        if (cycleLabels.isEmpty()) {
            firstCycleDayDropdown.setText("", false)
            firstCycleDayDropdown.isEnabled = false
            renderFirstCycleDayChips(emptyList(), "")
            return
        }

        firstCycleDayDropdown.isEnabled = true
        val currentValue = preferredValue ?: firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
        val finalValue = currentValue.takeIf { it in cycleLabels } ?: cycleLabels.first()
        firstCycleDayDropdown.setText(finalValue, false)
        renderFirstCycleDayChips(cycleLabels, finalValue)
    }

    private fun renderFirstCycleDayChips(cycleLabels: List<String>, selectedValue: String) {
        firstCycleDayChipGroup.removeAllViews()
        if (cycleLabels.isEmpty()) {
            firstCycleDayChipGroup.visibility = View.GONE
            return
        }

        firstCycleDayChipGroup.visibility = View.VISIBLE
        val isTemplateActive = TemplateManager.isTemplateActive(context)
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val savedIndex = if (isTemplateActive) prefs.getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1) else -1
        val selectedIndex = draftFirstCycleDayIndex ?: savedIndex
        val counters = mutableMapOf<String, Int>()
        val totals = cycleLabels.groupingBy { it }.eachCount()

        cycleLabels.forEachIndexed { index, label ->
            val displayText = if (isTemplateActive) {
                val count = (counters[label] ?: 0) + 1
                counters[label] = count
                "$label $count/${totals[label] ?: 1}"
            } else {
                label
            }

            val fillColor = getChipColorForLabel(label, cycleLabels)
            val textColor = CycleColorHelper.getTextColorForBackground(fillColor)
            val backgroundStates = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(fillColor, ColorUtils.setAlphaComponent(fillColor, 40))
            )
            val textStates = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(textColor, fillColor)
            )
            val strokeStates = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(fillColor, ColorUtils.setAlphaComponent(fillColor, 120))
            )
            val isSelected = if (isTemplateActive && selectedIndex in cycleLabels.indices) {
                index == selectedIndex
            } else {
                label == selectedValue
            }

            val chip = Chip(context).apply {
                id = View.generateViewId()
                text = displayText
                isCheckable = true
                isClickable = true
                isCheckedIconVisible = false
                setEnsureMinTouchTargetSize(false)
                isChecked = isSelected
                chipBackgroundColor = backgroundStates
                setTextColor(textStates)
                chipStrokeColor = strokeStates
                chipStrokeWidth = fragment.resources.displayMetrics.density

                setOnClickListener {
                    draftFirstCycleDayIndex = index
                    firstCycleDayDropdown.setText(label, false)
                    cycleDaysInputLayout.error = null
                    updateUnsavedChangesState()
                    renderFirstCycleDayChips(cycleLabels, label)
                }
            }
            firstCycleDayChipGroup.addView(chip)
        }
    }

    private fun setupPresetDropdown() {
        presetDropdown.setAdapter(null)
        presetDropdown.keyListener = null
        presetDropdown.isCursorVisible = false
        presetDropdown.threshold = Int.MAX_VALUE
        presetDropdown.setText(resolvePresetDisplayNameForCurrentState(), false)

        presetDropdown.setOnClickListener { showTemplatePickerBottomSheet() }
        presetInputLayout.setEndIconOnClickListener { showTemplatePickerBottomSheet() }
        presetDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showTemplatePickerBottomSheet()
        }
    }

    private fun showTemplatePickerBottomSheet() {
        if (!fragment.isAdded || fragment.parentFragmentManager.isStateSaved) return
        if (fragment.parentFragmentManager.findFragmentByTag("templatePicker") != null) return

        TemplatePickerBottomSheet(
            sections = buildTemplatePickerSections(),
            selectedTemplateId = TemplateManager.getActiveTemplate(context)?.id,
            onTemplateSelected = { templateId ->
                val template = ScheduleTemplateProvider.getById(templateId)
                if (template != null) {
                    showApplyTemplateDialog(
                        templateId = template.id,
                        templateTitle = fragment.getString(template.titleRes),
                        templateDescription = fragment.getString(template.descriptionRes)
                    )
                }
            }
        ).show(fragment.parentFragmentManager, "templatePicker")
    }

    private fun buildTemplatePickerSections(): List<TemplatePickerBottomSheet.Section> {
        return listOf(
            TemplatePickerBottomSheet.Section(
                title = shortTemplateGroupTitle(fragment.getString(R.string.template_group_general)),
                items = ScheduleTemplateProvider.getGeneralTemplates().map(::toTemplatePickerItem)
            ),
            TemplatePickerBottomSheet.Section(
                title = shortTemplateGroupTitle(fragment.getString(R.string.template_group_special)),
                items = ScheduleTemplateProvider.getSpecialTemplates().map(::toTemplatePickerItem)
            )
        )
    }

    private fun toTemplatePickerItem(template: ScheduleTemplate): TemplatePickerBottomSheet.Item {
        return TemplatePickerBottomSheet.Item(
            templateId = template.id,
            title = fragment.getString(template.getPickerTitleRes()),
            description = fragment.getString(template.getPickerDescriptionRes())
        )
    }

    private fun showApplyTemplateDialog(
        templateId: String,
        templateTitle: String,
        templateDescription: String
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(templateTitle)
            .setMessage(
                fragment.getString(
                    R.string.template_apply_message_format,
                    templateDescription,
                    fragment.getString(R.string.template_apply_confirm_message)
                )
            )
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                presetDropdown.setText(TemplateManager.getCurrentTemplateDisplayName(context), false)
                dialog.dismiss()
            }
            .setPositiveButton(R.string.apply) { _, _ ->
                applyTemplateWithOverrideCheck(templateId)
            }
            .show()
    }

    private fun applyTemplateWithOverrideCheck(templateId: String) {
        val repository = CycleOverrideRepository(context)
        val applyTemplateNow = {
            TemplateManager.applyTemplate(context, templateId)
            repository.clearAllOverrides()
            loadSettings()
            clearUnsavedChanges()
            WidgetRefreshHelper.refresh(context)
            onSaved()
        }

        if (!repository.hasOverrides()) {
            applyTemplateNow()
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.cycle_override_reset_dialog_title)
            .setMessage(R.string.cycle_override_reset_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.apply) { _, _ -> applyTemplateNow() }
            .show()
    }

    private fun setupHolidayCountryDropdown() {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val detectedCode = HolidayManager.getSelectedCountry(context)
        val isManual = prefs.getBoolean(AppPrefs.KEY_COUNTRY_MANUAL, false)

        holidayCountryDropdown.setAdapter(createNoFilterAdapter(buildCountryDisplayList(detectedCode, isManual)))
        configureAsSelectBox(holidayCountryDropdown)
        holidayCountryDropdown.setOnItemClickListener { _, _, _, _ ->
            updateUnsavedChangesState()
        }
    }

    private fun updateTemplateUiState() {
        val template = TemplateManager.getActiveTemplate(context)
        val hasTemplate = template != null

        activeTemplateCard.isVisible = hasTemplate
        templateLockedMessage.isVisible = hasTemplate

        if (template != null) {
            activeTemplateTitle.text = fragment.getString(
                R.string.template_active_title_format,
                fragment.getString(template.titleRes)
            )
            activeTemplateDescription.text = buildTemplateDescriptionText(template)
            presetDropdown.setText(fragment.getString(template.titleRes), false)
        } else {
            activeTemplateTitle.text = fragment.getString(R.string.template_active_title)
            activeTemplateDescription.text = ""
            presetDropdown.setText(resolvePresetDisplayNameForCurrentState(), false)
        }

        val cycleLocked = TemplateManager.isCycleEditingLocked(context)
        val rulesLocked = TemplateManager.isRulesEditingLocked(context)
        val canEditStartDate = TemplateManager.canEditStartDate(context)

        cycleDaysEdit.setEnabledWithAlpha(!cycleLocked)
        firstCycleDayDropdown.setEnabledWithAlpha(!cycleLocked)
        setChipGroupEnabled(!cycleLocked)
        pickDateButton.setEnabledWithAlpha(canEditStartDate)
        switchSaturdays.setEnabledWithAlpha(!rulesLocked)
        switchSundays.setEnabledWithAlpha(!rulesLocked)
        switchHolidays.setEnabledWithAlpha(!rulesLocked)
        holidayCountryDropdown.setEnabledWithAlpha(!rulesLocked)
    }

    private fun buildTemplateDescriptionText(template: ScheduleTemplate): String {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        val lockedItems = mutableListOf<String>()

        if (template.locksCycleEditing) lockedItems += fragment.getString(R.string.template_locked_item_cycle)
        if (template.locksRulesEditing) lockedItems += fragment.getString(R.string.template_locked_item_rules)
        if (!template.allowsStartDateEditing) lockedItems += fragment.getString(R.string.template_locked_item_start_date)
        if (TemplateManager.isAssignmentModeEditingLocked(context)) {
            lockedItems += fragment.getString(R.string.template_locked_item_assignment_mode)
        }

        val lockedSummary = if (lockedItems.isNotEmpty()) {
            fragment.getString(R.string.template_locked_summary_format, lockedItems.joinToString(", "))
        } else {
            ""
        }
        val continuousText = when (template.id) {
            ScheduleTemplateProvider.TEMPLATE_PANAMA_223,
            ScheduleTemplateProvider.TEMPLATE_4_ON_4_OFF -> fragment.getString(R.string.template_continuous_shift)
            else -> null
        }
        val parts = listOfNotNull(
            fragment.getString(template.descriptionRes),
            continuousText,
            fragment.getString(
                R.string.template_reference_date_format,
                template.fixedStartDate.format(dateFormatter),
                template.fixedFirstCycleDay
            ),
            lockedSummary.takeIf { it.isNotBlank() }
        )
        return parts.joinToString("\n\n")
    }

    private fun updatePresetSelectionState() {
        val presetText = resolvePresetDisplayNameForCurrentState()
        if (presetDropdown.text?.toString() != presetText) {
            presetDropdown.setText(presetText, false)
        }
    }

    private fun updateDateText() {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        dateText.text = fragment.getString(R.string.start_date, selectedDate.format(formatter))
    }

    private fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                updateDateText()
                updateUnsavedChangesState()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    private fun buildCurrentFormState(): PrimaryCycleFormState {
        val currentCycle = parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty())
        val currentFirstDay = sanitizeLabel(
            firstCycleDayDropdown.text?.toString().orEmpty(),
            currentCycle.firstOrNull() ?: "A"
        )
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val currentFirstDayIndex = if (TemplateManager.isTemplateActive(context)) {
            draftFirstCycleDayIndex ?: prefs.getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1)
        } else {
            -1
        }

        return PrimaryCycleFormState(
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

    private fun buildSavedFormState(): PrimaryCycleFormState {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val savedCycle = CycleManager.loadCycle(context).ifEmpty { listOf("A", "B") }
        val savedDate = CycleManager.loadStartDate(context)
        val savedFirstDay = sanitizeLabel(
            prefs.getString(AppPrefs.KEY_FIRST_CYCLE_DAY, savedCycle.firstOrNull() ?: "A").orEmpty(),
            savedCycle.firstOrNull() ?: "A"
        )
        val savedFirstDayIndex = if (TemplateManager.isTemplateActive(context)) {
            prefs.getInt(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX, -1)
        } else {
            -1
        }

        return PrimaryCycleFormState(
            cycle = savedCycle,
            selectedDate = savedDate,
            firstDay = savedFirstDay,
            firstDayIndex = savedFirstDayIndex,
            skipSaturdays = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true),
            skipSundays = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true),
            skipHolidays = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true),
            countryCode = HolidayManager.getSelectedCountry(context)
        )
    }

    private fun updateUnsavedChangesState() {
        if (buildCurrentFormState() == buildSavedFormState()) {
            clearUnsavedChanges()
        } else {
            markUnsavedChanges()
        }
    }

    private fun markUnsavedChanges() {
        if (isInitializing) return
        hasUnsavedChanges = true
        updateActionsState()
    }

    private fun clearUnsavedChanges() {
        hasUnsavedChanges = false
        updateActionsState()
    }

    private fun updateActionsState() {
        actions.visibility = if (hasUnsavedChanges) View.VISIBLE else View.GONE
        saveButton.isEnabled = hasUnsavedChanges
    }

    private fun runWithoutChangeTracking(block: () -> Unit) {
        val previous = isInitializing
        isInitializing = true
        try {
            block()
        } finally {
            isInitializing = previous
        }
    }

    private fun resolveSelectedCountryCodeFromUi(): String {
        val selectedText = holidayCountryDropdown.text?.toString()?.trim().orEmpty()
        return supportedCountries.firstOrNull {
            selectedText == HolidayManager.getCountryDisplayName(context, it.code) ||
                selectedText == HolidayManager.getCountryDisplayNameWithFlag(context, it.code) ||
                selectedText == HolidayManager.getCountryDisplayNameWithAutoDetected(context, it.code, true)
        }?.code ?: HolidayManager.DEFAULT_COUNTRY
    }

    private fun buildCountryDisplayList(detectedCode: String, isManual: Boolean): List<String> {
        return supportedCountries.map {
            HolidayManager.getCountryDisplayNameWithAutoDetected(
                context = context,
                countryCode = it.code,
                isAutoDetected = !isManual && it.code == detectedCode
            )
        }
    }

    private fun getCurrentCycleLabelsFromInput(): List<String> {
        return parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty()) { day ->
            fragment.getString(R.string.cycle_day_fallback, day)
        }
    }

    private fun parseCycleLabels(
        raw: String,
        fallbackLabelProvider: (Int) -> String = { index -> "Day $index" }
    ): List<String> {
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, label ->
                sanitizeLabel(label, fallbackLabelProvider(index + 1)).take(MAX_LABEL_LENGTH)
            }
            .take(MAX_CYCLE_ITEMS)
    }

    private fun resolvePresetDisplayNameForCurrentState(): String {
        val activeTemplate = TemplateManager.getActiveTemplate(context)
        if (activeTemplate != null) return fragment.getString(activeTemplate.titleRes)

        val matchedPreset = findMatchingCyclePresetForSavedState()
        return if (matchedPreset != null) {
            fragment.getString(matchedPreset.nameRes)
        } else {
            fragment.getString(R.string.template_none)
        }
    }

    private fun findMatchingCyclePresetForSavedState(): CyclePreset? {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val currentCycle = CycleManager.loadCycle(context).map {
            sanitizeLabel(it, "").take(MAX_LABEL_LENGTH)
        }
        val currentFirstDay = sanitizeLabel(
            prefs.getString(AppPrefs.KEY_FIRST_CYCLE_DAY, currentCycle.firstOrNull() ?: "").orEmpty(),
            currentCycle.firstOrNull() ?: ""
        ).take(MAX_LABEL_LENGTH)

        return CyclePresetProvider.getPresets().firstOrNull { preset ->
            val presetCycle = preset.cycleDaysProvider(context).map {
                sanitizeLabel(it, "").take(MAX_LABEL_LENGTH)
            }
            val presetFirstDay = sanitizeLabel(
                preset.defaultFirstDayProvider(context),
                presetCycle.firstOrNull() ?: ""
            ).take(MAX_LABEL_LENGTH)

            currentCycle == presetCycle && currentFirstDay.equals(presetFirstDay, ignoreCase = true)
        }
    }

    private fun getChipColorForLabel(label: String, cycleLabels: List<String>): Int {
        return try {
            CycleColorHelper.getBackgroundColor(context, label, cycleLabels)
        } catch (_: Exception) {
            ContextCompat.getColor(context, android.R.color.darker_gray)
        }
    }

    private fun setChipGroupEnabled(enabled: Boolean) {
        firstCycleDayChipGroup.isEnabled = enabled
        firstCycleDayChipGroup.alpha = if (enabled) 1f else 0.5f
        firstCycleDayChipGroup.children.forEach { chip ->
            chip.isEnabled = enabled
            chip.alpha = if (enabled) 1f else 0.6f
        }
    }

    private fun View.setEnabledWithAlpha(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
    }

    private fun configureAsSelectBox(dropdown: MaterialAutoCompleteTextView) {
        dropdown.keyListener = null
        dropdown.isCursorVisible = false
        dropdown.threshold = 0
        dropdown.setOnClickListener { dropdown.showDropdownIfPossible() }
        dropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) dropdown.showDropdownIfPossible()
        }
    }

    private fun MaterialAutoCompleteTextView.showDropdownIfPossible() {
        if ((adapter?.count ?: 0) > 0) {
            post { showDropDown() }
        }
    }

    private fun createNoFilterAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            context,
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

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showTemplateLockedMessage() {
        Toast.makeText(context, fragment.getString(R.string.template_locked_click_message), Toast.LENGTH_SHORT).show()
    }

    private data class PrimaryCycleFormState(
        val cycle: List<String>,
        val selectedDate: LocalDate,
        val firstDay: String,
        val firstDayIndex: Int,
        val skipSaturdays: Boolean,
        val skipSundays: Boolean,
        val skipHolidays: Boolean,
        val countryCode: String
    )
}

private fun ScheduleTemplate.getPickerTitleRes(): Int {
    return when (id) {
        ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB ->
            R.string.template_posta_slovenije_picker_title
        else -> titleRes
    }
}

private fun ScheduleTemplate.getPickerDescriptionRes(): Int {
    return when (id) {
        ScheduleTemplateProvider.TEMPLATE_SINGLE_SHIFT ->
            R.string.template_single_shift_picker_description
        ScheduleTemplateProvider.TEMPLATE_TWO_SHIFT ->
            R.string.template_two_shift_picker_description
        ScheduleTemplateProvider.TEMPLATE_THREE_SHIFT ->
            R.string.template_three_shift_picker_description
        ScheduleTemplateProvider.TEMPLATE_AB ->
            R.string.template_ab_picker_description
        ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB ->
            R.string.template_posta_slovenije_picker_description
        else -> descriptionRes
    }
}

private fun shortTemplateGroupTitle(title: String): String {
    return title.substringBefore(" ").uppercase(Locale.getDefault())
}
