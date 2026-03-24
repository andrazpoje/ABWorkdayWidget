package com.dante.abworkdaywidget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun MainActivity.setupPreviewRecyclerView() {
    previewAdapter = CyclePreviewAdapter()
    previewRecyclerView.layoutManager = LinearLayoutManager(this)
    previewRecyclerView.adapter = previewAdapter
    previewRecyclerView.isNestedScrollingEnabled = false
}

fun MainActivity.updateTodayStatus() {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    todayStatusText.text = formatDayLabel(today)

    tomorrowStatusText.text = getString(
        R.string.tomorrow_status,
        formatDayLabel(tomorrow)
    )

    val todayLabel = CycleManager.getCycleDayForDate(this, today)
    val cycle = CycleManager.loadCycle(this)

    val cardColor = CycleColorHelper.getBackgroundColor(
        context = this,
        label = todayLabel,
        cycle = cycle
    )

    animateStatusCardColor(cardColor)
    todayStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    tomorrowStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
}

private fun MainActivity.formatDayLabel(date: LocalDate): String {
    val locale = Locale.getDefault()

    val dayName = date.dayOfWeek
        .getDisplayName(java.time.format.TextStyle.SHORT, locale)
        .replaceFirstChar { it.titlecase(locale) }

    val cycleLabel = CycleManager.getCycleDayForDate(this, date)
        .trim()
        .ifBlank { "?" }

    return "$dayName $cycleLabel"
}

fun MainActivity.updateCyclePreview() {
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
            cycleLabel = CycleManager.getCycleDayForDate(this, date)
        )
    }

    val cycle = CycleManager.loadCycle(this)
    previewAdapter.submitList(items, cycle)
}

fun MainActivity.updateWidgetHint() {
    val manager = AppWidgetManager.getInstance(this)
    val ids = manager.getAppWidgetIds(
        ComponentName(this, ABWidgetProvider::class.java)
    )

    if (ids.isNotEmpty()) {
        widgetPromptContainer.visibility = View.GONE
    } else {
        widgetPromptContainer.visibility = View.VISIBLE
        widgetHint.text = getString(R.string.widget_not_added_short)
    }
}

fun MainActivity.refreshWidget() {
    val manager = AppWidgetManager.getInstance(this)
    val ids = manager.getAppWidgetIds(
        ComponentName(this, ABWidgetProvider::class.java)
    )

    ABWidgetProvider().onUpdate(this, manager, ids)
    updateWidgetHint()
}

fun MainActivity.showDatePicker() {
    val dialog = DatePickerDialog(
        this,
        { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            updateDateText()
            clearDateCheckResult()
            markUnsavedChanges()
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    dialog.show()
}

fun clearDateCheckResult() = Unit

fun MainActivity.updateDateText() {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    dateText.text = getString(
        R.string.start_date,
        selectedDate.format(formatter)
    )
}

fun MainActivity.animateStatusCardColor(toColor: Int) {
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

fun MainActivity.animateSaveButtonActivated() {
    saveButton.scaleX = 0.96f
    saveButton.scaleY = 0.96f

    saveButton.animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(180)
        .start()
}

fun MainActivity.updateSaveButtonVisualState() {
    if (hasUnsavedChanges) {
        if (saveBarContainer.visibility != View.VISIBLE) {
            saveBarContainer.visibility = View.VISIBLE
            saveBarContainer.alpha = 0f
            saveBarContainer.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }

        saveButton.isEnabled = true
        saveButton.alpha = 1f
    } else {
        saveButton.isEnabled = false
        saveButton.alpha = 0.6f

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

fun MainActivity.markUnsavedChanges() {
    if (!hasUnsavedChanges) {
        hasUnsavedChanges = true
        updateSaveButtonVisualState()
        animateSaveButtonActivated()
    }
}

fun MainActivity.clearUnsavedChanges() {
    hasUnsavedChanges = false
    updateSaveButtonVisualState()
}