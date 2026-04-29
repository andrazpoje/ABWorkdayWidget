package com.dante.workcycle.widget.worklog

import android.content.Context
import com.dante.workcycle.R
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.data.prefs.toAccountingRules
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
import com.dante.workcycle.domain.worklog.WorkLogSessionStatus
import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingCalculator
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Builds the Work Time widget display state from the same Work Event timeline
 * used by the dashboard.
 *
 * Widget totals, balance, and live-refresh requirements must stay consistent
 * with dashboard session state, recent events, and manual edit audit safety.
 * This class does not schedule refreshes; it only describes whether the
 * rendered state needs minute refresh while active.
 */
class WorkLogWidgetStateFactory(
    private val context: Context
) {

    private val repository by lazy {
        RepositoryProvider.workEventRepository(context)
    }
    private val workSettingsPrefs by lazy {
        WorkSettingsPrefs(context)
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Reads today's events and returns a safe fallback state if storage access
     * fails during widget rendering.
     */
    fun createCurrentState(): WorkLogWidgetState {
        return runCatching {
            runBlocking {
                createState(repository.getByDate(DateProvider.today()))
            }
        }.getOrElse {
            createNotStartedState()
        }
    }

    private fun createState(events: List<WorkEvent>): WorkLogWidgetState {
        val resolvedState = WorkLogSessionStateResolver.resolve(events)
        val sortedEvents = resolvedState.orderedEvents
        val balanceText = formatBalanceText(
            WorkLogAccountingCalculator.calculate(
                sessionState = resolvedState,
                rules = workSettingsPrefs.toAccountingRules()
            ).balanceMinutes
        )

        return when (resolvedState.status) {
            WorkLogSessionStatus.NOT_STARTED -> createNotStartedState()
            WorkLogSessionStatus.WORKING -> createWorkingState(sortedEvents, balanceText)
            WorkLogSessionStatus.ON_BREAK -> createOnBreakState(
                events = sortedEvents,
                activeBreakStart = resolvedState.activeBreakStart,
                balanceText = balanceText
            )
            WorkLogSessionStatus.FINISHED -> createFinishedState(sortedEvents, balanceText)
        }
    }

    private fun createNotStartedState(): WorkLogWidgetState {
        return WorkLogWidgetState(
            title = context.getString(R.string.work_log_widget_title),
            statusText = context.getString(R.string.work_log_widget_status_not_started),
            primaryValueText = context.getString(R.string.work_log_widget_no_session_today),
            secondaryValueText = context.getString(R.string.work_log_widget_tap_to_open)
        )
    }

    private fun createWorkingState(
        events: List<WorkEvent>,
        balanceText: String
    ): WorkLogWidgetState {
        val clockIn = events.firstOrNull { it.type == WorkEventType.CLOCK_IN }
        val startedAtText = context.getString(
            R.string.work_log_widget_started_at_value,
            clockIn?.time?.format(timeFormatter).orEmpty()
        )
        val isLiveMode = isLiveWidgetInfoMode()
        val workedMinutes = calculateWorkedMinutes(events)

        return WorkLogWidgetState(
            title = context.getString(R.string.work_log_widget_title),
            statusText = context.getString(R.string.work_log_state_working),
            primaryValueText = if (isLiveMode) formatWorkedTodayText(workedMinutes) else startedAtText,
            secondaryValueText = if (isLiveMode) startedAtText else formatWorkedTodayText(workedMinutes),
            tertiaryValueText = balanceText,
            requiresMinuteRefresh = isLiveMode
        )
    }

    private fun createOnBreakState(
        events: List<WorkEvent>,
        activeBreakStart: WorkEvent?,
        balanceText: String
    ): WorkLogWidgetState {
        val breakStartedText = context.getString(
            R.string.work_log_widget_break_started_value,
            activeBreakStart?.time?.format(timeFormatter).orEmpty()
        )
        val isLiveMode = isLiveWidgetInfoMode()
        val workedMinutes = calculateWorkedMinutes(events)

        return WorkLogWidgetState(
            title = context.getString(R.string.work_log_widget_title),
            statusText = context.getString(R.string.work_log_state_break),
            primaryValueText = if (isLiveMode) formatWorkedTodayText(workedMinutes) else breakStartedText,
            secondaryValueText = if (isLiveMode) breakStartedText else formatWorkedTodayText(workedMinutes),
            tertiaryValueText = balanceText,
            requiresMinuteRefresh = isLiveMode
        )
    }

    private fun createFinishedState(
        events: List<WorkEvent>,
        balanceText: String
    ): WorkLogWidgetState {
        val lastClockOut = events.lastOrNull { it.type == WorkEventType.CLOCK_OUT }
        val workedMinutes = calculateWorkedMinutes(events)

        return WorkLogWidgetState(
            title = context.getString(R.string.work_log_widget_title),
            statusText = context.getString(R.string.work_log_state_finished),
            primaryValueText = context.getString(
                R.string.work_log_widget_finished_at_value,
                lastClockOut?.time?.format(timeFormatter).orEmpty()
            ),
            secondaryValueText = formatWorkedTodayText(workedMinutes),
            tertiaryValueText = balanceText
        )
    }

    private fun calculateWorkedMinutes(events: List<WorkEvent>): Long {
        var currentStart: LocalTime? = null
        var totalMinutes = 0L

        for (event in events) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> currentStart = event.time

                WorkEventType.BREAK_START -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                WorkEventType.BREAK_END -> currentStart = event.time

                WorkEventType.CLOCK_OUT -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                else -> Unit
            }
        }

        if (currentStart != null) {
            totalMinutes += minutesBetween(currentStart, LocalTime.now())
        }

        return totalMinutes
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60
        }

        return (endMinutes - startMinutes).toLong()
    }

    private fun formatWorkedTodayText(workedMinutes: Long): String {
        return context.getString(
            R.string.work_log_widget_worked_today_value,
            formatDuration(workedMinutes)
        )
    }

    private fun isLiveWidgetInfoMode(): Boolean {
        return workSettingsPrefs.getWidgetInfoMode() ==
                WorkSettingsPrefs.WIDGET_INFO_MODE_WORKED_TODAY
    }

    private fun formatBalanceText(balanceMinutes: Long): String {
        val sign = when {
            balanceMinutes > 0L -> "+"
            balanceMinutes < 0L -> "-"
            else -> ""
        }

        val value = if (balanceMinutes == 0L) {
            "0m"
        } else {
            sign + formatCompactDuration(kotlin.math.abs(balanceMinutes))
        }

        return context.getString(R.string.work_log_widget_balance_value, value)
    }

    private fun formatCompactDuration(totalMinutes: Long): String {
        val safeMinutes = totalMinutes.coerceAtLeast(0L)
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60

        return if (hours == 0L) {
            "${minutes}m"
        } else {
            "${hours}h ${minutes.toString().padStart(2, '0')}m"
        }
    }

    private fun formatDuration(totalMinutes: Long): String {
        val safeMinutes = totalMinutes.coerceAtLeast(0L)
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60
        return "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

}
