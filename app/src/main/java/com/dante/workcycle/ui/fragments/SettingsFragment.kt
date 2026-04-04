package com.dante.workcycle.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.core.theme.AppThemeManager
import com.dante.workcycle.core.theme.ThemePreset
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.databinding.FragmentSettingsBinding
import com.dante.workcycle.domain.model.AssignmentCycleAdvanceMode
import com.dante.workcycle.domain.model.CycleMode
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.notifications.MidnightAlarmScheduler
import com.dante.workcycle.style.WidgetStyleManager
import com.dante.workcycle.ui.activity.MainActivity
import com.dante.workcycle.ui.dialogs.ColorPickerDialog
import com.dante.workcycle.widget.WidgetRefreshHelper

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/ABWorkdayWidget/blob/master/CHANGELOG.md"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var isInitializing = false

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

        val assignmentPrefs = AssignmentCyclePrefs(requireContext())

        isInitializing = true
        bindCurrentValues()

        binding.switchSecondaryEnabled.isChecked = assignmentPrefs.isEnabled()
        when (assignmentPrefs.getMode()) {
            CycleMode.MANUAL -> binding.radioManual.isChecked = true
            CycleMode.CYCLIC -> binding.radioCyclic.isChecked = true
        }

        setupAssignmentAdvanceModeDropdown()
        enforceTemplateAssignmentModeIfNeeded()
        updateSecondaryUi()
        updateCustomColorsUi()
        updateActiveTemplateInfoCard()
        setupListeners()
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
        val assignmentPrefs = AssignmentCyclePrefs(requireContext())
        val items = getAssignmentAdvanceModeItems()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        )

        binding.secondaryAdvanceModeDropdown.setAdapter(adapter)

        val currentMode = assignmentPrefs.getAdvanceMode()
        val selectedItem = items.firstOrNull { it.mode == currentMode } ?: items.first()

        binding.secondaryAdvanceModeDropdown.setText(selectedItem.title, false)

        binding.secondaryAdvanceModeDropdown.setOnItemClickListener { _, _, position, _ ->
            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) {
                binding.secondaryAdvanceModeDropdown.setText(selectedItem.title, false)
                return@setOnItemClickListener
            }

            val chosen = items[position]
            assignmentPrefs.setAdvanceMode(chosen.mode)
            WidgetRefreshHelper.refresh(requireContext())
        }
    }

    private fun enforceTemplateAssignmentModeIfNeeded() {
        if (!TemplateManager.isAssignmentModeEditingLocked(requireContext())) return

        val assignmentPrefs = AssignmentCyclePrefs(requireContext())

        assignmentPrefs.setEnabled(true)
        assignmentPrefs.setMode(CycleMode.MANUAL)
        assignmentPrefs.setAdvanceMode(AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY)

        binding.switchSecondaryEnabled.isChecked = true
        binding.radioManual.isChecked = true
    }

    private fun updateActiveTemplateInfoCard() {
        val template = TemplateManager.getActiveTemplate(requireContext())

        binding.activeTemplateSettingsCard.visibility =
            if (template != null) View.VISIBLE else View.GONE

        if (template != null) {
            val dateFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(
                java.time.format.FormatStyle.MEDIUM
            ).withLocale(java.util.Locale.getDefault())

            binding.activeTemplateSettingsTitle.text =
                getString(R.string.template_active_title) + ": " + getString(template.titleRes)

            binding.activeTemplateSettingsDescription.text =
                getString(template.descriptionRes) + "\n\n" +
                        getString(
                            R.string.template_reference_date_format,
                            template.fixedStartDate.format(dateFormatter),
                            template.fixedFirstCycleDay
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
                getString(R.string.assignment_mode_manual_helper)
            } else {
                getString(R.string.assignment_mode_cyclic_helper)
            }
    }

    private fun updateCustomColorsUi() {
        val isCustomTheme = binding.settingsThemeCustom.isChecked
        val alpha = if (isCustomTheme) 1f else 0.5f

        binding.textCustomColorsHelper.alpha = alpha
        binding.colorShiftA.alpha = alpha
        binding.colorShiftB.alpha = alpha
        binding.colorBackground.alpha = alpha

        binding.colorShiftA.isEnabled = isCustomTheme
        binding.colorShiftB.isEnabled = isCustomTheme
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

        binding.settingsVersionButton.text =
            getString(R.string.app_version, BuildConfig.VERSION_NAME)

        val colors = WidgetStyleManager.getColors(requireContext())
        binding.colorShiftA.setLabel(getString(R.string.color_shift_a))
        binding.colorShiftA.setColor(colors.shiftAColor)
        binding.colorShiftB.setLabel(getString(R.string.color_shift_b))
        binding.colorShiftB.setColor(colors.shiftBColor)
        binding.colorBackground.setLabel(getString(R.string.color_background))
        binding.colorBackground.setColor(colors.widgetBackgroundColor)

        updateCustomColorsUi()
    }

    private fun setupListeners() {
        val assignmentPrefs = AssignmentCyclePrefs(requireContext())

        binding.switchSecondaryEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            if (TemplateManager.isAssignmentModeEditingLocked(requireContext())) {
                isInitializing = true
                binding.switchSecondaryEnabled.isChecked = true
                isInitializing = false
                updateSecondaryUi()
                return@setOnCheckedChangeListener
            }

            assignmentPrefs.setEnabled(isChecked)
            updateSecondaryUi()
            WidgetRefreshHelper.refresh(requireContext())
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

            assignmentPrefs.setMode(mode)
            updateSecondaryUi()
            WidgetRefreshHelper.refresh(requireContext())
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

    private fun openChangelog() {
        val intent = Intent(Intent.ACTION_VIEW, CHANGELOG_URL.toUri())
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