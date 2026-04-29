package com.dante.workcycle.widget.worklog

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
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

    private fun paidAllowanceRules(dailyTargetMinutes: Int): WorkLogAccountingRules {
        return WorkLogAccountingRules(
            breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
            dailyTargetMinutes = dailyTargetMinutes,
            paidBreakBaseMinutesAt8h = 30,
            paidBreakProportionalEnabled = true,
            paidBreakMinimumThresholdMinutes = 240
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
