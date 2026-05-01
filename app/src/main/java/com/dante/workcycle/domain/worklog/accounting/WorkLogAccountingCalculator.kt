package com.dante.workcycle.domain.worklog.accounting

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogResolvedSession
import com.dante.workcycle.domain.worklog.WorkLogSessionState
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
import com.dante.workcycle.domain.worklog.WorkLogSessionStatus
import java.time.LocalTime

/**
 * Interprets raw Work Log events through country, sector, or employer work-time
 * accounting rules.
 *
 * Raw [WorkEvent] records are not changed by this layer. It derives accounting
 * facts such as presence time, effective work, paid break allowance, and balance
 * from the same event timeline used by the dashboard and widgets. Different
 * countries and employers may treat breaks as unpaid, fully paid, or paid up to
 * an allowance, so these calculations must stay separate from event storage and
 * UI formatting.
 *
 * Meal reimbursement is intentionally not implemented here; meal events and
 * meal reimbursement rules are separate from break-time accounting. This is a
 * product accounting helper, not a legal compliance engine or legal advice.
 */
object WorkLogAccountingCalculator {

    fun calculate(
        events: List<WorkEvent>,
        rules: WorkLogAccountingRules = WorkLogAccountingRules(),
        now: LocalTime = LocalTime.now()
    ): WorkLogAccountingSummary {
        val sessionState = WorkLogSessionStateResolver.resolve(events, now = now)
        return calculate(sessionState = sessionState, rules = rules, now = now)
    }

    fun calculate(
        sessionState: WorkLogSessionState,
        rules: WorkLogAccountingRules = WorkLogAccountingRules(),
        now: LocalTime = LocalTime.now()
    ): WorkLogAccountingSummary {
        val hasResolvedSessions = sessionState.sessions.isNotEmpty()
        val effectiveWorkMinutes = if (hasResolvedSessions) {
            sessionState.sessions.sumOf { it.workedMinutes }
        } else {
            sessionState.workedMinutes
        }
        val presenceMinutes = calculatePresenceMinutes(sessionState, now)
        val actualBreakMinutes = if (hasResolvedSessions) {
            calculateActualBreakMinutesForSessions(sessionState.sessions, now)
        } else {
            calculateActualBreakMinutes(sessionState.orderedEvents, now)
        }
        val paidBreakAllowanceMinutes = calculatePaidBreakAllowanceMinutes(
            rules = rules,
            presenceMinutes = presenceMinutes
        )

        val paidBreakMinutes = when (rules.breakAccountingMode) {
            BreakAccountingMode.UNPAID -> 0L
            BreakAccountingMode.FULLY_PAID -> actualBreakMinutes
            BreakAccountingMode.PAID_ALLOWANCE,
            BreakAccountingMode.EMPLOYER_POLICY_CUSTOM -> minOf(
                actualBreakMinutes,
                paidBreakAllowanceMinutes
            )
        }

        val excessBreakMinutes = when (rules.breakAccountingMode) {
            BreakAccountingMode.PAID_ALLOWANCE,
            BreakAccountingMode.EMPLOYER_POLICY_CUSTOM -> maxOf(
                0L,
                actualBreakMinutes - paidBreakAllowanceMinutes
            )

            BreakAccountingMode.UNPAID,
            BreakAccountingMode.FULLY_PAID -> 0L
        }

        val creditedWorkMinutes = when (rules.breakAccountingMode) {
            BreakAccountingMode.FULLY_PAID -> if (hasResolvedSessions) {
                effectiveWorkMinutes + paidBreakMinutes
            } else {
                presenceMinutes
            }
            else -> effectiveWorkMinutes + paidBreakMinutes
        }

        return WorkLogAccountingSummary(
            presenceMinutes = presenceMinutes,
            effectiveWorkMinutes = effectiveWorkMinutes,
            actualBreakMinutes = actualBreakMinutes,
            paidBreakAllowanceMinutes = paidBreakAllowanceMinutes,
            paidBreakMinutes = paidBreakMinutes,
            excessBreakMinutes = excessBreakMinutes,
            creditedWorkMinutes = creditedWorkMinutes,
            balanceMinutes = creditedWorkMinutes - rules.dailyTargetMinutes.toLong()
        )
    }

    private fun calculatePresenceMinutes(
        sessionState: WorkLogSessionState,
        now: LocalTime
    ): Long {
        // Multi-session v3.0 uses a day-span presence policy: first valid
        // clock-in through last valid clock-out, or now while a session is active.
        val start = sessionState.firstClockIn?.time ?: return 0L
        val end = when (sessionState.status) {
            WorkLogSessionStatus.WORKING,
            WorkLogSessionStatus.ON_BREAK -> now

            WorkLogSessionStatus.FINISHED -> sessionState.lastClockOut?.time ?: now
            WorkLogSessionStatus.NOT_STARTED -> return 0L
        }

        return minutesBetween(start, end)
    }

