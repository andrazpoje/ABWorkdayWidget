package com.dante.workcycle.ui.worklog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dante.workcycle.R
import com.dante.workcycle.core.status.StatusSemantics
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.data.prefs.WorkSessionPrefs
import com.dante.workcycle.data.prefs.toAccountingRules
import com.dante.workcycle.data.repository.WorkEventRepository
import com.dante.workcycle.domain.model.CycleLayer
import com.dante.workcycle.domain.model.ResolvedDay
import com.dante.workcycle.domain.model.StatusLabel
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.schedule.StatusRepository
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
import com.dante.workcycle.domain.worklog.WorkLogSessionStatus
import com.dante.workcycle.domain.worklog.accounting.BreakAccountingMode
import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingCalculator
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Builds the Work Log dashboard state from the event timeline for the selected
 * day.
 *
 * This is the current source of truth for the dashboard action state, totals,
 * balance, recent events, persistent notification, and Work Time widget refresh
 * triggers. The v2.x behavior supports one completed work session per day; once
 * a day resolves to FINISHED, the dashboard must not offer Start Work again.
 */
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
    private val statusRepository = StatusRepository(application)
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

    private fun s(resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
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

    /**
     * Applies the primary slider action for the current single-session state.
     *
     * FINISHED intentionally does nothing so a manually edited CLOCK_OUT, even
     * one moved into the future, cannot reopen Start Work in the v2.x model.
     */
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

                SessionState.FINISHED -> Unit

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

    /**
     * Starts a manual break only while the current session is actively working.
     *
     * Breaks are user-entered events; there is no automatic break timer,
     * reminder, or auto-end logic in this flow.
     */
    fun onBreakAction() {
        if (isActionLocked()) return

        viewModelScope.launch {
            refreshDateIfNeeded()
            when (resolveSessionState(currentEvents)) {
                SessionState.NOT_WORKING -> Unit
                SessionState.FINISHED -> Unit

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
            if (!WorkLogSessionStateResolver.resolve(currentEvents).canLogMeal) return@launch

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
        val now = LocalTime.now()
        val resolvedSessionState = WorkLogSessionStateResolver.resolve(events, now = now)
        val accountingSummary = WorkLogAccountingCalculator.calculate(
            sessionState = resolvedSessionState,
            rules = workSettingsPrefs.toAccountingRules(),
            now = now
        )
        val sessionState = resolvedSessionState.status.toDashboardSessionState()
        val clockIn = events.firstOrNull { it.type == CLOCK_IN }
        val workedText = formatWorkedTodayText(resolvedSessionState.workedMinutes)
        val breakStart = resolvedSessionState.activeBreakStart
        val hasStartedToday = clockIn != null
        val todayResolved = scheduleResolver.resolve(selectedDate)
        val todayCycleLabel = todayResolved.effectiveCycleLabel
        val sessionSnapshot = if (sessionState == SessionState.WORKING || sessionState == SessionState.ON_BREAK) {
            workSessionPrefs.getSnapshot()
        } else {
            null
        }
        val displayCycleLabel = sessionSnapshot?.cycleLabel ?: todayCycleLabel
        val liveExpectedTimes = resolveExpectedTimes(todayResolved)
        val liveExpectedStartText = liveExpectedTimes.startTime
        val expectedStartText = sessionSnapshot?.expectedStart ?: liveExpectedStartText
        val liveExpectedEndText = liveExpectedTimes.endTime
        val expectedEndText = sessionSnapshot?.expectedEnd ?: liveExpectedEndText
        val dailyTargetMinutes = workSettingsPrefs.getDailyTargetMinutes()
        val defaultBreakMinutes = workSettingsPrefs.getDefaultBreakMinutes()
        val breakAccountingMode = workSettingsPrefs.getBreakAccountingMode()
        val overtimeTrackingEnabled = workSettingsPrefs.isOvertimeTrackingEnabled()
        val showCreditedTime = overtimeTrackingEnabled &&
            breakAccountingMode != BreakAccountingMode.UNPAID &&
            accountingSummary.creditedWorkMinutes != accountingSummary.effectiveWorkMinutes
        val breakStartedAtText = breakStart?.time?.format(timeFormatter()) ?: WORK_LOG_PLACEHOLDER
        val breakDurationDisplay = if (sessionState == SessionState.ON_BREAK && breakStart != null) {
            resolveActiveBreakDurationDisplay(
                elapsedMinutes = minutesBetween(breakStart.time, now),
                defaultBreakMinutes = defaultBreakMinutes
            )
        } else {
            ActiveBreakDurationDisplay(
                labelText = s(R.string.work_log_break_elapsed),
                valueText = WORK_LOG_PLACEHOLDER
            )
        }
        val formatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.getDefault())

        val lastClockOut = findLastClockOut(events)
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
        val visualState: WorkLogDashboardVisualState

        when (sessionState) {
            SessionState.NOT_WORKING -> {
                stateText = s(R.string.work_log_state_off)
                stateDetailText = s(R.string.work_log_state_off_detail)
                visualState = WorkLogDashboardVisualState.NOT_STARTED
                sliderAction = WorkLogSliderAction.START_WORK
                sliderActionText = s(R.string.work_log_slide_start)
                sliderIconRes = R.drawable.ic_work_time_24
            }

            SessionState.FINISHED -> {
                stateText = s(R.string.work_log_state_finished)
                stateDetailText = getApplication<Application>().getString(
                    R.string.work_log_state_finished_detail,
                    lastClockOut?.time?.format(timeFormatter()) ?: WORK_LOG_PLACEHOLDER
                )
                visualState = WorkLogDashboardVisualState.FINISHED
                sliderAction = WorkLogSliderAction.START_WORK
                sliderActionText = s(R.string.work_log_slide_start)
                sliderIconRes = R.drawable.ic_work_time_24
            }

            SessionState.WORKING -> {
                stateText = s(R.string.work_log_state_working)
                stateDetailText = s(R.string.work_log_state_working_detail)
                visualState = WorkLogDashboardVisualState.WORKING
                sliderAction = WorkLogSliderAction.FINISH_WORK
                sliderActionText = s(R.string.work_log_slide_finish)
                sliderIconRes = R.drawable.ic_save_24
            }

            SessionState.ON_BREAK -> {
                stateText = s(R.string.work_log_state_break)
                stateDetailText = s(R.string.work_log_state_break_detail)
                visualState = WorkLogDashboardVisualState.BREAK
                sliderAction = WorkLogSliderAction.END_BREAK
                sliderActionText = s(R.string.work_log_slide_end_break)
                sliderIconRes = R.drawable.ic_coffee_break_24
            }
        }

        val mealLoggedToday = resolvedSessionState.mealLogged
        val mealButtonEnabled = resolvedSessionState.canLogMeal && !isActionLocked()

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
            visualState = visualState,
            sliderAction = sliderAction,
            sliderActionText = sliderActionText,
            sliderIconRes = sliderIconRes,
            showPrimaryAction = sessionState != SessionState.FINISHED,
            sliderEnabled = sessionState != SessionState.FINISHED && !isActionLocked(),
            startWarning = if (
                sessionState != SessionState.FINISHED &&
                sliderAction == WorkLogSliderAction.START_WORK
            ) {
                collectStartWarning(todayResolved)
            } else {
                null
            },
            showSecondaryActions = hasStartedToday,
            showBreakActionButton = resolvedSessionState.canStartBreak,
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
            balanceText = formatBalanceText(accountingSummary.balanceMinutes),
            showCreditedTime = hasStartedToday && showCreditedTime,
            creditedTimeText = formatDuration(accountingSummary.creditedWorkMinutes),
            breakActionText = s(R.string.work_log_break_action_start),
            breakButtonEnabled = resolvedSessionState.canStartBreak && !isActionLocked(),
            isOnBreak = sessionState == SessionState.ON_BREAK,
            showBreakInfo = sessionState == SessionState.ON_BREAK && breakStart != null,
            breakStartedAtText = breakStartedAtText,
            breakDurationLabelText = breakDurationDisplay.labelText,
            breakDurationText = breakDurationDisplay.valueText,
            showEndDeviation = endDeviation != null,
            endDeviationText = endDeviation?.text.orEmpty(),
            endDeviationTone = endDeviation?.tone ?: WorkLogDeviationTone.DEFAULT,
            mealActionText = mealActionText,
            mealButtonEnabled = mealButtonEnabled,
            recentEvents = events
                .takeLast(50)
                .reversed()
                .map(::buildRecentEventItem)
        )
    }

    private fun findLastClockOut(events: List<WorkEvent>): WorkEvent? {
        return WorkLogSessionStateResolver.ordered(events)
            .lastOrNull { it.type == CLOCK_OUT }
    }

    private fun formatDuration(totalMinutes: Long): String {
        if (totalMinutes < 0) return WORK_LOG_PLACEHOLDER
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    private fun formatWorkedTodayText(totalMinutes: Long): String {
        if (totalMinutes <= 0L) return WORK_LOG_PLACEHOLDER
        return formatDuration(totalMinutes)
    }

    private fun resolveActiveBreakDurationDisplay(
        elapsedMinutes: Long,
        defaultBreakMinutes: Int
    ): ActiveBreakDurationDisplay {
        if (defaultBreakMinutes <= 0) {
            return ActiveBreakDurationDisplay(
                labelText = s(R.string.work_log_break_elapsed),
                valueText = formatDuration(elapsedMinutes)
            )
        }

        val remainingMinutes = defaultBreakMinutes.toLong() - elapsedMinutes
        return if (remainingMinutes >= 0) {
            ActiveBreakDurationDisplay(
                labelText = s(R.string.work_log_break_remaining),
                valueText = formatDuration(remainingMinutes)
            )
        } else {
            ActiveBreakDurationDisplay(
                labelText = s(R.string.work_log_break_exceeded),
                valueText = formatDuration(-remainingMinutes)
            )
        }
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60
        }

        return (endMinutes - startMinutes).toLong()
    }

    private fun buildRecentEventItem(event: WorkEvent): WorkEventListItem {
        val timeText = event.time.format(timeFormatter())

        val item = when (event.type) {
            CLOCK_IN -> WorkEventListItem(
                event = event,
                timeText = timeText,
                titleText = s(R.string.work_log_event_clock_in),
                iconRes = R.drawable.ic_work_time_24
            )

            BREAK_START -> WorkEventListItem(
                event = event,
                timeText = timeText,
                titleText = s(R.string.work_log_event_break_start),
                iconRes = R.drawable.ic_coffee_break_24
            )

            BREAK_END -> WorkEventListItem(
                event = event,
                timeText = timeText,
                titleText = s(R.string.work_log_event_break_end),
                iconRes = R.drawable.ic_work_time_24
            )

            MEAL -> WorkEventListItem(
                event = event,
                timeText = timeText,
                titleText = s(R.string.work_log_event_meal),
                iconRes = R.drawable.ic_lunch_dining_24
            )

            WorkEventType.NOTE -> {
                val noteText = event.note?.takeIf { it.isNotBlank() }
                WorkEventListItem(
                    event = event,
                    timeText = timeText,
                    titleText = s(R.string.work_log_event_note),
                    detailText = noteText,
                    iconRes = R.drawable.ic_edit_note_24
                )
            }

            CLOCK_OUT -> {
                val totalAtClockOut = calculateWorkedTextUntilEvent(currentEvents, event)
                WorkEventListItem(
                    event = event,
                    timeText = timeText,
                    titleText = s(R.string.work_log_event_clock_out_label),
                    detailText = getApplication<Application>().getString(
                        R.string.work_log_event_clock_out_total_detail,
                        totalAtClockOut
                    ),
                    iconRes = R.drawable.ic_save_24
                )
            }
        }

        return applyEditAuditDisplay(item)
    }

    private fun applyEditAuditDisplay(item: WorkEventListItem): WorkEventListItem {
        val audit = item.event.editAudit ?: return item
        val details = mutableListOf<String>()

        if (!item.detailText.isNullOrBlank()) {
            details += item.detailText
        }

        details += s(
            R.string.work_log_event_edit_previous_time,
            audit.oldTime.format(timeFormatter())
        )
        details += s(
            R.string.work_log_event_edit_new_time,
            audit.newTime.format(timeFormatter())
        )
        details += s(
            R.string.work_log_event_edit_changed_at,
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(audit.editedAt),
                ZoneId.systemDefault()
            ).format(timeFormatter())
        )

        if (audit.wasFutureTime) {
            details += s(R.string.work_log_event_edit_future_saved)
        }

        return item.copy(
            editBadgeText = s(R.string.work_log_event_edited),
            detailText = details.joinToString(" • ")
        )
    }

    private fun formatBalanceText(balanceMinutes: Long): String {
        val sign = when {
            balanceMinutes > 0 -> "+"
            balanceMinutes < 0 -> "-"
            else -> ""
        }

        return if (balanceMinutes == 0L) {
            "0h 00m"
        } else {
            val absMinutes = kotlin.math.abs(balanceMinutes).toInt()
            "$sign${formatTargetMinutes(absMinutes)}"
        }
    }

    private fun formatTargetMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

    private fun calculateWorkedTextUntilEvent(
        events: List<WorkEvent>,
        clockOutEvent: WorkEvent
    ): String {
        val filtered = events
            .filter { it.time <= clockOutEvent.time }
            .let(WorkLogSessionStateResolver::ordered)

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

    /**
     * Resolves the dashboard action state from the ordered event stream.
     *
     * A valid CLOCK_OUT after CLOCK_IN/BREAK marks the day as FINISHED. Keeping
     * this resolver aligned with totals, validators, widgets, and recent events
     * is required before v3.0 can safely support multiple sessions per day.
     */
    private fun resolveSessionState(events: List<WorkEvent>): SessionState {
        return WorkLogSessionStateResolver.resolve(events).status.toDashboardSessionState()
    }

    private fun WorkLogSessionStatus.toDashboardSessionState(): SessionState {
        return when (this) {
            WorkLogSessionStatus.NOT_STARTED -> SessionState.NOT_WORKING
            WorkLogSessionStatus.WORKING -> SessionState.WORKING
            WorkLogSessionStatus.ON_BREAK -> SessionState.ON_BREAK
            WorkLogSessionStatus.FINISHED -> SessionState.FINISHED
        }
    }

    private fun timeFormatter(): DateTimeFormatter {
        return DateTimeFormatter.ofPattern("HH:mm")
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
        if (sessionState != SessionState.FINISHED) return null

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
        val expectedTimes = resolveExpectedTimes(resolvedDay)

        workSessionPrefs.saveSnapshot(
            WorkSessionPrefs.WorkSessionSnapshot(
                cycleLabel = currentCycleLabel,
                expectedStart = expectedTimes.startTime,
                expectedEnd = expectedTimes.endTime
            )
        )
    }

    private fun resolveExpectedTimes(resolvedDay: ResolvedDay): ExpectedTimes {
        return ExpectedTimes(
            startTime = resolveExpectedStartTime(resolvedDay),
            endTime = resolveExpectedEndTime(resolvedDay)
        )
    }

    private fun resolveExpectedStartTime(resolvedDay: ResolvedDay): String? {
        val secondary = resolvedDay.secondaryEffectiveLabel
            ?.trim()
            ?.ifBlank { null }
            ?.let { label ->
                workSettingsPrefs.getExpectedStartConfig(CycleLayer.SECONDARY, label)
                    ?.takeIf { it.enabled }
                    ?.startTime
            }

        if (!secondary.isNullOrBlank()) return secondary

        val primary = workSettingsPrefs.getExpectedStartConfig(CycleLayer.PRIMARY, resolvedDay.effectiveCycleLabel)
            ?.takeIf { it.enabled }
            ?.startTime

        if (!primary.isNullOrBlank()) return primary

        return if (isDefaultExpectedTimeAllowed(resolvedDay)) {
            WorkSettingsPrefs.DEFAULT_EXPECTED_START_TIME
        } else {
            null
        }
    }

    private fun resolveExpectedEndTime(resolvedDay: ResolvedDay): String? {
        val secondary = resolvedDay.secondaryEffectiveLabel
            ?.trim()
            ?.ifBlank { null }
            ?.let { label ->
                workSettingsPrefs.getExpectedEndConfig(CycleLayer.SECONDARY, label)
                    ?.takeIf { it.enabled }
                    ?.endTime
            }

        if (!secondary.isNullOrBlank()) return secondary

        val primary = workSettingsPrefs.getExpectedEndConfig(CycleLayer.PRIMARY, resolvedDay.effectiveCycleLabel)
            ?.takeIf { it.enabled }
            ?.endTime

        if (!primary.isNullOrBlank()) return primary

        return if (isDefaultExpectedTimeAllowed(resolvedDay)) {
            WorkSettingsPrefs.DEFAULT_EXPECTED_END_TIME
        } else {
            null
        }
    }

    private fun isDefaultExpectedTimeAllowed(resolvedDay: ResolvedDay): Boolean {
        val label = resolvedDay.effectiveCycleLabel.trim()
        if (label.isBlank()) return false
        if (isOffDayLabel(label)) return false

        return true
    }

    private fun isOffDayLabel(label: String): Boolean {
        val normalized = label.trim()
        if (normalized.isBlank()) return false

        return normalized.equals(getApplication<Application>().getString(R.string.off_day_label), ignoreCase = true) ||
            normalized.equals("Prosto", ignoreCase = true) ||
            normalized.equals("Off", ignoreCase = true)
    }

    fun removeStartWarningStatusesForSelectedDate() {
        refreshDateIfNeeded()

        val currentTags = statusRepository.getStatusTags(selectedDate)
        if (currentTags.isEmpty()) return

        val removableNames = resolveExclusiveNonWorkingStatusLabels(
            statusTags = currentTags,
            statusPrefs = StatusLabelsPrefs(getApplication<Application>())
        ).map { it.name.lowercase() }
            .toSet()

        if (removableNames.isEmpty()) return

        val keptTags = currentTags.filterNot { it.lowercase() in removableNames }
        statusRepository.setStatusTags(selectedDate, keptTags)
        emitCurrentUiState()
    }

    private fun collectStartWarning(resolvedDay: ResolvedDay): WorkLogStartWarning? {
        val reasons = collectStartWarningReasons(resolvedDay)
        if (reasons.isEmpty()) return null

        return WorkLogStartWarning(
            reasonText = formatStartWarningReasons(reasons.map { it.label }),
            removableStatusLabels = reasons.mapNotNull { it.removableStatusLabel }
        )
    }

    private fun collectStartWarningReasons(resolvedDay: ResolvedDay): List<StartWarningReason> {
        val reasons = mutableListOf<StartWarningReason>()
        val statusPrefs = StatusLabelsPrefs(getApplication<Application>())
        val warningStatuses = resolveExclusiveNonWorkingStatusLabels(
            statusTags = resolvedDay.statusTags,
            statusPrefs = statusPrefs
        )

        warningStatuses.forEach { label ->
            val reasonLabel = getExclusiveNonWorkingReasonLabel(label)
            reasons.add(
                StartWarningReason(
                    label = reasonLabel,
                    removableStatusLabel = reasonLabel
                )
            )
        }

        if (isOffDayLabel(resolvedDay.effectiveCycleLabel)) {
            reasons.add(
                StartWarningReason(
                    label = s(R.string.work_log_start_warning_reason_off_day)
                )
            )
        }

        return reasons
    }

    private fun resolveExclusiveNonWorkingStatusLabels(
        statusTags: Set<String>,
        statusPrefs: StatusLabelsPrefs
    ): List<StatusLabel> {
        val tagNames = statusTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .toSet()

        if (tagNames.isEmpty()) return emptyList()

        return statusPrefs.getLabels().filter { label ->
            label.name.lowercase() in tagNames &&
                StatusSemantics.isExclusiveNonWorkingStatus(label)
        }
    }

    private fun getExclusiveNonWorkingReasonLabel(label: StatusLabel): String {
        val resId = when (label.iconKey) {
            StatusSemantics.ICON_KEY_VACATION -> R.string.work_log_start_warning_reason_vacation
            StatusSemantics.ICON_KEY_SICK -> R.string.work_log_start_warning_reason_sick_leave
            else -> return StatusLabelsPrefs(getApplication<Application>()).getDisplayName(label)
        }

        return s(resId)
    }

    private fun formatStartWarningReasons(labels: List<String>): String {
        return when (labels.size) {
            0 -> ""
            1 -> labels.first()
            2 -> getApplication<Application>().getString(
                R.string.work_log_start_warning_reason_pair_format,
                labels[0],
                labels[1]
            )
            else -> {
                val separator = getApplication<Application>().getString(
                    R.string.work_log_start_warning_reason_separator
                )
                val allButLast = labels.dropLast(1).joinToString(separator = separator)
                getApplication<Application>().getString(
                    R.string.work_log_start_warning_reason_pair_format,
                    allButLast,
                    labels.last()
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun syncPersistentNotification(events: List<WorkEvent>) {
        val resolvedSessionState = WorkLogSessionStateResolver.resolve(events)

        when (resolvedSessionState.status.toDashboardSessionState()) {
            SessionState.NOT_WORKING,
            SessionState.FINISHED -> {
                notificationManager.remove()
            }

            SessionState.WORKING -> {
                val clockIn = events.firstOrNull { it.type == CLOCK_IN }
                val workedText = formatWorkedTodayText(resolvedSessionState.workedMinutes)

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
                val breakStart = resolvedSessionState.activeBreakStart
                val workedText = formatWorkedTodayText(resolvedSessionState.workedMinutes)
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
        FINISHED,
        WORKING,
        ON_BREAK
    }

    private data class DeviationInfo(
        val text: String,
        val tone: WorkLogDeviationTone
    )

    private data class ExpectedTimes(
        val startTime: String?,
        val endTime: String?
    )

    private data class ActiveBreakDurationDisplay(
        val labelText: String,
        val valueText: String
    )

    private data class StartWarningReason(
        val label: String,
        val removableStatusLabel: String? = null
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
            WorkSettingsPrefs.KEY_BREAK_ACCOUNTING_MODE,
            WorkSettingsPrefs.KEY_OVERTIME_TRACKING_ENABLED,
            WorkSettingsPrefs.KEY_EXPECTED_TIMES_BY_LAYER_AND_LABEL,
            WorkSettingsPrefs.KEY_EXPECTED_STARTS_BY_LABEL
        )
    }
}
