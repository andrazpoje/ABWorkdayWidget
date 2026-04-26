package com.dante.workcycle.ui.worklog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.data.prefs.WorkSessionPrefs
import com.dante.workcycle.data.repository.WorkEventRepository
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.model.WorkEventType.BREAK_END
import com.dante.workcycle.domain.model.WorkEventType.BREAK_START
import com.dante.workcycle.domain.model.WorkEventType.CLOCK_IN
import com.dante.workcycle.domain.model.WorkEventType.CLOCK_OUT
import com.dante.workcycle.domain.model.WorkEventType.MEAL
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher
import com.dante.workcycle.worklog.notification.WorkLogNotificationManager
import com.dante.workcycle.worklog.notification.WorkLogNotificationState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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
    private var actionLockRefreshJob: Job? = null
    private var tickerJob: Job? = null
    private val notificationManager = WorkLogNotificationManager(application)
    private val workSettingsPrefs = WorkSettingsPrefs(application)
    private val workSessionPrefs = WorkSessionPrefs(application)
    private val scheduleResolver = DefaultScheduleResolver(application)
    private val settingsChangeListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key in WORK_SETTINGS_KEYS) {
                emitCurrentUiState()
            }
        }

    init {
        workSettingsPrefs.registerListener(settingsChangeListener)
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
                emitCurrentUiState()
                restartTickerIfNeeded(events)
                syncPersistentNotification(events)
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
                    emitCurrentUiState()
                    delay(30_000)
                }
            }
        }
    }

    private fun isActionLocked(): Boolean {
        return System.currentTimeMillis() < actionLockUntilMillis
    }

    private fun emitCurrentUiState() {
        _uiState.value = buildUiState(currentEvents)
    }

    private fun lockActionsForMoment() {
        actionLockUntilMillis = System.currentTimeMillis() + ACTION_LOCK_DURATION_MS
        emitCurrentUiState()

        actionLockRefreshJob?.cancel()
        actionLockRefreshJob = viewModelScope.launch {
            val remainingMillis = (actionLockUntilMillis - System.currentTimeMillis())
                .coerceAtLeast(0L)
            delay(remainingMillis)
            emitCurrentUiState()
        }
    }

    fun onSliderAction() {
        if (isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()

            when (resolveSessionState(currentEvents)) {
                SessionState.NOT_WORKING -> {
                    createSessionSnapshot()
                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = CLOCK_IN
                        )
                    )
                }

                SessionState.WORKING -> {
                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = CLOCK_OUT
                        )
                    )
                    workSessionPrefs.clearSnapshot()
                }

                SessionState.ON_BREAK -> {
                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = BREAK_END
                        )
                    )
                }
            }

            refreshWorkLogWidgets()
            lockActionsForMoment()
        }
    }

    fun onBreakAction() {
        if (isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()
            when (resolveSessionState(currentEvents)) {
                SessionState.NOT_WORKING -> Unit

                SessionState.WORKING -> {
                    repository.insert(
                        WorkEvent(
                            date = selectedDate,
                            time = LocalTime.now(),
                            type = BREAK_START
                        )
                    )
                    refreshWorkLogWidgets()
                    lockActionsForMoment()
                }

                SessionState.ON_BREAK -> Unit
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
        val hasStartedToday = clockIn != null
        val hasBreakToday = events.any { it.type == BREAK_START || it.type == BREAK_END }
        val todayResolved = scheduleResolver.resolve(selectedDate)
        val todayCycleLabel = todayResolved.effectiveCycleLabel
        val sessionSnapshot = if (sessionState == SessionState.NOT_WORKING) {
            null
        } else {
            workSessionPrefs.getSnapshot()
        }
        val displayCycleLabel = sessionSnapshot?.cycleLabel ?: todayCycleLabel
        // TODO(expected-time-layers): Replace this primary-only lookup with a small resolver that
        // checks expected times in priority order:
        // 1) secondary assignment label from todayResolved.secondaryEffectiveLabel,
        // 2) primary cycle label from todayResolved.effectiveCycleLabel,
        // 3) global WorkSettingsPrefs defaults.
        // Keep sessionSnapshot as the frozen source while a session is active.
        val liveExpectedStartText = workSettingsPrefs.getExpectedStartConfig(todayCycleLabel)
            ?.takeIf { it.enabled }
            ?.startTime
        val expectedStartText = sessionSnapshot?.expectedStart ?: liveExpectedStartText
        val expectedEndConfig = workSettingsPrefs.getExpectedEndConfig(todayCycleLabel)
        val liveExpectedEndText = expectedEndConfig
            ?.takeIf { it.enabled }
            ?.endTime
        val expectedEndText = sessionSnapshot?.expectedEnd ?: liveExpectedEndText
        val dailyTargetMinutes = workSettingsPrefs.getDailyTargetMinutes()
        val overtimeTrackingEnabled = workSettingsPrefs.isOvertimeTrackingEnabled()
        val breakStartedAtText = breakStart?.time?.format(timeFormatter()) ?: WORK_LOG_PLACEHOLDER
        val breakDurationText = if (sessionState == SessionState.ON_BREAK && breakStart != null) {
            formatDuration(minutesBetween(breakStart.time, LocalTime.now()))
        } else {
            WORK_LOG_PLACEHOLDER
        }
        val formatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.getDefault())

        val lastClockOut = findLastClockOut(events)
        val hasAnyEvents = events.isNotEmpty()
        val startDeviation = buildStartDeviation(
            actualStartEvent = clockIn,
            expectedStart = expectedStartText
        )
        val endDeviation = buildEndDeviation(
            actualStartEvent = clockIn,
            actualEndEvent = lastClockOut,
            expectedStart = expectedStartText,
            expectedEnd = expectedEndText,
            overtimeTrackingEnabled = overtimeTrackingEnabled,
            sessionState = sessionState
        )

        val stateText: String
        val stateDetailText: String
        val sliderAction: WorkLogSliderAction
        val sliderActionText: String
        val sliderIconRes: Int

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
                sliderAction = WorkLogSliderAction.START_WORK
                sliderActionText = s(R.string.work_log_slide_start)
                sliderIconRes = R.drawable.ic_work_time_24
            }

            SessionState.WORKING -> {
                stateText = s(R.string.work_log_state_working)
                stateDetailText = s(R.string.work_log_state_working_detail)
                sliderAction = WorkLogSliderAction.FINISH_WORK
                sliderActionText = s(R.string.work_log_slide_finish)
                sliderIconRes = R.drawable.ic_save_24
            }

            SessionState.ON_BREAK -> {
                stateText = s(R.string.work_log_state_break)
                stateDetailText = s(R.string.work_log_state_break_detail)
                sliderAction = WorkLogSliderAction.END_BREAK
                sliderActionText = s(R.string.work_log_slide_end_break)
                sliderIconRes = R.drawable.ic_coffee_break_24
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

        return WorkLogDashboardUiState(
            todayText = getApplication<Application>().getString(
                R.string.work_log_date_with_cycle_label,
                selectedDate.format(formatter),
                displayCycleLabel
            ),
            stateText = stateText,
            stateDetailText = stateDetailText,
            sliderAction = sliderAction,
            sliderActionText = sliderActionText,
            sliderIconRes = sliderIconRes,
            sliderEnabled = !isActionLocked(),
            showSecondaryActions = hasStartedToday,
            showBreakActionButton = sessionState == SessionState.WORKING,
            showExpectedStart = !expectedStartText.isNullOrBlank(),
            expectedStartText = expectedStartText ?: WORK_LOG_PLACEHOLDER,
            showExpectedEnd = !expectedEndText.isNullOrBlank(),
            expectedEndText = expectedEndText ?: WORK_LOG_PLACEHOLDER,
            showStartedAt = hasStartedToday,
            startedAtText = clockIn?.time?.format(timeFormatter()) ?: WORK_LOG_PLACEHOLDER,
            showStartDeviation = startDeviation != null,
            startDeviationText = startDeviation?.text.orEmpty(),
            startDeviationTone = startDeviation?.tone ?: WorkLogDeviationTone.DEFAULT,
            showWorkedToday = hasStartedToday,
            workedTodayText = workedText,
            showTarget = hasStartedToday,
            showBalance = hasStartedToday && overtimeTrackingEnabled,
            targetWorkText = formatTargetMinutes(dailyTargetMinutes),
            balanceText = calculateBalanceText(events, dailyTargetMinutes),
            breakActionText = s(R.string.work_log_break_action_start),
            breakButtonEnabled = sessionState == SessionState.WORKING && !isActionLocked(),
            isOnBreak = sessionState == SessionState.ON_BREAK,
            showBreakInfo = hasBreakToday,
            breakStartedAtText = breakStartedAtText,
            breakDurationText = breakDurationText,
            showEndDeviation = endDeviation != null,
            endDeviationText = endDeviation?.text.orEmpty(),
            endDeviationTone = endDeviation?.tone ?: WorkLogDeviationTone.DEFAULT,
            mealActionText = mealActionText,
            mealButtonEnabled = mealButtonEnabled,
            recentEvents = events
                .takeLast(50)
                .reversed()
                .map { event ->
                    WorkEventListItem(
                        event = event,
                        text = formatEvent(event)
                    )
                }
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
        if (totalMinutes < 0) return WORK_LOG_PLACEHOLDER
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

        if (totalMinutes <= 0L) return WORK_LOG_PLACEHOLDER

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
        val dateText = event.date.format(DateTimeFormatter.ofPattern("dd.MM."))
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

    private fun calculateBalanceText(events: List<WorkEvent>, dailyTargetMinutes: Int): String {
        val targetMinutes = dailyTargetMinutes.toLong()
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
            val absMinutes = kotlin.math.abs(diff).toInt()
            "$sign${formatTargetMinutes(absMinutes)}"
        }
    }

    private fun formatTargetMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

    private fun calculateWorkedMinutes(events: List<WorkEvent>): Long {
        val sorted = events.sortedBy { it.time }

        var currentStart: LocalTime? = null
        var totalMinutes = 0L

        for (event in sorted) {
            when (event.type) {
                CLOCK_IN -> currentStart = event.time

                BREAK_START -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                BREAK_END -> currentStart = event.time

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
                CLOCK_IN -> currentStart = event.time

                BREAK_START -> {
                    if (currentStart != null) {
                        totalMinutes += minutesBetween(currentStart, event.time)
                        currentStart = null
                    }
                }

                BREAK_END -> currentStart = event.time

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

    private fun buildStartDeviation(
        actualStartEvent: WorkEvent?,
        expectedStart: String?
    ): DeviationInfo? {
        val parsedExpectedStart = parseExpectedTime(expectedStart) ?: return null
        val startEvent = actualStartEvent ?: return null
        val expectedStartDateTime = buildExpectedStartDateTime(
            sessionDate = startEvent.date,
            expectedStart = parsedExpectedStart
        )
        val actualStartDateTime = LocalDateTime.of(startEvent.date, startEvent.time)
        val diffMinutes = computeDeviationMinutes(
            expectedDateTime = expectedStartDateTime,
            actualDateTime = actualStartDateTime
        ) ?: return null

        return when {
            diffMinutes > 0 -> {
                DeviationInfo(
                    text = getApplication<Application>().getString(
                        R.string.work_log_deviation_late_start,
                        formatSignedMinutes(diffMinutes)
                    ),
                    tone = WorkLogDeviationTone.ERROR
                )
            }

            diffMinutes < 0 -> {
                DeviationInfo(
                    text = getApplication<Application>().getString(
                        R.string.work_log_deviation_early_start,
                        formatSignedMinutes(diffMinutes)
                    ),
                    tone = WorkLogDeviationTone.ACCENT
                )
            }

            else -> {
                DeviationInfo(
                    text = s(R.string.work_log_deviation_on_time),
                    tone = WorkLogDeviationTone.DEFAULT
                )
            }
        }
    }

    private fun buildEndDeviation(
        actualStartEvent: WorkEvent?,
        actualEndEvent: WorkEvent?,
        expectedStart: String?,
        expectedEnd: String?,
        overtimeTrackingEnabled: Boolean,
        sessionState: SessionState
    ): DeviationInfo? {
        if (sessionState != SessionState.NOT_WORKING) return null

        val startEvent = actualStartEvent ?: return null
        val endEvent = actualEndEvent ?: return null
        val expectedStartTime = parseExpectedTime(expectedStart) ?: startEvent.time
        val parsedExpectedEnd = parseExpectedTime(expectedEnd) ?: return null
        val expectedEndDateTime = buildExpectedEndDateTime(
            sessionDate = startEvent.date,
            expectedStart = expectedStartTime,
            expectedEnd = parsedExpectedEnd
        )
        val actualEndDateTime = buildActualEndDateTime(
            startEvent = startEvent,
            endEvent = endEvent
        )
        val diffMinutes = computeDeviationMinutes(
            expectedDateTime = expectedEndDateTime,
            actualDateTime = actualEndDateTime
        ) ?: return null
        val actualEndText = endEvent.time.format(timeFormatter())

        return when {
            diffMinutes < 0 -> {
                DeviationInfo(
                    text = getApplication<Application>().getString(
                        R.string.work_log_deviation_actual_with_meaning,
                        actualEndText,
                        getApplication<Application>().getString(
                            R.string.work_log_deviation_early_finish_label,
                            formatSignedMinutes(diffMinutes)
                        )
                    ),
                    tone = WorkLogDeviationTone.ERROR
                )
            }

            diffMinutes > 0 && overtimeTrackingEnabled -> {
                DeviationInfo(
                    text = getApplication<Application>().getString(
                        R.string.work_log_deviation_actual_with_meaning,
                        actualEndText,
                        getApplication<Application>().getString(
                            R.string.work_log_deviation_overtime,
                            formatSignedMinutes(diffMinutes)
                        )
                    ),
                    tone = WorkLogDeviationTone.ACCENT
                )
            }

            diffMinutes > 0 -> {
                DeviationInfo(
                    text = getApplication<Application>().getString(
                        R.string.work_log_deviation_actual_with_meaning,
                        actualEndText,
                        getApplication<Application>().getString(
                            R.string.work_log_deviation_extended_work,
                            formatSignedMinutes(diffMinutes)
                        )
                    ),
                    tone = WorkLogDeviationTone.DEFAULT
                )
            }

            else -> {
                DeviationInfo(
                    text = getApplication<Application>().getString(
                        R.string.work_log_deviation_actual_with_meaning,
                        actualEndText,
                        s(R.string.work_log_deviation_on_time)
                    ),
                    tone = WorkLogDeviationTone.DEFAULT
                )
            }
        }
    }

    private fun parseExpectedTime(value: String?): LocalTime? {
        val safeValue = value?.trim().orEmpty()
        if (safeValue.isBlank() || safeValue == WORK_LOG_PLACEHOLDER) return null
        return runCatching { LocalTime.parse(safeValue, timeFormatter()) }.getOrNull()
    }

    private fun buildExpectedStartDateTime(
        sessionDate: LocalDate,
        expectedStart: LocalTime
    ): LocalDateTime {
        return LocalDateTime.of(sessionDate, expectedStart)
    }

    private fun buildExpectedEndDateTime(
        sessionDate: LocalDate,
        expectedStart: LocalTime,
        expectedEnd: LocalTime
    ): LocalDateTime {
        val base = LocalDateTime.of(sessionDate, expectedEnd)
        return if (expectedEnd.isBefore(expectedStart)) {
            base.plusDays(1)
        } else {
            base
        }
    }

    private fun buildActualEndDateTime(
        startEvent: WorkEvent,
        endEvent: WorkEvent
    ): LocalDateTime {
        val base = LocalDateTime.of(endEvent.date, endEvent.time)
        return if (endEvent.date == startEvent.date && endEvent.time.isBefore(startEvent.time)) {
            base.plusDays(1)
        } else {
            base
        }
    }

    private fun computeDeviationMinutes(
        expectedDateTime: LocalDateTime,
        actualDateTime: LocalDateTime
    ): Int? {
        val bestMatch = listOf(
            actualDateTime.minusDays(1),
            actualDateTime,
            actualDateTime.plusDays(1)
        ).minByOrNull { candidate ->
            kotlin.math.abs(ChronoUnit.MINUTES.between(expectedDateTime, candidate))
        } ?: return null

        val diffMinutes = ChronoUnit.MINUTES.between(expectedDateTime, bestMatch).toInt()
        return if (kotlin.math.abs(diffMinutes) > MAX_DEVIATION_MINUTES) {
            null
        } else {
            diffMinutes
        }
    }

    private fun formatSignedMinutes(diffMinutes: Int): String {
        val sign = if (diffMinutes > 0) "+" else "-"
        return getApplication<Application>().getString(
            R.string.work_log_deviation_minutes_format,
            sign,
            kotlin.math.abs(diffMinutes)
        )
    }

    private fun createSessionSnapshot() {
        val resolvedDay = scheduleResolver.resolve(selectedDate)
        val currentCycleLabel = resolvedDay.effectiveCycleLabel
        // TODO(expected-time-layers): Snapshot should eventually store the resolved expected-time
        // source too, not only the display cycle label. Future order: secondary assignment label
        // (resolvedDay.secondaryEffectiveLabel), primary cycle label, then global defaults.
        val expectedStart = workSettingsPrefs.getExpectedStartConfig(currentCycleLabel)
            ?.takeIf { it.enabled }
            ?.startTime
        val expectedEnd = workSettingsPrefs.getExpectedEndConfig(currentCycleLabel)
            ?.takeIf { it.enabled }
            ?.endTime

        workSessionPrefs.saveSnapshot(
            WorkSessionPrefs.WorkSessionSnapshot(
                cycleLabel = currentCycleLabel,
                expectedStart = expectedStart,
                expectedEnd = expectedEnd
            )
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun syncPersistentNotification(events: List<WorkEvent>) {
        when (resolveSessionState(events)) {
            SessionState.NOT_WORKING -> {
                notificationManager.remove()
            }

            SessionState.WORKING -> {
                val clockIn = events.firstOrNull { it.type == CLOCK_IN }
                val workedText = calculateWorkedTodayText(events)

                notificationManager.showOrUpdate(
                    WorkLogNotificationState(
                        status = WorkLogNotificationState.Status.WORKING,
                        title = s(R.string.work_log_notification_title),
                        summaryText = s(R.string.work_log_state_working_detail),
                        startTimeText = clockIn?.time?.format(timeFormatter())?.let {
                            getApplication<Application>().getString(
                                R.string.work_log_notification_started_at,
                                it
                            )
                        },
                        workedTodayText = workedText
                            .takeUnless { it.isBlank() || it == WORK_LOG_PLACEHOLDER }
                            ?.let {
                                getApplication<Application>().getString(
                                    R.string.work_log_notification_worked_today,
                                    it
                                )
                            },
                        breakText = null
                    )
                )
            }

            SessionState.ON_BREAK -> {
                val clockIn = events.firstOrNull { it.type == CLOCK_IN }
                val breakStart = findActiveBreakStart(events)
                val workedText = calculateWorkedTodayText(events)
                val breakDurationText = breakStart?.let {
                    formatDuration(minutesBetween(it.time, LocalTime.now()))
                }

                notificationManager.showOrUpdate(
                    WorkLogNotificationState(
                        status = WorkLogNotificationState.Status.ON_BREAK,
                        title = s(R.string.work_log_notification_title),
                        summaryText = s(R.string.work_log_state_break_detail),
                        startTimeText = clockIn?.time?.format(timeFormatter())?.let {
                            getApplication<Application>().getString(
                                R.string.work_log_notification_started_at,
                                it
                            )
                        },
                        workedTodayText = workedText
                            .takeUnless { it.isBlank() || it == WORK_LOG_PLACEHOLDER }
                            ?.let {
                                getApplication<Application>().getString(
                                    R.string.work_log_notification_worked_today,
                                    it
                                )
                            },
                        breakText = breakDurationText?.let {
                            getApplication<Application>().getString(
                                R.string.work_log_notification_break,
                                it
                            )
                        }
                    )
                )
            }
        }
    }

    private fun refreshWorkLogWidgets() {
        WidgetRefreshDispatcher.refreshWorkLogWidgets(getApplication())
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

    private data class DeviationInfo(
        val text: String,
        val tone: WorkLogDeviationTone
    )

    override fun onCleared() {
        workSettingsPrefs.unregisterListener(settingsChangeListener)
        super.onCleared()
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

    private companion object {
        const val ACTION_LOCK_DURATION_MS = 800L
        const val MAX_DEVIATION_MINUTES = 18 * 60
        val WORK_SETTINGS_KEYS = setOf(
            WorkSettingsPrefs.KEY_DAILY_TARGET_MINUTES,
            WorkSettingsPrefs.KEY_DEFAULT_BREAK_MINUTES,
            WorkSettingsPrefs.KEY_OVERTIME_TRACKING_ENABLED,
            WorkSettingsPrefs.KEY_EXPECTED_STARTS_BY_LABEL
        )
    }
}