    private fun calculateActualBreakMinutesForSessions(
        sessions: List<WorkLogResolvedSession>,
        now: LocalTime
    ): Long {
        return sessions.sumOf { session ->
            calculateActualBreakMinutes(session.events, now)
        }
    }

    private fun calculateActualBreakMinutes(
        orderedEvents: List<WorkEvent>,
        now: LocalTime
    ): Long {
        var status = WorkLogSessionStatus.NOT_STARTED
        var activeBreakStart: LocalTime? = null
        var actualBreakMinutes = 0L

        for (event in orderedEvents) {
            when (event.type) {
                WorkEventType.CLOCK_IN -> {
                    if (status == WorkLogSessionStatus.NOT_STARTED) {
                        status = WorkLogSessionStatus.WORKING
                    }
                }

                WorkEventType.BREAK_START -> {
                    if (status == WorkLogSessionStatus.WORKING) {
                        activeBreakStart = event.time
                        status = WorkLogSessionStatus.ON_BREAK
                    }
                }

                WorkEventType.BREAK_END -> {
                    if (status == WorkLogSessionStatus.ON_BREAK) {
                        activeBreakStart?.let { start ->
                            actualBreakMinutes += minutesBetween(start, event.time)
                        }
                        activeBreakStart = null
                        status = WorkLogSessionStatus.WORKING
                    }
                }

                WorkEventType.CLOCK_OUT -> {
                    if (status == WorkLogSessionStatus.ON_BREAK) {
                        activeBreakStart?.let { start ->
                            actualBreakMinutes += minutesBetween(start, event.time)
                        }
                        activeBreakStart = null
                        status = WorkLogSessionStatus.FINISHED
                    } else if (status == WorkLogSessionStatus.WORKING) {
                        status = WorkLogSessionStatus.FINISHED
                    }
                }

                WorkEventType.MEAL,
                WorkEventType.NOTE -> Unit
            }

            if (status == WorkLogSessionStatus.FINISHED) break
        }

        if (status == WorkLogSessionStatus.ON_BREAK) {
            activeBreakStart?.let { start ->
                actualBreakMinutes += minutesBetween(start, now)
            }
        }

        return actualBreakMinutes
    }

    private fun calculatePaidBreakAllowanceMinutes(
        rules: WorkLogAccountingRules,
        presenceMinutes: Long
    ): Long {
        if (
            rules.breakAccountingMode == BreakAccountingMode.UNPAID ||
            rules.breakAccountingMode == BreakAccountingMode.FULLY_PAID ||
            rules.paidBreakBaseMinutesAt8h <= 0
        ) {
            return 0L
        }

        val basisMinutes = when (rules.allowanceBasis) {
            BreakAllowanceBasis.DAILY_TARGET -> rules.dailyTargetMinutes.toLong()
            BreakAllowanceBasis.PRESENCE -> presenceMinutes
            BreakAllowanceBasis.SCHEDULED_SHIFT -> rules.dailyTargetMinutes.toLong()
        }

        if (basisMinutes < rules.paidBreakMinimumThresholdMinutes) return 0L

        return if (rules.paidBreakProportionalEnabled) {
            ceilDivide(rules.paidBreakBaseMinutesAt8h.toLong() * basisMinutes, MINUTES_AT_8H)
        } else {
            rules.paidBreakBaseMinutesAt8h.toLong()
        }
    }

    private fun ceilDivide(value: Long, divisor: Long): Long {
        if (value <= 0L) return 0L
        return (value + divisor - 1L) / divisor
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60
        }

        return (endMinutes - startMinutes).toLong()
    }

    private const val MINUTES_AT_8H = 480L
}

enum class BreakAccountingMode {
    UNPAID,
    FULLY_PAID,
    PAID_ALLOWANCE,
    EMPLOYER_POLICY_CUSTOM
}

enum class BreakAllowanceBasis {
    DAILY_TARGET,
    PRESENCE,
    SCHEDULED_SHIFT
}

/**
 * Configurable work-time accounting rules.
 *
 * Defaults are conservative: breaks are unpaid, the daily target is eight
 * hours, and no paid break allowance is granted unless a policy explicitly
 * enables it.
 */
data class WorkLogAccountingRules(
    val breakAccountingMode: BreakAccountingMode = BreakAccountingMode.UNPAID,
    val dailyTargetMinutes: Int = 480,
    val paidBreakBaseMinutesAt8h: Int = 0,
    val paidBreakProportionalEnabled: Boolean = false,
    val paidBreakMinimumThresholdMinutes: Int = 0,
    val allowanceBasis: BreakAllowanceBasis = BreakAllowanceBasis.DAILY_TARGET
)

data class WorkLogAccountingSummary(
    val presenceMinutes: Long,
    val effectiveWorkMinutes: Long,
    val actualBreakMinutes: Long,
    val paidBreakAllowanceMinutes: Long,
    val paidBreakMinutes: Long,
    val excessBreakMinutes: Long,
    val creditedWorkMinutes: Long,
    val balanceMinutes: Long
)
