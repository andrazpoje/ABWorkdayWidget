package com.dante.workcycle.domain.worklog

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalTime

/**
 * Resolves Work Log day/session state from the ordered event timeline.
 *
 * This resolver is the v3.0 foundation for making dashboard, validators,
 * widgets, totals, and future reminders share one interpretation of Work Log
 * events. The default resolve mode preserves the existing single-session-per-day
 * behavior, while multi-session resolution is available when explicitly
 * requested through [WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY].
 *
 * Events are ordered by time and then id, matching the DAO and manual-edit
 * validator semantics. Future multiple-session support should extend this
 * resolver instead of adding parallel state machines in UI or widget code.
 */
object WorkLogSessionStateResolver {

    fun resolve(
        events: List<WorkEvent>,
        now: LocalTime = LocalTime.now()
    ): WorkLogSessionState {
        return resolve(
            events = events,
            now = now,
            sessionMode = WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY
        )
    }

    fun resolve(
        events: List<WorkEvent>,
        now: LocalTime = LocalTime.now(),
        sessionMode: WorkLogDaySessionMode
    ): WorkLogSessionState {
        return when (sessionMode) {
            WorkLogDaySessionMode.SINGLE_SESSION_PER_DAY -> resolveSingleSession(events, now)
            WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY -> resolveMultipleSessions(events, now)
        }
    }

    private fun resolveSingleSession(
        events: List<WorkEvent>,
        now: LocalTime
    ): WorkLogSessionState {
        val orderedEvents = ordered(events)
        var status = WorkLogSessionStatus.NOT_STARTED
        var firstClockIn: WorkEvent? = null
        var lastClockOut: WorkEvent? = null
        var activeBreakStart: WorkEvent? = null
        var currentWorkStart: LocalTime? = null
        var workedMinutes = 0L
        var mealLogged = false

        for (event in orderedEvents) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> {
                    if (status == WorkLogSessionStatus.NOT_STARTED) {
                        firstClockIn = firstClockIn ?: event
                        currentWorkStart = event.time
                        activeBreakStart = null
                        status = WorkLogSessionStatus.WORKING
                    }
                }

                WorkEventType.BREAK_START -> {
                    if (status == WorkLogSessionStatus.WORKING) {
                        currentWorkStart?.let { start ->
                            workedMinutes += minutesBetween(start, event.time)
                        }
                        currentWorkStart = null
                        activeBreakStart = event
                        status = WorkLogSessionStatus.ON_BREAK
                    }
                }

                WorkEventType.BREAK_END -> {
                    if (status == WorkLogSessionStatus.ON_BREAK) {
                        currentWorkStart = event.time
                        activeBreakStart = null
                        status = WorkLogSessionStatus.WORKING
                    }
                }

                WorkEventType.CLOCK_OUT -> {
                    if (
                        status == WorkLogSessionStatus.WORKING ||
                        status == WorkLogSessionStatus.ON_BREAK
                    ) {
                        currentWorkStart?.let { start ->
                            workedMinutes += minutesBetween(start, event.time)
                        }
                        currentWorkStart = null
                        activeBreakStart = null
                        lastClockOut = event
                        status = WorkLogSessionStatus.FINISHED
                    }
                }

                WorkEventType.MEAL -> {
                    mealLogged = true
                }

                WorkEventType.NOTE -> Unit
            }
        }

        if (status == WorkLogSessionStatus.WORKING) {
            currentWorkStart?.let { start ->
                workedMinutes += minutesBetween(start, now)
            }
        }

        return WorkLogSessionState(
            orderedEvents = orderedEvents,
            status = status,
            firstClockIn = firstClockIn,
            lastClockOut = lastClockOut,
            activeBreakStart = activeBreakStart,
            workedMinutes = workedMinutes,
            mealLogged = mealLogged,
            canStart = status == WorkLogSessionStatus.NOT_STARTED,
            canFinish = status == WorkLogSessionStatus.WORKING,
            canStartBreak = status == WorkLogSessionStatus.WORKING,
            canEndBreak = status == WorkLogSessionStatus.ON_BREAK,
            canLogMeal = status in setOf(
                WorkLogSessionStatus.WORKING,
                WorkLogSessionStatus.ON_BREAK
            ) && !mealLogged,
            requiresLiveTick = status == WorkLogSessionStatus.WORKING ||
                status == WorkLogSessionStatus.ON_BREAK
        )
    }

    private fun resolveMultipleSessions(
        events: List<WorkEvent>,
        now: LocalTime
    ): WorkLogSessionState {
        val orderedEvents = ordered(events)
        val sessions = mutableListOf<MutableResolvedSession>()
        var activeSession: MutableResolvedSession? = null
        var firstClockIn: WorkEvent? = null
        var lastClockOut: WorkEvent? = null
        var mealLogged = false

        for (event in orderedEvents) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> {
                    if (activeSession == null) {
                        val session = MutableResolvedSession(
                            index = sessions.size + 1,
                            clockIn = event,
                            currentWorkStart = event.time
                        )
                        firstClockIn = firstClockIn ?: event
                        activeSession = session
                        sessions += session
                    }
                }

                WorkEventType.BREAK_START -> {
                    val session = activeSession
                    if (session?.status == WorkLogSessionStatus.WORKING) {
                        session.currentWorkStart?.let { start ->
                            session.workedMinutes += minutesBetween(start, event.time)
                        }
                        session.currentWorkStart = null
                        session.activeBreakStart = event
                        session.status = WorkLogSessionStatus.ON_BREAK
                        session.events += event
                    }
                }

                WorkEventType.BREAK_END -> {
                    val session = activeSession
                    if (session?.status == WorkLogSessionStatus.ON_BREAK) {
                        session.currentWorkStart = event.time
                        session.activeBreakStart = null
                        session.status = WorkLogSessionStatus.WORKING
                        session.events += event
                    }
                }

                WorkEventType.CLOCK_OUT -> {
                    val session = activeSession
                    if (
                        session?.status == WorkLogSessionStatus.WORKING ||
                        session?.status == WorkLogSessionStatus.ON_BREAK
                    ) {
                        session.currentWorkStart?.let { start ->
                            session.workedMinutes += minutesBetween(start, event.time)
                        }
                        session.currentWorkStart = null
                        session.activeBreakStart = null
                        session.clockOut = event
                        session.status = WorkLogSessionStatus.FINISHED
                        session.events += event
                        lastClockOut = event
                        activeSession = null
                    }
                }

                WorkEventType.MEAL -> {
                    mealLogged = true
                    activeSession?.events?.add(event)
                }

                WorkEventType.NOTE -> {
                    activeSession?.events?.add(event)
                }
            }
        }

        activeSession?.let { session ->
            if (session.status == WorkLogSessionStatus.WORKING) {
                session.currentWorkStart?.let { start ->
                    session.workedMinutes += minutesBetween(start, now)
                }
            }
        }

        val status = activeSession?.status
            ?: if (sessions.isNotEmpty()) {
                WorkLogSessionStatus.FINISHED
            } else {
                WorkLogSessionStatus.NOT_STARTED
            }
        val resolvedSessions = sessions.map { it.toResolvedSession() }

        return WorkLogSessionState(
            orderedEvents = orderedEvents,
            status = status,
            firstClockIn = firstClockIn,
            lastClockOut = lastClockOut,
            activeBreakStart = activeSession?.activeBreakStart,
            workedMinutes = resolvedSessions.sumOf { it.workedMinutes },
            mealLogged = mealLogged,
            canStart = activeSession == null,
            canFinish = status == WorkLogSessionStatus.WORKING,
            canStartBreak = status == WorkLogSessionStatus.WORKING,
            canEndBreak = status == WorkLogSessionStatus.ON_BREAK,
            canLogMeal = status in setOf(
                WorkLogSessionStatus.WORKING,
                WorkLogSessionStatus.ON_BREAK
            ) && !mealLogged,
            requiresLiveTick = status == WorkLogSessionStatus.WORKING ||
                status == WorkLogSessionStatus.ON_BREAK,
            sessions = resolvedSessions
        )
    }

    fun ordered(events: List<WorkEvent>): List<WorkEvent> {
        return events.sortedWith(
            compareBy<WorkEvent> { it.time }.thenBy { it.id }
        )
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60
        }

        return (endMinutes - startMinutes).toLong()
    }

    private data class MutableResolvedSession(
        val index: Int,
        val clockIn: WorkEvent,
        var clockOut: WorkEvent? = null,
        var status: WorkLogSessionStatus = WorkLogSessionStatus.WORKING,
        var activeBreakStart: WorkEvent? = null,
        var currentWorkStart: LocalTime? = null,
        var workedMinutes: Long = 0L,
        val events: MutableList<WorkEvent> = mutableListOf(clockIn)
    ) {
        fun toResolvedSession(): WorkLogResolvedSession {
            return WorkLogResolvedSession(
                index = index,
                status = status,
                clockIn = clockIn,
                clockOut = clockOut,
                activeBreakStart = activeBreakStart,
                workedMinutes = workedMinutes,
                events = events.toList()
            )
        }
    }
}

