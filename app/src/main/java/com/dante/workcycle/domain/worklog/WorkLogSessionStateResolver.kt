package com.dante.workcycle.domain.worklog

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalTime

/**
 * Resolves Work Log day/session state from the ordered event timeline.
 *
 * This resolver is the v3.0 foundation for making dashboard, validators,
 * widgets, totals, and future reminders share one interpretation of Work Log
 * events. It currently preserves the existing one-completed-session-per-day
 * model: once a valid CLOCK_OUT finishes the day, Start is not available again.
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
    val requiresLiveTick: Boolean
)

enum class WorkLogSessionStatus {
    NOT_STARTED,
    WORKING,
    ON_BREAK,
    FINISHED
}
