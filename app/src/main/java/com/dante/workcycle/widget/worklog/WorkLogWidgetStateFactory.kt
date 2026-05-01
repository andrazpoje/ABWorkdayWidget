package com.dante.workcycle.widget.worklog

import android.content.Context
import com.dante.workcycle.R
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.data.prefs.toAccountingRules
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogDaySessionMode
import com.dante.workcycle.domain.worklog.WorkLogSessionState
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
import com.dante.workcycle.domain.worklog.WorkLogSessionStatus
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
        val now = LocalTime.now()
        val resolvedState = WorkLogSessionStateResolver.resolve(
            events = events,
            now = now,
            sessionMode = resolveSessionMode()
        )
        val sortedEvents = resolvedState.orderedEvents
        val balanceText = formatBalanceText(
            WorkLogWidgetBalanceCalculator.calculateBalanceMinutes(
                sessionState = resolvedState,
                rules = workSettingsPrefs.toAccountingRules(),
                now = now
            )
        )

        return when (resolvedState.status) {
            WorkLogSessionStatus.NOT_STARTED -> createNotStartedState()
            WorkLogSessionStatus.WORKING -> createWorkingState(
                clockIn = findActiveClockIn(sortedEvents, resolvedState),
                workedMinutes = resolvedState.workedMinutes,
                balanceText = balanceText
            )
            WorkLogSessionStatus.ON_BREAK -> createOnBreakState(
                activeBreakStart = resolvedState.activeBreakStart,
                workedMinutes = resolvedState.workedMinutes,
                balanceText = balanceText
            )
            WorkLogSessionStatus.FINISHED -> createFinishedState(
                events = sortedEvents,
                workedMinutes = resolvedState.workedMinutes,
                balanceText = balanceText
            )
        }
    }

    private fun findActiveClockIn(
        events: List<WorkEvent>,
        resolvedState: WorkLogSessionState
    ): WorkEvent? {
        return resolvedState.sessions.lastOrNull { it.clockOut == null }?.clockIn
            ?: events.firstOrNull { it.type == WorkEventType.CLOCK_IN }
    }

    private fun resolveSessionMode(): WorkLogDaySessionMode {
        return if (workSettingsPrefs.isMultipleWorkSessionsEnabled()) {
            WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        } else {
            WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY
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
        clockIn: WorkEvent?,
        workedMinutes: Long,
        balanceText: String
    ): WorkLogWidgetState {
        val startedAtText = context.getString(
            R.string.work_log_widget_started_at_value,
            clockIn?.time?.format(timeFormatter).orEmpty()
        )
        val isLiveMode = isLiveWidgetInfoMode()

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
        activeBreakStart: WorkEvent?,
        workedMinutes: Long,
        balanceText: String
    ): WorkLogWidgetState {
        val breakStartedText = context.getString(
            R.string.work_log_widget_break_started_value,
            activeBreakStart?.time?.format(timeFormatter).orEmpty()
        )
        val isLiveMode = isLiveWidgetInfoMode()

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
        workedMinutes: Long,
        balanceText: String
    ): WorkLogWidgetState {
        val lastClockOut = events.lastOrNull { it.type == WorkEventType.CLOCK_OUT }

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
