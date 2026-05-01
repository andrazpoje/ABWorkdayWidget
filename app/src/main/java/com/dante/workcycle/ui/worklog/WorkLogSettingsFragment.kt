package com.dante.workcycle.ui.worklog

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.WorkCycleApp
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.local.db.AppDatabase
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.domain.backup.WorkCycleBackupManifest
import com.dante.workcycle.domain.backup.WorkCycleBackupPayloadCollector
import com.dante.workcycle.domain.backup.WorkCycleBackupWriter
import com.dante.workcycle.domain.model.CycleLayer
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.worklog.accounting.BreakAccountingMode
import com.dante.workcycle.domain.worklog.export.WorkLogCsvExporter
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class WorkLogSettingsFragment : Fragment(R.layout.fragment_work_log_settings) {

    private lateinit var workSettingsPrefs: WorkSettingsPrefs
    private val workEventRepository by lazy {
        RepositoryProvider.workEventRepository(requireContext())
    }
    private lateinit var rowDailyTarget: View
    private lateinit var rowDefaultBreak: View
    private lateinit var rowBreakAccountingMode: View
    private lateinit var rowOvertimeTracking: View
    private lateinit var rowWidgetInfoMode: View
    private lateinit var rowExportWorkLogCsv: View
    private lateinit var rowExportFullBackup: View
    private lateinit var containerExpectedStartRows: LinearLayout
    private lateinit var textDailyTargetValue: TextView
    private lateinit var textDefaultBreakValue: TextView
    private lateinit var textBreakAccountingModeValue: TextView
    private lateinit var textOvertimeTrackingValue: TextView
    private lateinit var textWidgetInfoModeValue: TextView
    private lateinit var switchOvertimeTracking: MaterialSwitch
    private var pendingCsvExportRequest: CsvExportRequest? = null

    private var isBindingSwitch = false
    private val exportWorkLogCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val request = pendingCsvExportRequest ?: CsvExportRequest.All
        pendingCsvExportRequest = null
        if (uri != null) {
            exportWorkLogCsv(uri, request)
        }
    }
    private val exportFullBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            exportFullBackup(uri)
        }
    }

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
        rowBreakAccountingMode = view.findViewById(R.id.rowBreakAccountingMode)
        rowOvertimeTracking = view.findViewById(R.id.rowOvertimeTracking)
        rowWidgetInfoMode = view.findViewById(R.id.rowWidgetInfoMode)
        rowExportWorkLogCsv = view.findViewById(R.id.rowExportWorkLogCsv)
        rowExportFullBackup = view.findViewById(R.id.rowExportFullBackup)
        containerExpectedStartRows = view.findViewById(R.id.containerExpectedStartRows)
        textDailyTargetValue = view.findViewById(R.id.textDailyTargetValue)
        textDefaultBreakValue = view.findViewById(R.id.textDefaultBreakValue)
        textBreakAccountingModeValue = view.findViewById(R.id.textBreakAccountingModeValue)
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

        rowBreakAccountingMode.setOnClickListener {
            showBreakAccountingModeDialog()
        }

        rowOvertimeTracking.setOnClickListener {
            switchOvertimeTracking.toggle()
        }

        rowWidgetInfoMode.setOnClickListener {
            showWidgetInfoModeDialog()
        }

        rowExportWorkLogCsv.setOnClickListener {
            showWorkLogCsvExportDialog()
        }

        rowExportFullBackup.setOnClickListener {
            exportFullBackupLauncher.launch(buildFullBackupFileName())
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
        textBreakAccountingModeValue.text = getBreakAccountingModeLabel(
            workSettingsPrefs.getBreakAccountingMode()
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
                refreshWorkLogWidget()
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
                refreshWorkLogWidget()
                bindSettings()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showBreakAccountingModeDialog() {
        val modes = BreakAccountingMode.entries.toTypedArray()
        val labels = modes.map(::getBreakAccountingModeLabel).toTypedArray()
        val checkedIndex = modes.indexOf(workSettingsPrefs.getBreakAccountingMode()).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.work_log_settings_choose_break_accounting)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                workSettingsPrefs.setBreakAccountingMode(modes[which])
                refreshWorkLogWidget()
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
                refreshWorkLogWidget()
                bindSettings()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun getBreakAccountingModeLabel(mode: BreakAccountingMode): String {
        return getString(
            when (mode) {
                BreakAccountingMode.UNPAID ->
                    R.string.work_log_settings_break_accounting_unpaid
                BreakAccountingMode.FULLY_PAID ->
                    R.string.work_log_settings_break_accounting_fully_paid
                BreakAccountingMode.PAID_ALLOWANCE ->
                    R.string.work_log_settings_break_accounting_paid_allowance
                BreakAccountingMode.EMPLOYER_POLICY_CUSTOM ->
                    R.string.work_log_settings_break_accounting_custom
            }
        )
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

    private fun refreshWorkLogWidget() {
        WidgetRefreshDispatcher.refreshWorkLogWidgets(requireContext())
    }

    private fun showWorkLogCsvExportDialog() {
        val options = arrayOf(
            getString(R.string.work_log_export_all),
            getString(R.string.work_log_export_date_range)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.work_log_export_choose_range)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchAllWorkLogCsvExport()
                    1 -> showWorkLogCsvStartDatePicker()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showWorkLogCsvStartDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.work_log_export_from_date))
            .setSelection(
                LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            showWorkLogCsvEndDatePicker(startDate)
        }

        picker.show(childFragmentManager, "work_log_csv_export_start_date")
    }

    private fun showWorkLogCsvEndDatePicker(startDate: LocalDate) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.work_log_export_to_date))
            .setSelection(
                startDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val endDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            if (endDate.isBefore(startDate)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.work_log_export_invalid_date_range),
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnPositiveButtonClickListener
            }

            launchRangeWorkLogCsvExport(startDate, endDate)
        }

        picker.show(childFragmentManager, "work_log_csv_export_end_date")
    }

    private fun launchAllWorkLogCsvExport() {
        pendingCsvExportRequest = CsvExportRequest.All
        exportWorkLogCsvLauncher.launch(buildWorkLogCsvFileName())
    }

    private fun launchRangeWorkLogCsvExport(
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        pendingCsvExportRequest = CsvExportRequest.Range(
            startDate = startDate,
            endDate = endDate
        )
        exportWorkLogCsvLauncher.launch(buildWorkLogCsvFileName(startDate, endDate))
    }

    private fun buildWorkLogCsvFileName(): String {
        return "workcycle-work-log-${LocalDate.now()}.csv"
    }

    private fun buildWorkLogCsvFileName(
        startDate: LocalDate,
        endDate: LocalDate
    ): String {
        return "workcycle-work-log-${startDate}_to_${endDate}.csv"
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

    private fun exportWorkLogCsv(
        uri: Uri,
        request: CsvExportRequest
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val exportResult = runCatching {
                val events = withContext(Dispatchers.IO) {
                    when (request) {
                        CsvExportRequest.All -> workEventRepository.getAllEventsForExport()
                        is CsvExportRequest.Range -> workEventRepository.getEventsBetweenForExport(
                            startDate = request.startDate,
                            endDate = request.endDate
                        )
                    }
                }
                val csv = WorkLogCsvExporter.export(events)

                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                            writer.write(csv)
                            writer.flush()
                        }
                    } ?: error("Failed to open CSV export output stream.")
                }

                events.isEmpty()
            }

            if (exportResult.isSuccess) {
                val isEmpty = exportResult.getOrDefault(false)
                val messageRes = when {
                    isEmpty && request is CsvExportRequest.Range ->
                        R.string.work_log_export_csv_range_header_only
                    isEmpty ->
                        R.string.work_log_export_csv_header_only
                    else ->
                        R.string.work_log_export_csv_success
                }
                Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.work_log_export_csv_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportFullBackup(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val exportResult = runCatching {
                val app = requireContext().applicationContext as WorkCycleApp
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

    private companion object {
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    private sealed interface CsvExportRequest {
        data object All : CsvExportRequest

        data class Range(
            val startDate: LocalDate,
            val endDate: LocalDate
        ) : CsvExportRequest
    }
}
