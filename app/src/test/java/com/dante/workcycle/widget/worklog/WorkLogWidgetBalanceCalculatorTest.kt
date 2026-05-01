package com.dante.workcycle.widget.worklog

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogDaySessionMode
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
import com.dante.workcycle.domain.worklog.accounting.BreakAllowanceBasis
import com.dante.workcycle.domain.worklog.accounting.BreakAccountingMode
import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingRules
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkLogWidgetBalanceCalculatorTest {

    @Test
    fun unpaidBreakUsesEffectiveWorkBalance() {
        val balanceMinutes = calculateBalanceMinutes(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 480
            ),
            now = time(17, 0)
        )

        assertEquals(-30L, balanceMinutes)
    }

    @Test
    fun paidAllowanceCreditsBreakUpToAllowance() {
        val balanceMinutes = calculateBalanceMinutes(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )

        assertEquals(0L, balanceMinutes)
    }

    @Test
    fun paidAllowanceLeavesExcessBreakUncredited() {
        val balanceMinutes = calculateBalanceMinutes(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 45),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )

        assertEquals(-15L, balanceMinutes)
    }

    @Test
    fun fullyPaidBreakCreditsFullPresenceTime() {
        val balanceMinutes = calculateBalanceMinutes(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 45),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.FULLY_PAID,
                dailyTargetMinutes = 480
            ),
            now = time(17, 0)
        )

        assertEquals(0L, balanceMinutes)
    }

    @Test
    fun activeWorkUsesLiveAccountingResult() {
        val balanceMinutes = calculateBalanceMinutes(
            events = listOf(event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 480
            ),
            now = time(10, 30)
        )

        assertEquals(-330L, balanceMinutes)
    }

    @Test
    fun activeBreakUsesAccountingResultForConfiguredMode() {
        val balanceMinutes = calculateBalanceMinutes(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START)
            ),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(10, 30)
        )

        assertEquals(-330L, balanceMinutes)
    }

    @Test
    fun finishedDayBalanceStaysFixed() {
        val balanceMinutes = calculateBalanceMinutes(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(23, 0)
        )

        assertEquals(0L, balanceMinutes)
    }

    @Test
    fun unpaidMultiSessionBalanceUsesTotalEffectiveWork() {
        val balanceMinutes = calculateMultiSessionBalanceMinutes(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 14, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 240
            ),
            now = time(15, 0)
        )

        assertEquals(0L, balanceMinutes)
    }

    @Test
    fun fullyPaidMultiSessionCreditsBreakButNotOffSessionGap() {
        val balanceMinutes = calculateMultiSessionBalanceMinutes(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 13, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 5, hour = 13, minute = 30, type = WorkEventType.BREAK_END),
                event(id = 6, hour = 14, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.FULLY_PAID,
                dailyTargetMinutes = 240
            ),
            now = time(15, 0)
        )

        assertEquals(0L, balanceMinutes)
    }

    @Test
    fun paidAllowanceMultiSessionCapsBreaksPerDay() {
        val balanceMinutes = calculateMultiSessionBalanceMinutes(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 9, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 3, hour = 9, minute = 20, type = WorkEventType.BREAK_END),
                event(id = 4, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 5, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 6, hour = 13, minute = 0, type = WorkEventType.BREAK_START),
                event(id = 7, hour = 13, minute = 25, type = WorkEventType.BREAK_END),
                event(id = 8, hour = 14, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            rules = fixedPaidAllowanceRules(dailyTargetMinutes = 240),
            now = time(15, 0)
        )

        assertEquals(-15L, balanceMinutes)
    }

    @Test
    fun activeSecondSessionMultiSessionBalanceIsLive() {
        val balanceMinutes = calculateMultiSessionBalanceMinutes(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 240
            ),
            now = time(13, 30)
        )

        assertEquals(-30L, balanceMinutes)
    }

    private fun calculateBalanceMinutes(
        events: List<WorkEvent>,
        rules: WorkLogAccountingRules,
        now: LocalTime
    ): Long {
        val sessionState = WorkLogSessionStateResolver.resolve(events, now = now)
        return WorkLogWidgetBalanceCalculator.calculateBalanceMinutes(
            sessionState = sessionState,
            rules = rules,
            now = now
        )
    }

    private fun calculateMultiSessionBalanceMinutes(
        events: List<WorkEvent>,
        rules: WorkLogAccountingRules,
        now: LocalTime
    ): Long {
        val sessionState = WorkLogSessionStateResolver.resolve(
            events = events,
            now = now,
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        return WorkLogWidgetBalanceCalculator.calculateBalanceMinutes(
            sessionState = sessionState,
            rules = rules,
            now = now
        )
    }

    private fun paidAllowanceRules(dailyTargetMinutes: Int): WorkLogAccountingRules {
        return WorkLogAccountingRules(
            breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
            dailyTargetMinutes = dailyTargetMinutes,
            paidBreakBaseMinutesAt8h = 30,
            paidBreakProportionalEnabled = true,
            paidBreakMinimumThresholdMinutes = 240
        )
    }

    private fun fixedPaidAllowanceRules(dailyTargetMinutes: Int): WorkLogAccountingRules {
        return WorkLogAccountingRules(
            breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
            dailyTargetMinutes = dailyTargetMinutes,
            paidBreakBaseMinutesAt8h = 30,
            paidBreakProportionalEnabled = false,
            paidBreakMinimumThresholdMinutes = 0,
            allowanceBasis = BreakAllowanceBasis.DAILY_TARGET
        )
    }

    private fun finishedDayWithBreak(
        clockOutHour: Int = 16,
        clockOutMinute: Int = 0,
        breakEndHour: Int,
        breakEndMinute: Int
    ): List<WorkEvent> {
        return listOf(
            event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
            event(id = 2, hour = 12, minute = 0, type = WorkEventType.BREAK_START),
            event(id = 3, hour = breakEndHour, minute = breakEndMinute, type = WorkEventType.BREAK_END),
            event(id = 4, hour = clockOutHour, minute = clockOutMinute, type = WorkEventType.CLOCK_OUT)
        )
    }

    private fun event(
        id: Long,
        hour: Int,
        minute: Int,
        type: WorkEventType
    ): WorkEvent {
        return WorkEvent(
            id = id,
            date = DATE,
            time = time(hour, minute),
            type = type,
            createdAt = id
        )
    }

    private fun time(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour, minute)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 4, 29)
    }
}