/**
 * Pure domain result describing the current Work Log day/session state.
 *
 * The model intentionally exposes derived facts instead of UI labels so it can
 * be reused by dashboard, widgets, validators, and future multi-session logic.
 */
data class WorkLogSessionState(
    val orderedEvents: List<WorkEvent>,
    val status: WorkLogSessionStatus,
    val firstClockIn: WorkEvent?,
    val lastClockOut: WorkEvent?,
    val activeBreakStart: WorkEvent?,
    val workedMinutes: Long,
    val mealLogged: Boolean,
    val canStart: Boolean,
    val canFinish: Boolean,
    val canStartBreak: Boolean,
    val canEndBreak: Boolean,
    val canLogMeal: Boolean,
    val requiresLiveTick: Boolean,
    val sessions: List<WorkLogResolvedSession> = emptyList()
)

enum class WorkLogDaySessionMode {
    SINGLE_SESSION_PER_DAY,
    MULTIPLE_SESSIONS_PER_DAY
}

data class WorkLogResolvedSession(
    val index: Int,
    val status: WorkLogSessionStatus,
    val clockIn: WorkEvent,
    val clockOut: WorkEvent?,
    val activeBreakStart: WorkEvent?,
    val workedMinutes: Long,
    val events: List<WorkEvent>
)

enum class WorkLogSessionStatus {
    NOT_STARTED,
    WORKING,
    ON_BREAK,
    FINISHED
}
