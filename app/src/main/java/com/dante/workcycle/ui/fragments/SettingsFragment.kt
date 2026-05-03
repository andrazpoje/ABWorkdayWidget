package com.dante.workcycle.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.WorkCycleApp
import com.dante.workcycle.core.theme.AppThemeManager
import com.dante.workcycle.core.theme.ThemePreset
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.local.db.AppDatabase
import com.dante.workcycle.data.prefs.SettingsSectionPrefs
import com.dante.workcycle.debug.DebugDataResetHelper
import com.dante.workcycle.data.prefs.SecondaryCyclePrefs
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.databinding.FragmentSettingsBinding
import com.dante.workcycle.domain.backup.WorkCycleBackupManifest
import com.dante.workcycle.domain.backup.WorkCycleBackupPayloadCollector
import com.dante.workcycle.domain.backup.WorkCycleBackupWriter
import com.dante.workcycle.domain.model.AssignmentCycleAdvanceMode
import com.dante.workcycle.domain.model.CycleMode
import com.dante.workcycle.domain.premium.DebugPremiumEntitlementPrefs
import com.dante.workcycle.domain.premium.EntitlementOverrideMode
import com.dante.workcycle.domain.premium.PremiumFeature
import com.dante.workcycle.domain.premium.PremiumProvider
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.notifications.MidnightAlarmScheduler
import com.dante.workcycle.style.WidgetStyleManager
import com.dante.workcycle.ui.activity.MainActivity
import com.dante.workcycle.ui.dialogs.ColorPickerDialog
import com.dante.workcycle.ui.settings.PrimaryCycleSettingsController
import com.dante.workcycle.widget.WidgetRefreshHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/ABWorkdayWidget/blob/master/CHANGELOG.md"
        private const val DEVELOPER_TOOLS_UNLOCK_TAPS = 7
        private const val DEVELOPER_TOOLS_COUNTDOWN_START_TAP = 4
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var isInitializing = false
    private var developerToolsTapCount = 0
    private lateinit var primaryCycleSettingsController: PrimaryCycleSettingsController
    private lateinit var settingsSectionPrefs: SettingsSectionPrefs
    private val exportFullBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            exportFullBackup(uri)
        }
    }

    private data class AssignmentAdvanceModeUiItem(
        val mode: AssignmentCycleAdvanceMode,
        val title: String
    ) {
        override fun toString(): String = title
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSettingsBinding.bind(view)

        binding.settingsScrollView.applySystemBarsBottomInsetAsPadding()
        binding.settingsContentContainer.applySystemBarsHorizontalInsetAsPadding()
        settingsSectionPrefs = SettingsSectionPrefs(requireContext())

        val secondaryPrefs = SecondaryCyclePrefs(requireContext())
        val scrollToCycleSettings = arguments?.getBoolean(
            PrimaryCycleSettingsController.ARG_SCROLL_TO_CYCLE_SETTINGS,
            false
        ) == true

        isInitializing = true
        bindCurrentValues()
        primaryCycleSettingsController = PrimaryCycleSettingsController(
            fragment = this,
            root = view,
            onSaved = { updateActiveTemplateInfoCard() }
        )
        primaryCycleSettingsController.initialize(expandCycleSection = scrollToCycleSettings)

        binding.switchSecondaryEnabled.isChecked = secondaryPrefs.isEnabled()
        when (secondaryPrefs.getMode()) {
            CycleMode.MANUAL -> binding.radioManual.isChecked = true
            CycleMode.CYCLIC -> binding.radioCyclic.isChecked = true
        }

        setupAssignmentAdvanceModeDropdown()
        enforceTemplateAssignmentModeIfNeeded()
        updateSecondaryUi()
        updateCustomColorsUi()
        updateActiveTemplateInfoCard()
        setupCollapsibleSections()
        setupListeners()
        setupDeveloperTools()
        setupScrollHint()
        scrollToCycleSettingsIfRequested(scrollToCycleSettings)
        isInitializing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getAssignmentAdvanceModeItems(): List<AssignmentAdvanceModeUiItem> {
        return listOf(
            AssignmentAdvanceModeUiItem(
                mode = AssignmentCycleAdvanceMode.ALL_DAYS,
                title = getString(R.string.assignment_advance_mode_all_days)
            ),
            AssignmentAdvanceModeUiItem(
                mode = AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY,
                title = getString(R.string.assignment_advance_mode_working_days_only)
            )
        )
    }

    private fun setupAssignmentAdvanceModeDropdown() {
        val secondaryPrefs = SecondaryCyclePrefs(requireContext())
        val items = getAssignmentAdvanceModeItems()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        )

        binding.secondaryAdvanceModeDropdown.setAdapter(adapter)

        val currentMode = secondaryPrefs.getAdvanceMode()
        val selectedItem = items.firstOrNull { it.mode == currentMode } ?: items.first()

        binding.secondaryAdvanceModeDropdown.setText(selectedItem.title, false)

        binding.secondaryAdvanceModeDropdown.setOnItemClickListener { _, _, position, _ ->
            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) {
                binding.secondaryAdvanceModeDropdown.setText(selectedItem.title, false)
                return@setOnItemClickListener
            }

            val chosen = items[position]
            secondaryPrefs.setAdvanceMode(chosen.mode)
            WidgetRefreshHelper.refresh(requireContext())
        }
    }

    private fun enforceTemplateAssignmentModeIfNeeded() {
        if (!TemplateManager.isAssignmentModeEditingLocked(requireContext())) return

        val secondaryPrefs = SecondaryCyclePrefs(requireContext())

        secondaryPrefs.setEnabled(true)
        secondaryPrefs.setMode(CycleMode.MANUAL)
        secondaryPrefs.setAdvanceMode(AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY)

        binding.switchSecondaryEnabled.isChecked = true
        binding.radioManual.isChecked = true
    }

    private fun updateActiveTemplateInfoCard() {
        val template = TemplateManager.getActiveTemplate(requireContext())

        binding.activeTemplateSettingsCard.visibility =
            if (template != null) View.VISIBLE else View.GONE

        if (template != null) {
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(
                FormatStyle.MEDIUM
            ).withLocale(Locale.getDefault())

            binding.activeTemplateSettingsTitle.text =
                getString(R.string.template_active_title_format, getString(template.titleRes))

            binding.activeTemplateSettingsDescription.text =
                getString(
                    R.string.template_description_with_reference_format,
                    getString(template.descriptionRes),
                    getString(
                        R.string.template_reference_date_format,
                        template.fixedStartDate.format(dateFormatter),
                        template.resolveFixedFirstCycleDay(requireContext())
                    )
                )
        }
    }

    private fun updateSecondaryUi() {
        val enabled = binding.switchSecondaryEnabled.isChecked
        val isManual = binding.radioManual.isChecked

        val isLockedByTemplate = TemplateManager.isAssignmentModeEditingLocked(requireContext())

        binding.switchSecondaryEnabled.isEnabled = !isLockedByTemplate

        binding.radioGroupMode.isEnabled = enabled && !isLockedByTemplate
        binding.radioCyclic.isEnabled = enabled && !isLockedByTemplate
        binding.radioManual.isEnabled = enabled && !isLockedByTemplate

        binding.secondaryAdvanceModeDropdown.isEnabled = enabled && !isLockedByTemplate

        binding.layoutCyclicSettings.visibility =
            if (enabled && !isManual) View.VISIBLE else View.GONE

        binding.btnSecondaryLabels.visibility =
            if (enabled && isManual) View.VISIBLE else View.GONE

        binding.layoutCyclicSettings.alpha =
            if (enabled && !isManual) 1f else 0.4f

        binding.btnSecondaryLabels.alpha =
            if (enabled && isManual) 1f else 0.4f

        val alpha = if (enabled && !isLockedByTemplate) 1f else 0.4f
        binding.radioCyclic.alpha = alpha
        binding.radioManual.alpha = alpha
        binding.secondaryAdvanceModeDropdown.alpha = alpha
        binding.switchSecondaryEnabled.alpha = if (!isLockedByTemplate) 1f else 0.4f

        binding.assignmentModeHelperText.text =
            if (isLockedByTemplate) {
                getString(R.string.assignment_mode_locked_by_template)
            } else if (binding.radioManual.isChecked) {
                getString(R.string.secondary_mode_manual_helper)
            } else {
                getString(R.string.secondary_mode_cyclic_helper)
            }

        val canEditCyclicFields = enabled && !isManual && !isLockedByTemplate

        binding.editStartDate.isEnabled = canEditCyclicFields
        binding.editStartDateLayout.isEnabled = canEditCyclicFields
        binding.secondaryCycleChipGroup.isEnabled = canEditCyclicFields
        binding.btnAddCycleLabel.isEnabled = canEditCyclicFields
        binding.secondaryFirstDayLayout.isEnabled = canEditCyclicFields
        binding.secondaryFirstDayDropdown.isEnabled = canEditCyclicFields
        binding.secondaryAdvanceModeDropdown.isEnabled = canEditCyclicFields
    }

    private fun updateCustomColorsUi() {
        val isCustomTheme = binding.settingsThemeCustom.isChecked
        val alpha = if (isCustomTheme) 1f else 0.5f

        binding.textCustomColorsHelper.alpha = alpha
        binding.colorShiftA.alpha = alpha
        binding.colorShiftB.alpha = alpha
        binding.colorShiftC.alpha = alpha
        binding.colorOffDay.alpha = alpha
        binding.textWidgetBackgroundHelper.alpha = alpha
        binding.colorBackground.alpha = alpha

        binding.colorShiftA.isEnabled = isCustomTheme
        binding.colorShiftB.isEnabled = isCustomTheme
        binding.colorShiftC.isEnabled = isCustomTheme
        binding.colorOffDay.isEnabled = isCustomTheme
        binding.colorBackground.isEnabled = isCustomTheme
    }

    private fun bindCurrentValues() {
        when (AppThemeManager.loadTheme(requireContext())) {
            AppThemeManager.THEME_LIGHT -> binding.settingsAppThemeLight.isChecked = true
            AppThemeManager.THEME_DARK -> binding.settingsAppThemeDark.isChecked = true
            else -> binding.settingsAppThemeSystem.isChecked = true
        }

        when (WidgetStyleManager.getCurrentPreset(requireContext())) {
            ThemePreset.DARK -> binding.settingsThemeDark.isChecked = true
            ThemePreset.CLASSIC -> binding.settingsThemeClassic.isChecked = true
            ThemePreset.CUSTOM -> binding.settingsThemeCustom.isChecked = true
        }

        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val widgetStyle = prefs.getString(
            Prefs.KEY_WIDGET_STYLE,
            Prefs.WIDGET_STYLE_CLASSIC
        ) ?: Prefs.WIDGET_STYLE_CLASSIC

        binding.settingsWidgetStyleClassic.isChecked = widgetStyle == Prefs.WIDGET_STYLE_CLASSIC
        binding.settingsWidgetStyleMinimal.isChecked = widgetStyle == Prefs.WIDGET_STYLE_MINIMAL

        val notificationsEnabled = prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
        val silentEnabled = prefs.getBoolean(Prefs.KEY_SILENT_NOTIFICATION, false)

        binding.settingsSwitchNotificationsEnabled.isChecked = notificationsEnabled
        binding.settingsSwitchSilentNotification.isChecked = notificationsEnabled && silentEnabled
        binding.settingsSwitchSilentNotification.isEnabled = notificationsEnabled

        binding.settingsSwitchAssignmentIconsCalendar.isChecked = prefs.getBoolean(
            Prefs.KEY_SHOW_ASSIGNMENT_ICONS_CALENDAR,
            Prefs.DEFAULT_SHOW_ASSIGNMENT_ICONS_CALENDAR
        )

        binding.settingsSwitchAssignmentIconsWeekly.isChecked = prefs.getBoolean(
            Prefs.KEY_SHOW_ASSIGNMENT_ICONS_WEEKLY,
            Prefs.DEFAULT_SHOW_ASSIGNMENT_ICONS_WEEKLY
        )

        binding.settingsVersionText.text =
            getString(R.string.app_version_short, BuildConfig.VERSION_NAME)

        val colors = WidgetStyleManager.getColors(requireContext())
        binding.colorShiftA.setLabel(getString(R.string.color_shift_a))
        binding.colorShiftA.setColor(colors.shiftAColor)
        binding.colorShiftB.setLabel(getString(R.string.color_shift_b))
        binding.colorShiftB.setColor(colors.shiftBColor)
        binding.colorShiftC.setLabel(getString(R.string.color_shift_c))
        binding.colorShiftC.setColor(colors.shiftCColor)
        binding.colorOffDay.setLabel(getString(R.string.color_off_day))
        binding.colorOffDay.setColor(colors.offDayColor)
        binding.colorBackground.setLabel(getString(R.string.color_background))
        binding.colorBackground.setColor(colors.widgetBackgroundColor)

        updateCustomColorsUi()

        val secondaryPrefs = SecondaryCyclePrefs(requireContext())
        updateSecondaryStartDateText(secondaryPrefs.getStartDate())
        renderSecondaryCycleChips()
        refreshSecondaryFirstDayDropdown()
    }

    private fun updateSecondaryStartDateText(date: LocalDate) {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())

        binding.editStartDate.setText(date.format(formatter))
    }

    private fun showSecondaryStartDatePicker() {
        val secondaryPrefs = SecondaryCyclePrefs(requireContext())
        val selectedDate = secondaryPrefs.getStartDate()

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                secondaryPrefs.setStartDate(newDate)
                updateSecondaryStartDateText(newDate)
                WidgetRefreshHelper.refresh(requireContext())
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )

        dialog.show()
    }

    private fun getSecondaryCycleLabels(): List<String> {
        return SecondaryCyclePrefs(requireContext()).getCycle()
    }

    private fun refreshSecondaryFirstDayDropdown() {
        val secondaryPrefs = SecondaryCyclePrefs(requireContext())
        val labels = getSecondaryCycleLabels().ifEmpty { secondaryPrefs.getCycle() }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            labels
        )

        binding.secondaryFirstDayDropdown.setAdapter(adapter)

        val savedFirstDay = secondaryPrefs.getFirstCycleDay().trim()
        val selected = labels.firstOrNull { it == savedFirstDay }
            ?: labels.firstOrNull()
            ?: ""

        if (selected.isNotBlank()) {
            if (selected != savedFirstDay) {
                secondaryPrefs.setFirstCycleDay(selected)
            }
            binding.secondaryFirstDayDropdown.setText(selected, false)
        } else {
            binding.secondaryFirstDayDropdown.setText("", false)
        }
    }

    private fun setupScrollHint() {
        val scrollView = binding.settingsScrollView
        val hint = binding.viewBottomScrollHint

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val child = scrollView.getChildAt(0)
            if (child != null) {
                val diff = child.bottom - (scrollView.height + scrollView.scrollY)
                hint.visibility = if (diff > 24) View.VISIBLE else View.GONE
            } else {
                hint.visibility = View.GONE
            }
        }

        scrollView.post {
            val child = scrollView.getChildAt(0)
            if (child != null) {
                val diff = child.bottom - (scrollView.height + scrollView.scrollY)
                hint.visibility = if (diff > 24) View.VISIBLE else View.GONE
            } else {
                hint.visibility = View.GONE
            }
        }
    }

    private fun setupCollapsibleSections() {
        setupCollapsibleSection(
            header = binding.secondaryCycleHeader,
            content = binding.secondaryCycleSection,
            arrow = binding.secondaryCycleArrow,
            sectionKey = SettingsSectionPrefs.SECTION_SECONDARY_CYCLE
        )
        setupCollapsibleSection(
            header = binding.statusLabelsHeader,
            content = binding.statusLabelsSection,
            arrow = binding.statusLabelsArrow,
            sectionKey = SettingsSectionPrefs.SECTION_STATUS_LABELS
        )
        setupCollapsibleSection(
            header = binding.displayHeader,
            content = binding.displaySection,
            arrow = binding.displayArrow,
            sectionKey = SettingsSectionPrefs.SECTION_DISPLAY
        )
        setupCollapsibleSection(
            header = binding.colorsHeader,
            content = binding.colorsSection,
            arrow = binding.colorsArrow,
            sectionKey = SettingsSectionPrefs.SECTION_COLORS
        )
        setupCollapsibleSection(
            header = binding.appearanceHeader,
            content = binding.appearanceSection,
            arrow = binding.appearanceArrow,
            sectionKey = SettingsSectionPrefs.SECTION_APPEARANCE
        )
        setupCollapsibleSection(
            header = binding.widgetHeader,
            content = binding.widgetSection,
            arrow = binding.widgetArrow,
            sectionKey = SettingsSectionPrefs.SECTION_WIDGETS
        )
        setupCollapsibleSection(
            header = binding.notificationsHeader,
            content = binding.notificationsSection,
            arrow = binding.notificationsArrow,
            sectionKey = SettingsSectionPrefs.SECTION_NOTIFICATIONS
        )
        setupCollapsibleSection(
            header = binding.backupHeader,
            content = binding.backupSection,
            arrow = binding.backupArrow,
            sectionKey = SettingsSectionPrefs.SECTION_BACKUP
        )
        setupCollapsibleSection(
            header = binding.developerToolsHeader,
            content = binding.developerToolsSection,
            arrow = binding.developerToolsArrow,
            sectionKey = SettingsSectionPrefs.SECTION_DEVELOPER_TOOLS
        )
    }

    private fun setupCollapsibleSection(
        header: View,
        content: View,
        arrow: View,
        sectionKey: String,
        defaultExpanded: Boolean = false
    ) {
        val expanded = settingsSectionPrefs.isExpanded(sectionKey, defaultExpanded)
        setSectionExpanded(content, arrow, expanded)

        header.setOnClickListener {
            val newExpanded = !content.isVisible
            setSectionExpanded(content, arrow, newExpanded)
            settingsSectionPrefs.setExpanded(sectionKey, newExpanded)
        }
    }

    private fun setSectionExpanded(content: View, arrow: View, expanded: Boolean) {
        content.isVisible = expanded
        arrow.rotation = if (expanded) 180f else 0f
    }

    private fun scrollToCycleSettingsIfRequested(shouldScroll: Boolean) {
        if (!shouldScroll) return

        binding.settingsScrollView.post {
            binding.settingsScrollView.smoothScrollTo(
                0,
                binding.primaryCycleSettingsContainer.top
            )
        }
    }

    private fun setupListeners() {
        val secondaryPrefs = SecondaryCyclePrefs(requireContext())

        binding.switchSecondaryEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) {
                isInitializing = true
                binding.switchSecondaryEnabled.isChecked = true
                isInitializing = false
                updateSecondaryUi()
                return@setOnCheckedChangeListener
            }

            secondaryPrefs.setEnabled(isChecked)
            updateSecondaryUi()
            WidgetRefreshHelper.refresh(requireContext())
        }

        binding.btnAddCycleLabel.setOnClickListener {
            showAddCycleLabelDialog()
        }


        binding.radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            if (isInitializing) return@setOnCheckedChangeListener

            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) {
                isInitializing = true
                binding.radioManual.isChecked = true
                isInitializing = false
                updateSecondaryUi()
                return@setOnCheckedChangeListener
            }

            val mode = if (checkedId == R.id.radioManual) {
                CycleMode.MANUAL
            } else {
                CycleMode.CYCLIC
            }

            secondaryPrefs.setMode(mode)
            updateSecondaryUi()
            WidgetRefreshHelper.refresh(requireContext())
            refreshSecondaryFirstDayDropdown()
        }

        binding.settingsThemeCustom.setOnClickListener {
            if (!isInitializing) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CUSTOM)
                WidgetRefreshHelper.refresh(requireContext())
                updateCustomColorsUi()
            }
        }

        binding.btnSecondaryLabels.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_secondaryLabelsFragment
            )
        }

        binding.btnStatusLabels.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_statusLabelsFragment
            )
        }

        binding.settingsAppThemeSystem.setOnClickListener {
            if (!isInitializing) saveAppTheme(AppThemeManager.THEME_SYSTEM)
        }

        binding.settingsAppThemeLight.setOnClickListener {
            if (!isInitializing) saveAppTheme(AppThemeManager.THEME_LIGHT)
        }

        binding.settingsAppThemeDark.setOnClickListener {
            if (!isInitializing) saveAppTheme(AppThemeManager.THEME_DARK)
        }

        binding.settingsThemeClassic.setOnClickListener {
            if (!isInitializing) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CLASSIC)
                WidgetRefreshHelper.refresh(requireContext())
                updateCustomColorsUi()
            }
        }

        binding.settingsThemeDark.setOnClickListener {
            if (!isInitializing) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.DARK)
                WidgetRefreshHelper.refresh(requireContext())
                updateCustomColorsUi()
            }
        }

        binding.colorShiftA.setOnRowClick {
            if (!binding.settingsThemeCustom.isChecked) return@setOnRowClick

            val currentColors = WidgetStyleManager.getColors(requireContext())

            ColorPickerDialog(
                titleText = getString(R.string.color_picker_title_cycle_a),
                initialColor = currentColors.shiftAColor
            ) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CUSTOM)
                WidgetStyleManager.updateShiftAColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorA")
        }

        binding.colorShiftB.setOnRowClick {
            if (!binding.settingsThemeCustom.isChecked) return@setOnRowClick

            val currentColors = WidgetStyleManager.getColors(requireContext())

            ColorPickerDialog(
                titleText = getString(R.string.color_picker_title_cycle_b),
                initialColor = currentColors.shiftBColor
            ) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CUSTOM)
                WidgetStyleManager.updateShiftBColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorB")
        }

        binding.colorShiftC.setOnRowClick {
            if (!binding.settingsThemeCustom.isChecked) return@setOnRowClick

            val currentColors = WidgetStyleManager.getColors(requireContext())

            ColorPickerDialog(
                titleText = getString(R.string.color_picker_title_cycle_c),
                initialColor = currentColors.shiftCColor
            ) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CUSTOM)
                WidgetStyleManager.updateShiftCColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorC")
        }

        binding.colorOffDay.setOnRowClick {
            if (!binding.settingsThemeCustom.isChecked) return@setOnRowClick

            val currentColors = WidgetStyleManager.getColors(requireContext())

            ColorPickerDialog(
                titleText = getString(R.string.color_picker_title_off_day),
                initialColor = currentColors.offDayColor
            ) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CUSTOM)
                WidgetStyleManager.updateOffDayColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorOffDay")
        }

        binding.colorBackground.setOnRowClick {
            if (!binding.settingsThemeCustom.isChecked) return@setOnRowClick

            val currentColors = WidgetStyleManager.getColors(requireContext())

            ColorPickerDialog(
                titleText = getString(R.string.color_picker_title_background),
                initialColor = currentColors.widgetBackgroundColor
            ) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CUSTOM)
                WidgetStyleManager.updateWidgetBackgroundColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorBg")
        }

        binding.settingsWidgetStyleClassic.setOnClickListener {
            if (!isInitializing) saveWidgetStyle(Prefs.WIDGET_STYLE_CLASSIC)
        }

        binding.settingsWidgetStyleMinimal.setOnClickListener {
            if (!isInitializing) saveWidgetStyle(Prefs.WIDGET_STYLE_MINIMAL)
        }

        binding.settingsSwitchNotificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            binding.settingsSwitchSilentNotification.isEnabled = isChecked

            if (isChecked) {
                val mainActivity = activity as? MainActivity
                if (mainActivity == null) {
                    isInitializing = true
                    binding.settingsSwitchNotificationsEnabled.isChecked = false
                    binding.settingsSwitchSilentNotification.isChecked = false
                    binding.settingsSwitchSilentNotification.isEnabled = false
                    isInitializing = false

                    saveNotificationSettings(enabled = false, silent = false)
                    MidnightAlarmScheduler.cancel(requireContext())
                    return@setOnCheckedChangeListener
                }

                mainActivity.requestNotificationPermissionIfNeeded { granted ->
                    if (granted) {
                        binding.settingsSwitchSilentNotification.isEnabled = true

                        saveNotificationSettings(
                            enabled = true,
                            silent = binding.settingsSwitchSilentNotification.isChecked
                        )
                        MidnightAlarmScheduler.scheduleNext(requireContext())
                    } else {
                        isInitializing = true
                        binding.settingsSwitchNotificationsEnabled.isChecked = false
                        binding.settingsSwitchSilentNotification.isChecked = false
                        binding.settingsSwitchSilentNotification.isEnabled = false
                        isInitializing = false

                        saveNotificationSettings(enabled = false, silent = false)
                        MidnightAlarmScheduler.cancel(requireContext())
                    }
                }
            } else {
                binding.settingsSwitchSilentNotification.isChecked = false
                binding.settingsSwitchSilentNotification.isEnabled = false

                saveNotificationSettings(enabled = false, silent = false)
                MidnightAlarmScheduler.cancel(requireContext())
            }
        }

        binding.settingsSwitchSilentNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            saveNotificationSettings(
                enabled = binding.settingsSwitchNotificationsEnabled.isChecked,
                silent = if (binding.settingsSwitchNotificationsEnabled.isChecked) isChecked else false
            )
        }

        binding.settingsSwitchAssignmentIconsCalendar.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            requireContext()
                .getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(Prefs.KEY_SHOW_ASSIGNMENT_ICONS_CALENDAR, isChecked)
                .apply()
        }

        binding.settingsSwitchAssignmentIconsWeekly.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            requireContext()
                .getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(Prefs.KEY_SHOW_ASSIGNMENT_ICONS_WEEKLY, isChecked)
                .apply()
        }

        binding.buttonSettingsViewWhatsNew.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_whatsNewFragment)
        }

        binding.buttonSettingsFullChangelog.setOnClickListener {
            openChangelog()
        }
        setupBackupActions()
        binding.editStartDate.setOnClickListener {
            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) return@setOnClickListener
            showSecondaryStartDatePicker()
        }

        binding.editStartDateLayout.setOnClickListener {
            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) return@setOnClickListener
            showSecondaryStartDatePicker()
        }

        binding.secondaryFirstDayDropdown.setOnItemClickListener { _, _, position, _ ->
            if (isInitializing) return@setOnItemClickListener
            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) return@setOnItemClickListener

            val labels = getSecondaryCycleLabels().ifEmpty {
                secondaryPrefs.getCycle()
            }

            val selected = labels.getOrNull(position) ?: return@setOnItemClickListener

            secondaryPrefs.setFirstCycleDay(selected)
            WidgetRefreshHelper.refresh(requireContext())
        }


    }

    private fun setupBackupActions() {
        // TODO: Deferred after the v3.0 release pass. If backup export is ever
        // gated, do it through the audited runtime FeatureGate chain rather
        // than ad hoc UI checks here. Do not enable real premium gating from
        // this screen piecemeal.
        binding.rowExportFullBackup.setOnClickListener {
            exportFullBackupLauncher.launch(buildFullBackupFileName())
        }
    }

    private fun setupDeveloperTools() {
        binding.cardDeveloperTools.visibility =
            if (BuildConfig.DEBUG && DebugDataResetHelper.isDeveloperToolsUnlocked(requireContext())) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (!BuildConfig.DEBUG) return

        binding.settingsVersionText.setOnClickListener {
            handleDeveloperToolsVersionTap()
        }

        binding.buttonResetOnboardingTestState.setOnClickListener {
            if (DebugDataResetHelper.resetOnboardingTestState(requireContext())) {
                DebugDataResetHelper.restartApp(requireActivity())
            }
        }

        binding.buttonClearLocalAppData.setOnClickListener {
            showClearLocalDataConfirmation()
        }

        // TODO: Deferred after the v3.0 release pass. Connect Developer tools
        // premium override to the audited runtime FeatureGate chain only for
        // debug diagnostics and future UI gating tests. Do not place
        // production entitlement, billing, or real gating behavior here.
        binding.buttonUnlockPremiumForTesting.setOnClickListener {
            val prefs = DebugPremiumEntitlementPrefs.create(
                context = requireContext(),
                isDebugBuild = BuildConfig.DEBUG
            )
            prefs.setOverrideMode(EntitlementOverrideMode.UNLOCK_ALL_PREMIUM)
            Toast.makeText(
                requireContext(),
                getString(R.string.debug_premium_override_enabled),
                Toast.LENGTH_SHORT
            ).show()
            updatePremiumTestStateUi()
        }

        binding.buttonLockPremiumForTesting.setOnClickListener {
            val prefs = DebugPremiumEntitlementPrefs.create(
                context = requireContext(),
                isDebugBuild = BuildConfig.DEBUG
            )
            prefs.setOverrideMode(EntitlementOverrideMode.LOCK_ALL_PREMIUM)
            Toast.makeText(
                requireContext(),
                getString(R.string.debug_premium_override_locked),
                Toast.LENGTH_SHORT
            ).show()
            updatePremiumTestStateUi()
        }

        binding.buttonResetPremiumOverride.setOnClickListener {
            val prefs = DebugPremiumEntitlementPrefs.create(
                context = requireContext(),
                isDebugBuild = BuildConfig.DEBUG
            )
            prefs.clear()
            Toast.makeText(
                requireContext(),
                getString(R.string.debug_premium_override_reset),
                Toast.LENGTH_SHORT
            ).show()
            updatePremiumTestStateUi()
        }

        updatePremiumTestStateUi()
    }

    private fun handleDeveloperToolsVersionTap() {
        if (!BuildConfig.DEBUG) return
        if (DebugDataResetHelper.isDeveloperToolsUnlocked(requireContext())) return

        developerToolsTapCount += 1

        val remaining = DEVELOPER_TOOLS_UNLOCK_TAPS - developerToolsTapCount
        if (remaining <= 0) {
            if (DebugDataResetHelper.unlockDeveloperTools(requireContext())) {
                binding.cardDeveloperTools.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    getString(R.string.debug_developer_tools_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        if (developerToolsTapCount >= DEVELOPER_TOOLS_COUNTDOWN_START_TAP) {
            Toast.makeText(
                requireContext(),
                getString(R.string.debug_developer_tools_taps_remaining, remaining),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showClearLocalDataConfirmation() {
        if (!BuildConfig.DEBUG) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.debug_clear_local_data_title)
            .setMessage(R.string.debug_clear_local_data_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (DebugDataResetHelper.clearLocalAppData(requireContext())) {
                        DebugDataResetHelper.restartApp(requireActivity())
                    }
                }
            }
            .show()
    }

    private fun updatePremiumTestStateUi() {
        if (!BuildConfig.DEBUG) return

        val prefs = DebugPremiumEntitlementPrefs.create(
            context = requireContext(),
            isDebugBuild = BuildConfig.DEBUG
        )
        val override = prefs.getOverride()
        val decision = PremiumProvider.featureGate(requireContext())
            .canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        binding.textPremiumTestStateTitle.text = getString(R.string.debug_premium_test_state_title)
        binding.textPremiumTestOverride.text = getString(
            R.string.debug_premium_test_override_format,
            override.mode.name
        )
        binding.textPremiumTestDecision.text = getString(
            R.string.debug_premium_test_sample_gate_format,
            getString(
                if (decision.allowed) {
                    R.string.debug_premium_test_allowed
                } else {
                    R.string.debug_premium_test_locked
                }
            ),
            decision.source.name
        )
    }

    private fun saveAppTheme(theme: String) {
        AppThemeManager.saveTheme(requireContext(), theme)
        AppThemeManager.apply(theme)
    }

    private fun saveWidgetStyle(style: String) {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(Prefs.KEY_WIDGET_STYLE, style)
        }
        refreshWidget()
    }

    private fun saveNotificationSettings(enabled: Boolean, silent: Boolean) {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, enabled)
            putBoolean(Prefs.KEY_SILENT_NOTIFICATION, silent)
        }
    }

    private fun refreshWidget() {
        WidgetRefreshHelper.refresh(requireContext())
    }

    private fun buildFullBackupFileName(): String {
        return "workcycle-backup-${LocalDate.now()}.zip"
    }

    private fun buildBackupManifest(): WorkCycleBackupManifest {
        return WorkCycleBackupManifest(
            backupFormatVersion = 1,
            createdAt = System.currentTimeMillis(),
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            databaseVersion = AppDatabase.DATABASE_VERSION,
            packageName = BuildConfig.APPLICATION_ID
        )
    }

    private fun exportFullBackup(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val exportResult = runCatching {
                val app = requireContext().applicationContext as WorkCycleApp
                // Future restore/import must remain a separate flow and must
                // use WorkCycleBackupValidator preflight before any write to
                // local database or prefs.
                val collector = WorkCycleBackupPayloadCollector(
                    context = requireContext(),
                    database = app.database
                )
                val payload = withContext(Dispatchers.IO) {
                    collector.collect(buildBackupManifest())
                }
                val zipBytes = withContext(Dispatchers.IO) {
                    WorkCycleBackupWriter.toByteArray(payload)
                }

                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(zipBytes)
                        outputStream.flush()
                    } ?: error("Failed to open backup export output stream.")
                }
            }

            if (exportResult.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.work_cycle_backup_export_success),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.work_cycle_backup_export_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showAddCycleLabelDialog() {
        val input = EditText(requireContext())

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_label)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotBlank()) {
                    val prefs = SecondaryCyclePrefs(requireContext())
                    val updated = prefs.getCycle().toMutableList()
                    updated.add(value)
                    prefs.setCycle(updated)

                    renderSecondaryCycleChips()
                    refreshSecondaryFirstDayDropdown()
                    WidgetRefreshHelper.refresh(requireContext())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renderSecondaryCycleChips() {
        val prefs = SecondaryCyclePrefs(requireContext())
        val labels = prefs.getCycle()

        binding.secondaryCycleChipGroup.removeAllViews()

        labels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCloseIconVisible = true

                setOnCloseIconClickListener {
                    val updated = labels.toMutableList()
                    updated.remove(label)
                    prefs.setCycle(updated)

                    if (prefs.getFirstCycleDay() == label) {
                        prefs.setFirstCycleDay(updated.firstOrNull() ?: "")
                    }

                    renderSecondaryCycleChips()
                    refreshSecondaryFirstDayDropdown()
                    WidgetRefreshHelper.refresh(requireContext())
                }
            }

            binding.secondaryCycleChipGroup.addView(chip)
        }
    }

    private fun openChangelog() {
        val uri = Uri.parse(CHANGELOG_URL.trim())
        Log.d("CHANGELOG_URL", uri.toString())
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
