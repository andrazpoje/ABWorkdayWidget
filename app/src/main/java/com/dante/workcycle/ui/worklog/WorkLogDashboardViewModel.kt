package com.dante.workcycle.ui.worklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dante.workcycle.data.repository.WorkEventRepository
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.model.WorkEventType.BREAK_END
import com.dante.workcycle.domain.model.WorkEventType.BREAK_START
import com.dante.workcycle.domain.model.WorkEventType.CLOCK_IN
import com.dante.workcycle.domain.model.WorkEventType.CLOCK_OUT
import com.dante.workcycle.domain.model.WorkEventType.MEAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dante.workcycle.R

class WorkLogDashboardViewModel(
    application: Application,
    private val repository: WorkEventRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WorkLogDashboardUiState())
    val uiState: StateFlow<WorkLogDashboardUiState> = _uiState.asStateFlow()

    private var selectedDate: LocalDate = LocalDate.now()
    private var currentEvents: List<WorkEvent> = emptyList()
    private var observeJob: Job? = null

    private var actionLockUntilMillis: Long = 0L
    private var tickerJob: Job? = null

    init {
        observeToday()
    }

    private fun s(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun observeToday() {
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            repository.observeByDate(selectedDate).collect { events ->
                currentEvents = events
                _uiState.value = buildUiState(events)
                restartTickerIfNeeded(events)
            }
        }
    }

    private fun refreshDateIfNeeded() {
        val today = LocalDate.now()
        if (today == selectedDate) return

        selectedDate = today
        currentEvents = emptyList()
        observeToday()
    }

    private fun restartTickerIfNeeded(events: List<WorkEvent>) {
        val sessionState = resolveSessionState(events)

        tickerJob?.cancel()

        if (sessionState == SessionState.WORKING || sessionState == SessionState.ON_BREAK) {
            tickerJob = viewModelScope.launch {
                while (isActive) {
                    refreshDateIfNeeded()
                    _uiState.value = buildUiState(currentEvents)
                    delay(30_000)
                }
            }
        }
    }

    private fun isActionLocked(): Boolean {
        return System.currentTimeMillis() < actionLockUntilMillis
    }

    private fun lockActionsForMoment() {
        actionLockUntilMillis = System.currentTimeMillis() + 800L
    }

    fun onPrimaryAction() {
        if (isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()
            val sessionState = resolveSessionState(currentEvents)
            val lastEvent = currentEvents.maxByOrNull { it.time }

            when (sessionState) {
                SessionState.NOT_WORKING -> {
                    if (!canRunActionAfterOneMinute(lastEvent)) {
                        blockWithOneMinuteMessage()
                        return@launch
                    }

                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = CLOCK_IN
                        )
                    )
                    lockActionsForMoment()
                }

                SessionState.WORKING,
                SessionState.ON_BREAK -> {
                    val lastClockIn = currentEvents.lastOrNull { it.type == CLOCK_IN }
                    if (!canRunActionAfterOneMinute(lastClockIn)) {
                        blockWithOneMinuteMessage()
                        return@launch
                    }

                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = CLOCK_OUT
                        )
                    )
                    lockActionsForMoment()
                }
            }
        }
    }

    fun onBreakAction() {
        if (isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()
            when (resolveSessionState(currentEvents)) {
                SessionState.NOT_WORKING -> Unit

                SessionState.WORKING -> {
                    val lastClockIn = currentEvents.lastOrNull { it.type == CLOCK_IN }
                    if (!canRunActionAfterOneMinute(lastClockIn)) {
                        blockWithOneMinuteMessage()
                        return@launch
                    }

                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = BREAK_START
                        )
                    )
                    lockActionsForMoment()
                }

                SessionState.ON_BREAK -> {
                    val activeBreakStart = findActiveBreakStart(currentEvents)
                    if (!canRunActionAfterOneMinute(activeBreakStart)) {
                        blockWithOneMinuteMessage()
                        return@launch
                    }

                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = BREAK_END
                        )
                    )
                    lockActionsForMoment()
                }
            }
        }
    }

    fun onMealAction() {
        if (isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()
            val sessionState = resolveSessionState(currentEvents)
            val mealAlreadyLogged = hasMealLoggedToday(currentEvents)

            val canLogMeal = sessionState != SessionState.NOT_WORKING && !mealAlreadyLogged
            if (!canLogMeal) return@launch

            repository.insert(
                WorkEvent(
                    date = selectedDate,
                    time = LocalTime.now(),
                    type = MEAL
                )
            )
            lockActionsForMoment()
        }
    }

    private fun buildUiState(events: List<WorkEvent>): WorkLogDashboardUiState {
        val sessionState = resolveSessionState(events)
        val clockIn = events.firstOrNull { it.type == CLOCK_IN }
        val workedText = calculateWorkedTodayText(events)
        val breakStart = findActiveBreakStart(events)
        val breakStartedAtText = breakStart?.time?.format(timeFormatter()) ?: "—"
        val breakDurationText = if (sessionState == SessionState.ON_BREAK && breakStart != null) {
            formatDuration(minutesBetween(breakStart.time, LocalTime.now()))
        } else {
            "—"
        }
        val formatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.getDefault())

        val lastClockOut = findLastClockOut(events)
        val hasAnyEvents = events.isNotEmpty()

        val stateText: String
        val stateDetailText: String
        val primaryButtonText: String

        when (sessionState) {
            SessionState.NOT_WORKING -> {
                if (hasAnyEvents && lastClockOut != null) {
                    stateText = s(R.string.work_log_state_finished)
                    stateDetailText = getApplication<Application>().getString(
                        R.string.work_log_state_finished_detail,
                        lastClockOut.time.format(timeFormatter())
                    )
                } else {
                    stateText = s(R.string.work_log_state_off)
                    stateDetailText = s(R.string.work_log_state_off_detail)
                }
                primaryButtonText = s(R.string.work_log_action_start)
            }

            SessionState.WORKING -> {
                stateText = s(R.string.work_log_state_working)
                stateDetailText = s(R.string.work_log_state_working_detail)
                primaryButtonText = s(R.string.work_log_action_finish)
            }

            SessionState.ON_BREAK -> {
                stateText = s(R.string.work_log_state_break)
                stateDetailText = s(R.string.work_log_state_break_detail)
                primaryButtonText = s(R.string.work_log_action_finish)
            }
        }

        val mealLoggedToday = hasMealLoggedToday(events)
        val mealButtonEnabled =
            sessionState != SessionState.NOT_WORKING &&
                    !mealLoggedToday &&
                    !isActionLocked()

        val mealActionText = if (mealLoggedToday) {
            s(R.string.work_log_meal_logged_short)
        } else {
            s(R.string.work_log_meal)
        }

        val targetWorkText = "8h 00m"
        val balanceText = calculateBalanceText(events)

        return WorkLogDashboardUiState(
            todayText = selectedDate.format(formatter),
            stateText = stateText,
            stateDetailText = stateDetailText,
            startedAtText = clockIn?.time?.format(timeFormatter()) ?: "—",
            targetWorkText = targetWorkText,
            balanceText = balanceText,
            workedTodayText = workedText,
            primaryButtonText = primaryButtonText,
            breakActionText = if (sessionState == SessionState.ON_BREAK) {
                s(R.string.work_log_break_action_resume)
            } else {
                s(R.string.work_log_break_action_start)
            },
            canBreak = sessionState != SessionState.NOT_WORKING,
            breakButtonEnabled = sessionState != SessionState.NOT_WORKING && !isActionLocked(),
            isOnBreak = sessionState == SessionState.ON_BREAK,
            breakStartedAtText = breakStartedAtText,
            breakDurationText = breakDurationText,
            mealActionText = mealActionText,
            mealButtonEnabled = mealButtonEnabled,
            recentEvents = events
                .takeLast(50)
                .reversed()
                .map { formatEvent(it) }
        )
    }

    private fun findLastClockOut(events: List<WorkEvent>): WorkEvent? {
        return events
            .filter { it.type == CLOCK_OUT }
            .maxByOrNull { it.time }
    }

    private fun findActiveBreakStart(events: List<WorkEvent>): WorkEvent? {
        var currentBreakStart: WorkEvent? = null

        for (event in events.sortedBy { it.time }) {
            when (event.type) {
                BREAK_START -> currentBreakStart = event
                BREAK_END, CLOCK_OUT -> currentBreakStart = null
                else -> Unit
            }
        }

        return currentBreakStart
    }

    private fun formatDuration(totalMinutes: Long): String {
        if (totalMinutes < 0) return "—"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    private fun calculateWorkedTodayText(events: List<WorkEvent>): String {
        var currentStart: LocalTime? = null
        var breakStart: LocalTime? = null
        var totalMinutes = 0L

        for (event in events.sortedBy { it.time }) {
            when (event.type) {
                CLOCK_IN -> {
                    currentStart = event.time
                    breakStart = null
                }

                BREAK_START -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        breakStart = event.time
                        currentStart = null
                    }
                }

                BREAK_END -> {
                    if (breakStart != null) {
                        currentStart = event.time
                        breakStart = null
                    }
                }

                CLOCK_OUT -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                    breakStart = null
                }

                else -> Unit
            }
        }

        if (currentStart != null) {
            totalMinutes += minutesBetween(currentStart, LocalTime.now())
        }

        if (totalMinutes <= 0L) return "—"

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60
        }

        return (endMinutes - startMinutes).toLong()
    }

    private fun formatEvent(event: WorkEvent): String {
        val dateText = event.date.format(
            DateTimeFormatter.ofPattern("dd.MM.")
        )
        val timeText = event.time.format(timeFormatter())

        return when (event.type) {
            CLOCK_IN -> "$dateText $timeText  ${s(R.string.work_log_event_clock_in)}"

            BREAK_START -> "$dateText $timeText  ${s(R.string.work_log_event_break_start)}"

            BREAK_END -> "$dateText $timeText  ${s(R.string.work_log_event_break_end)}"

            MEAL -> "$dateText $timeText  ${s(R.string.work_log_event_meal)}"

            WorkEventType.NOTE -> {
                val noteText = event.note?.takeIf { it.isNotBlank() }
                    ?: s(R.string.work_log_event_note)

                "$dateText $timeText  ${s(R.string.work_log_event_note)}: $noteText"
            }

            CLOCK_OUT -> {
                val totalAtClockOut = calculateWorkedTextUntilEvent(currentEvents, event)
                "$dateText $timeText  ${
                    getApplication<Application>().getString(
                        R.string.work_log_event_clock_out_with_total,
                        totalAtClockOut
                    )
                }"
            }
        }
    }

    private fun calculateBalanceText(events: List<WorkEvent>): String {
        val targetMinutes = 8 * 60L
        val workedMinutes = calculateWorkedMinutes(events)
        val diff = workedMinutes - targetMinutes

        val sign = when {
            diff > 0 -> "+"
            diff < 0 -> "-"
            else -> ""
        }

        return if (diff == 0L) {
            "0h 00m"
        } else {
            val absMinutes = kotlin.math.abs(diff)
            "$sign${formatDuration(absMinutes)}"
        }
    }

    private fun calculateWorkedMinutes(events: List<WorkEvent>): Long {
        val sorted = events.sortedBy { it.time }

        var currentStart: LocalTime? = null
        var totalMinutes = 0L

        for (event in sorted) {
            when (event.type) {
                CLOCK_IN -> {
                    currentStart = event.time
                }

                BREAK_START -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                BREAK_END -> {
                    currentStart = event.time
                }

                CLOCK_OUT -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                else -> Unit
            }
        }

        return totalMinutes
    }

    private fun calculateWorkedTextUntilEvent(
        events: List<WorkEvent>,
        clockOutEvent: WorkEvent
    ): String {
        val filtered = events
            .filter { it.time <= clockOutEvent.time }
            .sortedBy { it.time }

        var currentStart: LocalTime? = null
        var totalMinutes = 0L

        for (event in filtered) {
            when (event.type) {
                CLOCK_IN -> {
                    currentStart = event.time
                }

                BREAK_START -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                BREAK_END -> {
                    currentStart = event.time
                }

                CLOCK_OUT -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                else -> Unit
            }
        }

        return formatDuration(totalMinutes)
    }

    private fun resolveSessionState(events: List<WorkEvent>): SessionState {
        var isWorking = false
        var isOnBreak = false

        for (event in events.sortedBy { it.time }) {
            when (event.type) {
                CLOCK_IN -> {
                    isWorking = true
                    isOnBreak = false
                }

                BREAK_START -> {
                    if (isWorking) isOnBreak = true
                }

                BREAK_END -> {
                    if (isWorking) isOnBreak = false
                }

                CLOCK_OUT -> {
                    isWorking = false
                    isOnBreak = false
                }

                else -> Unit
            }
        }

        return when {
            !isWorking -> SessionState.NOT_WORKING
            isOnBreak -> SessionState.ON_BREAK
            else -> SessionState.WORKING
        }
    }

    private fun timeFormatter(): DateTimeFormatter {
        return DateTimeFormatter.ofPattern("HH:mm")
    }

    private fun hasMealLoggedToday(events: List<WorkEvent>): Boolean {
        return events.any { it.type == MEAL }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun canRunActionAfterOneMinute(
        relevantEvent: WorkEvent?
    ): Boolean {
        if (relevantEvent == null) return true
        return minutesBetween(relevantEvent.time, LocalTime.now()) >= 1
    }

    private fun blockWithOneMinuteMessage() {
        _uiState.value = _uiState.value.copy(
            message = s(R.string.work_log_action_too_soon)
        )
    }

    fun onNoteAdded(note: String) {
        val trimmed = note.trim()
        if (trimmed.isBlank() || isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()
            repository.insert(
                WorkEvent(
                    date = selectedDate,
                    time = LocalTime.now(),
                    type = WorkEventType.NOTE,
                    note = trimmed
                )
            )
            lockActionsForMoment()
        }
    }

    private enum class SessionState {
        NOT_WORKING,
        WORKING,
        ON_BREAK
    }

    class Factory(
        private val application: Application,
        private val repository: WorkEventRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkLogDashboardViewModel(
                application,
                repository
            ) as T
        }
    }
}