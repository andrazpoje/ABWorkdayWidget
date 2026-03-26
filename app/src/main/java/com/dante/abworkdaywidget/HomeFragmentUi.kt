package com.dante.abworkdaywidget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dante.abworkdaywidget.util.parseCycleInput
import com.dante.abworkdaywidget.util.sanitizeLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun HomeFragment.setupPreviewRecyclerView() {
    previewAdapter = CyclePreviewAdapter()
    previewRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    previewRecyclerView.adapter = previewAdapter
    previewRecyclerView.isNestedScrollingEnabled = false
}

fun HomeFragment.updateTodayStatus() {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    todayStatusText.text = formatDayLabel(today)

    tomorrowStatusText.text = getString(
        R.string.tomorrow_status,
        formatDayLabel(tomorrow)
    )

    val cycle = getPreviewCycle()
    val todayLabel = getPreviewCycleDayForDate(today)

    val cardColor = CycleColorHelper.getBackgroundColor(
        context = requireContext(),
        label = todayLabel,
        cycle = cycle
    )

    animateStatusCardColor(cardColor)
    todayStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    tomorrowStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
}

fun HomeFragment.revertToSavedState() {
    val cycle = CycleManager.loadCycle(requireContext())
    val startDate = CycleManager.loadStartDate(requireContext())

    val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

    val firstDay = prefs.getString(
        AppPrefs.KEY_FIRST_CYCLE_DAY,
        cycle.firstOrNull() ?: "A"
    ) ?: (cycle.firstOrNull() ?: "A")

    val skippedLabel = prefs.getString(
        AppPrefs.KEY_SKIPPED_LABEL,
        AppPrefs.DEFAULT_SKIPPED_LABEL
    ) ?: AppPrefs.DEFAULT_SKIPPED_LABEL

    cycleDaysEdit.setText(cycle.joinToString(", "))
    selectedDate = startDate
    updateDateText()

    refreshFirstCycleDayDropdown(firstDay)

    skippedDayLabelEdit.setText(skippedLabel)

    switchSaturdays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SATURDAYS, true)
    switchSundays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_SUNDAYS, true)
    switchHolidays.isChecked = prefs.getBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, true)
    switchOverrideSkippedDays.isChecked = prefs.getBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)

    updateTodayStatus()
    updateCyclePreview()

    clearUnsavedChanges()

    Toast.makeText(requireContext(), getString(R.string.reverted), Toast.LENGTH_SHORT).show()
}

private fun HomeFragment.formatDayLabel(date: LocalDate): String {
    val locale = Locale.getDefault()

    val dayName = date.dayOfWeek
        .getDisplayName(java.time.format.TextStyle.SHORT, locale)
        .replaceFirstChar { it.titlecase(locale) }

    val cycleLabel = getPreviewCycleDayForDate(date)
        .trim()
        .ifBlank { "?" }

    return "$dayName $cycleLabel"
}

fun HomeFragment.updateCyclePreview() {
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    val items = (2..6).map { offset ->
        val date = today.plusDays(offset.toLong())
        val title = date.format(dayFormatter).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        CyclePreviewAdapter.PreviewItem(
            title = title,
            dateText = date.format(dateFormatter),
            cycleLabel = getPreviewCycleDayForDate(date)
        )
    }

    previewAdapter.submitList(items, getPreviewCycle())
}

fun HomeFragment.updateWidgetHint() {
    val manager = AppWidgetManager.getInstance(requireContext())
    val ids = manager.getAppWidgetIds(
        ComponentName(requireContext(), ABWidgetProvider::class.java)
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
        ComponentName(requireContext(), ABWidgetProvider::class.java)
    )

    ABWidgetProvider().onUpdate(requireContext(), manager, ids)
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

fun HomeFragment.animateStatusCardColor(toColor: Int) {
    val fromColor = statusCard.cardBackgroundColor.defaultColor

    ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
        duration = 220
        addUpdateListener { animator ->
            val animatedColor = animator.animatedValue as Int
            statusCard.setCardBackgroundColor(animatedColor)
        }
        start()
    }
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

private fun HomeFragment.getPreviewSkippedLabel(): String {
    return sanitizeLabel(
        skippedDayLabelEdit.text?.toString().orEmpty(),
        AppPrefs.DEFAULT_SKIPPED_LABEL
    )
}

private fun HomeFragment.getPreviewCycleDayForDate(date: LocalDate): String {
    val cycle = getPreviewCycle()
    if (cycle.isEmpty()) return "?"

    val firstDay = getPreviewFirstCycleDay(cycle)
    val startIndex = cycle.indexOfFirst { it.equals(firstDay, ignoreCase = true) }
        .takeIf { it >= 0 } ?: 0

    if (isPreviewSkippedOverrideActiveForDate(date)) {
        return getPreviewSkippedLabel()
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
    if (!switchOverrideSkippedDays.isChecked) return false
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