package com.dante.workcycle.ui.worklog

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.domain.model.CycleLayer
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WorkLogSettingsFragment : Fragment(R.layout.fragment_work_log_settings) {

    private lateinit var workSettingsPrefs: WorkSettingsPrefs
    private lateinit var rowDailyTarget: View
    private lateinit var rowDefaultBreak: View
    private lateinit var rowOvertimeTracking: View
    private lateinit var rowWidgetInfoMode: View
    private lateinit var containerExpectedStartRows: LinearLayout
    private lateinit var textDailyTargetValue: TextView
    private lateinit var textDefaultBreakValue: TextView
    private lateinit var textOvertimeTrackingValue: TextView
    private lateinit var textWidgetInfoModeValue: TextView
    private lateinit var switchOvertimeTracking: MaterialSwitch

    private var isBindingSwitch = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workSettingsPrefs = WorkSettingsPrefs(requireContext())

        view.findViewById<View>(R.id.workLogSettingsScrollView)
            .applySystemBarsBottomInsetAsPadding()

        view.findViewById<View>(R.id.workLogSettingsContentContainer)
            .applySystemBarsHorizontalInsetAsPadding()

        setupViews(view)
        setupActions()
        bindSettings()
    }

    override fun onResume() {
        super.onResume()
        bindSettings()
    }

    private fun setupViews(view: View) {
        rowDailyTarget = view.findViewById(R.id.rowDailyTarget)
        rowDefaultBreak = view.findViewById(R.id.rowDefaultBreak)
        rowOvertimeTracking = view.findViewById(R.id.rowOvertimeTracking)
        rowWidgetInfoMode = view.findViewById(R.id.rowWidgetInfoMode)
        containerExpectedStartRows = view.findViewById(R.id.containerExpectedStartRows)
        textDailyTargetValue = view.findViewById(R.id.textDailyTargetValue)
        textDefaultBreakValue = view.findViewById(R.id.textDefaultBreakValue)
        textOvertimeTrackingValue = view.findViewById(R.id.textOvertimeTrackingValue)
        textWidgetInfoModeValue = view.findViewById(R.id.textWidgetInfoModeValue)
        switchOvertimeTracking = view.findViewById(R.id.switchOvertimeTracking)
    }

    private fun setupActions() {
        rowDailyTarget.setOnClickListener {
            showDailyTargetDialog()
        }

        rowDefaultBreak.setOnClickListener {
            showDefaultBreakDialog()
        }

        rowOvertimeTracking.setOnClickListener {
            switchOvertimeTracking.toggle()
        }

        rowWidgetInfoMode.setOnClickListener {
            showWidgetInfoModeDialog()
        }

        switchOvertimeTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingSwitch) return@setOnCheckedChangeListener
            workSettingsPrefs.setOvertimeTrackingEnabled(isChecked)
            bindSettings()
        }
    }

    private fun bindSettings() {
        textDailyTargetValue.text = formatHoursMinutesLabel(
            workSettingsPrefs.getDailyTargetMinutes()
        )
        textDefaultBreakValue.text = formatBreakLabel(
            workSettingsPrefs.getDefaultBreakMinutes()
        )

        val overtimeEnabled = workSettingsPrefs.isOvertimeTrackingEnabled()
        textOvertimeTrackingValue.text = getString(
            if (overtimeEnabled) {
                R.string.work_log_settings_enabled
            } else {
                R.string.work_log_settings_disabled
            }
        )

        isBindingSwitch = true
        switchOvertimeTracking.isChecked = overtimeEnabled
        isBindingSwitch = false

        textWidgetInfoModeValue.text = getWidgetInfoModeLabel(workSettingsPrefs.getWidgetInfoMode())

        bindExpectedTimeRows()
    }

    private fun showDailyTargetDialog() {
        val presets = WorkSettingsPrefs.DAILY_TARGET_PRESETS
        val labels = presets.map(::formatHoursMinutesLabel).toTypedArray()
        val checkedIndex = presets.indexOf(workSettingsPrefs.getDailyTargetMinutes()).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.work_log_settings_choose_daily_target)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                workSettingsPrefs.setDailyTargetMinutes(presets[which])
                bindSettings()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDefaultBreakDialog() {
        val presets = WorkSettingsPrefs.BREAK_PRESETS
        val labels = presets.map(::formatBreakLabel).toTypedArray()
        val checkedIndex = presets.indexOf(workSettingsPrefs.getDefaultBreakMinutes()).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.work_log_settings_choose_default_break)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                workSettingsPrefs.setDefaultBreakMinutes(presets[which])
                bindSettings()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showWidgetInfoModeDialog() {
        val modes = arrayOf(
            WorkSettingsPrefs.WIDGET_INFO_MODE_WORKED_TODAY,
            WorkSettingsPrefs.WIDGET_INFO_MODE_START_TIME
        )
        val labels = modes.map(::getWidgetInfoModeLabel).toTypedArray()
        val checkedIndex = modes.indexOf(workSettingsPrefs.getWidgetInfoMode()).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.work_log_settings_widget_info_mode)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                workSettingsPrefs.setWidgetInfoMode(modes[which])
                WidgetRefreshDispatcher.refreshWorkLogWidgets(requireContext())
                bindSettings()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun getWidgetInfoModeLabel(mode: String): String {
        return getString(
            when (mode) {
                WorkSettingsPrefs.WIDGET_INFO_MODE_WORKED_TODAY ->
                    R.string.work_log_settings_widget_info_mode_worked_today
                else ->
                    R.string.work_log_settings_widget_info_mode_start_time
            }
        )
    }

    private fun formatHoursMinutesLabel(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (minutes == 0) {
            getString(R.string.work_log_settings_hours_format, hours)
        } else {
            getString(R.string.work_log_settings_hours_minutes_format, hours, minutes)
        }
    }

    private fun formatBreakLabel(totalMinutes: Int): String {
        return if (totalMinutes == 0) {
            getString(R.string.work_log_settings_no_break)
        } else {
            getString(R.string.work_log_settings_minutes_format, totalMinutes)
        }
    }

    private fun bindExpectedTimeRows() {
        containerExpectedStartRows.removeAllViews()

        val primaryLabels = CycleManager.loadCycle(requireContext())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val secondaryLabels = AssignmentLabelsPrefs(requireContext())
            .getSelectableLabels()
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val inflater = LayoutInflater.from(requireContext())

        addExpectedTimeSectionHeader(R.string.work_log_settings_primary_expected_times_section)
        bindExpectedTimeRows(
            inflater = inflater,
            layer = CycleLayer.PRIMARY,
            labels = primaryLabels
        )

        if (secondaryLabels.isNotEmpty()) {
            addExpectedTimeSectionHeader(R.string.work_log_settings_secondary_expected_times_section)
            bindExpectedTimeRows(
                inflater = inflater,
                layer = CycleLayer.SECONDARY,
                labels = secondaryLabels
            )
        }
    }

    private fun bindExpectedTimeRows(
        inflater: LayoutInflater,
        layer: CycleLayer,
        labels: List<String>
    ) {
        labels.forEachIndexed { index, label ->
            val row = inflater.inflate(
                R.layout.item_work_log_expected_start_setting,
                containerExpectedStartRows,
                false
            )

            val textLabel = row.findViewById<TextView>(R.id.textExpectedTimeLabel)
            val textStartTime = row.findViewById<TextView>(R.id.textExpectedStartTime)
            val textEndTime = row.findViewById<TextView>(R.id.textExpectedEndTime)
            val switchStartEnabled = row.findViewById<MaterialSwitch>(R.id.switchExpectedStart)
            val switchEndEnabled = row.findViewById<MaterialSwitch>(R.id.switchExpectedEnd)

            val startConfig = workSettingsPrefs.getExpectedStartConfig(layer, label)
            val endConfig = workSettingsPrefs.getExpectedEndConfig(layer, label)
            val isStartEnabled = startConfig?.enabled == true
            val isEndEnabled = endConfig?.enabled == true
            val savedStartTime = startConfig?.startTime
            val savedEndTime = endConfig?.endTime

            textLabel.text = label
            textStartTime.text = savedStartTime ?: getString(R.string.work_log_settings_expected_time_not_set)
            textEndTime.text = savedEndTime ?: getString(R.string.work_log_settings_expected_time_not_set)
            textStartTime.alpha = if (isStartEnabled) 1f else 0.6f
            textEndTime.alpha = if (isEndEnabled) 1f else 0.6f

            switchStartEnabled.isChecked = isStartEnabled
            switchStartEnabled.setOnCheckedChangeListener { _, checked ->
                workSettingsPrefs.setExpectedStartEnabled(layer, label, checked)
                bindExpectedTimeRows()
            }

            switchEndEnabled.isChecked = isEndEnabled
            switchEndEnabled.setOnCheckedChangeListener { _, checked ->
                workSettingsPrefs.setExpectedEndEnabled(layer, label, checked)
                bindExpectedTimeRows()
            }

            textStartTime.setOnClickListener {
                showExpectedStartTimePicker(layer, label)
            }

            textEndTime.setOnClickListener {
                showExpectedEndTimePicker(layer, label)
            }

            containerExpectedStartRows.addView(row)

            if (index < labels.lastIndex) {
                containerExpectedStartRows.addView(
                    View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                        setBackgroundColor(
                            com.google.android.material.color.MaterialColors.getColor(
                                this,
                                com.google.android.material.R.attr.colorOutlineVariant
                            )
                        )
                    }
                )
            }
        }
    }

    private fun addExpectedTimeSectionHeader(titleRes: Int) {
        containerExpectedStartRows.addView(
            TextView(requireContext()).apply {
                text = getString(titleRes)
                textSize = 13f
                alpha = 0.75f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 14, 0, 6)
            }
        )
    }

    private fun showExpectedStartTimePicker(layer: CycleLayer, label: String) {
        val currentTime = parseTimeOrNull(workSettingsPrefs.getExpectedStartTimeOrDefault(layer, label))
            ?: LocalTime.of(8, 0)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText(getString(R.string.work_log_expected_start))
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedTime = LocalTime.of(picker.hour, picker.minute)
            workSettingsPrefs.setExpectedStartTime(layer, label, timeFormatter.format(selectedTime))
            bindExpectedTimeRows()
        }

        picker.show(childFragmentManager, "expected_start_time_picker_${layer.name}_$label")
    }

    private fun showExpectedEndTimePicker(layer: CycleLayer, label: String) {
        val currentTime = parseTimeOrNull(workSettingsPrefs.getExpectedEndTimeOrDefault(layer, label))
            ?: LocalTime.of(16, 0)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText(getString(R.string.work_log_expected_end))
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedTime = LocalTime.of(picker.hour, picker.minute)
            workSettingsPrefs.setExpectedEndTime(layer, label, timeFormatter.format(selectedTime))
            bindExpectedTimeRows()
        }

        picker.show(childFragmentManager, "expected_end_time_picker_${layer.name}_$label")
    }

    private fun parseTimeOrNull(value: String?): LocalTime? {
        val safeValue = value?.trim().orEmpty()
        if (safeValue.isBlank()) return null
        return runCatching { LocalTime.parse(safeValue, timeFormatter) }.getOrNull()
    }

    private companion object {
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
