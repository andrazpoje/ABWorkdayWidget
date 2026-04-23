package com.dante.workcycle.widget.worklog

import android.content.Context
import com.dante.workcycle.R
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
        val sortedEvents = events.sortedBy { it.time }

        return when (resolveSessionState(sortedEvents)) {
            SessionState.NOT_STARTED -> createNotStartedState()
            SessionState.WORKING -> createWorkingState(sortedEvents)
            SessionState.ON_BREAK -> createOnBreakState(sortedEvents)
            SessionState.FINISHED -> createFinishedState(sortedEvents)
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

    private fun createWorkingState(events: List<WorkEvent>): WorkLogWidgetState {
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
            secondaryValueText = if (isLiveMode) startedAtText else null,
            tertiaryValueText = null,
            requiresMinuteRefresh = isLiveMode
        )
    }

    private fun createOnBreakState(events: List<WorkEvent>): WorkLogWidgetState {
        val breakStart = findActiveBreakStart(events)
        val breakStartedText = context.getString(
            R.string.work_log_widget_break_started_value,
            breakStart?.time?.format(timeFormatter).orEmpty()
        )
        val isLiveMode = isLiveWidgetInfoMode()
        val workedMinutes = calculateWorkedMinutes(events)

        return WorkLogWidgetState(
            title = context.getString(R.string.work_log_widget_title),
            statusText = context.getString(R.string.work_log_state_break),
            primaryValueText = if (isLiveMode) formatWorkedTodayText(workedMinutes) else breakStartedText,
            secondaryValueText = if (isLiveMode) breakStartedText else null,
            tertiaryValueText = if (isLiveMode) breakStart?.let {
                context.getString(
                    R.string.work_log_widget_break_value,
                    formatDuration(minutesBetween(it.time, LocalTime.now()))
                )
            } else null,
            requiresMinuteRefresh = isLiveMode
        )
    }

    private fun createFinishedState(events: List<WorkEvent>): WorkLogWidgetState {
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
            tertiaryValueText = null
        )
    }

    private fun resolveSessionState(events: List<WorkEvent>): SessionState {
        var isWorking = false
        var isOnBreak = false
        var hasClockOut = false

        for (event in events) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> {
                    isWorking = true
                    isOnBreak = false
                }

                WorkEventType.BREAK_START -> {
                    if (isWorking) {
                        isOnBreak = true
                    }
                }

                WorkEventType.BREAK_END -> {
                    if (isWorking) {
                        isOnBreak = false
                    }
                }

                WorkEventType.CLOCK_OUT -> {
                    isWorking = false
                    isOnBreak = false
                    hasClockOut = true
                }

                else -> Unit
            }
        }

        return when {
            isWorking && isOnBreak -> SessionState.ON_BREAK
            isWorking -> SessionState.WORKING
            hasClockOut -> SessionState.FINISHED
            else -> SessionState.NOT_STARTED
        }
    }

    private fun findActiveBreakStart(events: List<WorkEvent>): WorkEvent? {
        var currentBreakStart: WorkEvent? = null

        for (event in events) {
            when (event.type) {
                WorkEventType.BREAK_START -> currentBreakStart = event
                WorkEventType.BREAK_END, WorkEventType.CLOCK_OUT -> currentBreakStart = null
                else -> Unit
            }
        }

        return currentBreakStart
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

    private fun formatBalanceText(workedMinutes: Long): String {
        val targetMinutes = 8 * 60L
        val difference = workedMinutes - targetMinutes
        val sign = when {
            difference > 0L -> "+"
            difference < 0L -> "-"
            else -> ""
        }

        val value = if (difference == 0L) {
            "0h 00m"
        } else {
            sign + formatDuration(kotlin.math.abs(difference))
        }

        return context.getString(R.string.work_log_widget_balance_value, value)
    }

    private fun formatDuration(totalMinutes: Long): String {
        val safeMinutes = totalMinutes.coerceAtLeast(0L)
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60
        return "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

    private enum class SessionState {
        NOT_STARTED,
        WORKING,
        ON_BREAK,
        FINISHED
    }
}
