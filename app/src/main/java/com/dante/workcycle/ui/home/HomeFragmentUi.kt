package com.dante.workcycle.ui.home

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.schedule.parseCycleInput
import com.dante.workcycle.domain.schedule.sanitizeLabel
import com.dante.workcycle.ui.adapter.CyclePreviewAdapter
import com.dante.workcycle.ui.dialogs.EditAssignmentDayBottomSheet
import com.dante.workcycle.widget.WidgetRefreshHelper
import com.dante.workcycle.widget.WorkCycleWidgetProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.dante.workcycle.core.util.DateProvider

fun HomeFragment.setupPreviewRecyclerView() {
    previewAdapter = CyclePreviewAdapter()

    previewRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    previewRecyclerView.adapter = previewAdapter
    previewRecyclerView.isNestedScrollingEnabled = false

    previewAdapter.onItemClick = { item ->

        if (isAdded && !parentFragmentManager.isStateSaved) {

            val ctx = context
            if (ctx != null) {

                val assignmentPrefs = AssignmentCyclePrefs(ctx)

                if (assignmentPrefs.isEnabled()) {
                    EditAssignmentDayBottomSheet(
                        date = item.date,
                        onSaved = {
                            updateCyclePreview()
                            WidgetRefreshHelper.refresh(ctx)
                        }
                    ).show(parentFragmentManager, "editDay")
                } else {
                    Toast.makeText(
                        ctx,
                        getString(R.string.assignment_enable_first_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

fun HomeFragment.updateTodayStatus() = Unit

fun HomeFragment.revertToSavedState() {
    val cycle = CycleManager.loadCycle(requireContext())
    val startDate = CycleManager.loadStartDate(requireContext())

    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    val firstDay = prefs.getString(
        AppPrefs.KEY_FIRST_CYCLE_DAY,
        cycle.firstOrNull() ?: "A"
    ) ?: (cycle.firstOrNull() ?: "A")


    cycleDaysEdit.setText(cycle.joinToString(", "))
    selectedDate = startDate
    updateDateText()

    refreshFirstCycleDayDropdown(firstDay)


    switchSaturdays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
    switchSundays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
    switchHolidays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)

    updateTodayStatus()
    updateCyclePreview()

    clearUnsavedChanges()

    Toast.makeText(requireContext(), getString(R.string.reverted), Toast.LENGTH_SHORT).show()
}

fun HomeFragment.updateCyclePreview() {

    if (!isAdded) return

    val ctx = context ?: return

    val today = DateProvider.today()

    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    val resolver = DefaultScheduleResolver(ctx)

    val list = mutableListOf<CyclePreviewAdapter.PreviewItem>()

    for (offset in 0..6) {

        val date = today.plusDays(offset.toLong())

        val resolved = try {
            resolver.resolve(date)
        } catch (e: Exception) {
            null
        } ?: continue

        val title = try {
            date.format(dayFormatter).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        } catch (e: Exception) {
            ""
        }

        val offDayLabel = ctx.getString(R.string.off_day_label)
        val isOffDay = resolved.effectiveCycleLabel.equals(offDayLabel, ignoreCase = true)

        list.add(
            CyclePreviewAdapter.PreviewItem(
                date = date,
                title = title,
                dateText = date.format(dateFormatter),
                cycleLabel = resolved.effectiveCycleLabel.ifEmpty { "-" },
                colorLabel = resolved.baseCycleLabel,
                helperText = resolved.assignmentLabel,
                isOffDay = isOffDay
            )
        )
    }
    val cycle = CycleManager.loadCycle(ctx)
    previewAdapter.submitPreviewItems(list, cycle)
}
fun HomeFragment.updateWidgetHint() {
    val manager = AppWidgetManager.getInstance(requireContext())
    val ids = manager.getAppWidgetIds(
        ComponentName(requireContext(), WorkCycleWidgetProvider::class.java)
    )

    if (ids.isNotEmpty()) {
        widgetPromptContainer.visibility = View.GONE
    } else {
        widgetPromptContainer.visibility = View.VISIBLE
        widgetHint.text = getString(R.string.widget_not_added_short)
    }
}

fun HomeFragment.refreshWidget() {
    val manager = AppWidgetManager.getInstance(requireContext())
    val ids = manager.getAppWidgetIds(
        ComponentName(requireContext(), WorkCycleWidgetProvider::class.java)
    )

    WorkCycleWidgetProvider().onUpdate(requireContext(), manager, ids)
    updateWidgetHint()
}

fun HomeFragment.showDatePicker() {
    val dialog = DatePickerDialog(
        requireContext(),
        { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            updateDateText()
            clearDateCheckResult()
            markUnsavedChanges()
            updateTodayStatus()
            updateCyclePreview()
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    dialog.show()
}

fun clearDateCheckResult() = Unit

fun HomeFragment.updateDateText() {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    dateText.text = getString(
        R.string.start_date,
        selectedDate.format(formatter)
    )
}

fun HomeFragment.animateSaveButtonActivated() {
    saveButton.scaleX = 0.96f
    saveButton.scaleY = 0.96f

    saveButton.animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(180)
        .start()
}

fun HomeFragment.updateSaveButtonVisualState() {
    if (hasUnsavedChanges) {
        if (saveBarContainer.visibility != View.VISIBLE) {
            saveBarContainer.visibility = View.VISIBLE
            saveBarContainer.alpha = 0f
            saveBarContainer.animate().alpha(1f).setDuration(200).start()
        }

        saveButton.isEnabled = true
        saveButton.alpha = 1f

        revertButton.visibility = View.VISIBLE
        revertButton.alpha = 1f
    } else {
        saveButton.isEnabled = false
        saveButton.alpha = 0.6f

        revertButton.visibility = View.GONE

        if (saveBarContainer.visibility != View.GONE) {
            saveBarContainer.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    saveBarContainer.visibility = View.GONE
                    saveBarContainer.alpha = 1f
                }
                .start()
        }
    }
}

fun HomeFragment.markUnsavedChanges() {
    if (isInitializing) return

    if (!hasUnsavedChanges) {
        hasUnsavedChanges = true
        updateSaveButtonVisualState()
        animateSaveButtonActivated()
    }
}

fun HomeFragment.clearUnsavedChanges() {
    hasUnsavedChanges = false
    updateSaveButtonVisualState()
}

private fun HomeFragment.getPreviewCycle(): List<String> {
    val inputCycle = parseCycleInput(cycleDaysEdit.text?.toString().orEmpty())
    return inputCycle.ifEmpty { CycleManager.loadCycle(requireContext()) }
}

private fun HomeFragment.getPreviewFirstCycleDay(cycle: List<String>): String {
    val fallback = cycle.firstOrNull() ?: "A"
    return sanitizeLabel(
        firstCycleDayDropdown.text?.toString().orEmpty(),
        fallback
    )
}

private fun HomeFragment.getPreviewCycleDayForDate(date: LocalDate): String {
    val cycle = getPreviewCycle()
    if (cycle.isEmpty()) return "?"

    val firstDay = getPreviewFirstCycleDay(cycle)
    val startIndex = cycle.indexOfFirst { it.equals(firstDay, ignoreCase = true) }
        .takeIf { it >= 0 } ?: 0

    if (isPreviewSkippedOverrideActiveForDate(date)) {
        return getString(R.string.off_day_label)
    }

    val startDate = selectedDate

    return if (date == startDate) {
        cycle[startIndex]
    } else if (date.isAfter(startDate)) {
        val stepsForward = countPreviewIncludedDaysForward(
            fromExclusive = startDate,
            toInclusive = date
        )
        cycle[positiveModulo(startIndex + stepsForward, cycle.size)]
    } else {
        val stepsBack = countPreviewIncludedDaysBackward(
            fromInclusive = date,
            toExclusive = startDate
        )
        cycle[positiveModulo(startIndex - stepsBack, cycle.size)]
    }
}

private fun HomeFragment.isPreviewSkippedOverrideActiveForDate(date: LocalDate): Boolean {
    return isPreviewSkippedDay(date)
}

private fun HomeFragment.isPreviewSkippedDay(date: LocalDate): Boolean {
    val dayOfWeek = date.dayOfWeek.value

    if (switchSaturdays.isChecked && dayOfWeek == 6) return true
    if (switchSundays.isChecked && dayOfWeek == 7) return true
    if (switchHolidays.isChecked && HolidayManager.isHoliday(requireContext(), date)) return true

    return false
}

private fun HomeFragment.countPreviewIncludedDaysForward(
    fromExclusive: LocalDate,
    toInclusive: LocalDate
): Int {
    var count = 0
    var current = fromExclusive.plusDays(1)

    while (!current.isAfter(toInclusive)) {
        if (!isPreviewSkippedDay(current)) {
            count++
        }
        current = current.plusDays(1)
    }

    return count
}

private fun HomeFragment.countPreviewIncludedDaysBackward(
    fromInclusive: LocalDate,
    toExclusive: LocalDate
): Int {
    var count = 0
    var current = fromInclusive

    while (current.isBefore(toExclusive)) {
        if (!isPreviewSkippedDay(current)) {
            count++
        }
        current = current.plusDays(1)
    }

    return count
}

private fun positiveModulo(value: Int, mod: Int): Int {
    return ((value % mod) + mod) % mod
}