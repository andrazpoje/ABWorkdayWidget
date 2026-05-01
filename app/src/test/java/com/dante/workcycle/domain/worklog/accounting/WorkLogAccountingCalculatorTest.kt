package com.dante.workcycle.domain.worklog.accounting

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.domain.worklog.WorkLogDaySessionMode
import com.dante.workcycle.domain.worklog.WorkLogSessionStateResolver
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkLogAccountingCalculatorTest {

    @Test
    fun unpaidBreakMatchesCurrentEffectiveWorkAccounting() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 480
            ),
            now = time(17, 0)
        )

        assertEquals(480L, summary.presenceMinutes)
        assertEquals(450L, summary.effectiveWorkMinutes)
        assertEquals(30L, summary.actualBreakMinutes)
        assertEquals(0L, summary.paidBreakMinutes)
        assertEquals(450L, summary.creditedWorkMinutes)
        assertEquals(-30L, summary.balanceMinutes)
    }

    @Test
    fun fullyPaidBreakCreditsPresenceTime() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.FULLY_PAID,
                dailyTargetMinutes = 480
            ),
            now = time(17, 0)
        )

        assertEquals(480L, summary.presenceMinutes)
        assertEquals(450L, summary.effectiveWorkMinutes)
        assertEquals(30L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(480L, summary.creditedWorkMinutes)
        assertEquals(0L, summary.balanceMinutes)
    }

    @Test
    fun paidAllowanceCreditsBreakUpToAllowance() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )

        assertEquals(30L, summary.paidBreakAllowanceMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(0L, summary.excessBreakMinutes)
        assertEquals(480L, summary.creditedWorkMinutes)
        assertEquals(0L, summary.balanceMinutes)
    }

    @Test
    fun paidAllowanceLeavesExcessBreakUncredited() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 45),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )

        assertEquals(435L, summary.effectiveWorkMinutes)
        assertEquals(45L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(15L, summary.excessBreakMinutes)
        assertEquals(465L, summary.creditedWorkMinutes)
        assertEquals(-15L, summary.balanceMinutes)
    }

    @Test
    fun paidAllowanceCreditsOnlyActualBreakWhenBreakIsShorterThanAllowance() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 20),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )

        assertEquals(460L, summary.effectiveWorkMinutes)
        assertEquals(20L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakAllowanceMinutes)
        assertEquals(20L, summary.paidBreakMinutes)
        assertEquals(0L, summary.excessBreakMinutes)
        assertEquals(480L, summary.creditedWorkMinutes)
        assertEquals(0L, summary.balanceMinutes)
    }

    @Test
    fun proportionalAllowanceRoundsUpFromDailyTarget() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(
                clockOutHour = 14,
                clockOutMinute = 0,
                breakEndHour = 12,
                breakEndMinute = 30
            ),
            rules = paidAllowanceRules(dailyTargetMinutes = 360),
            now = time(15, 0)
        )

        assertEquals(23L, summary.paidBreakAllowanceMinutes)
    }

    @Test
    fun paidAllowanceIsZeroWhenBasisIsUnderThreshold() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(
                clockOutHour = 12,
                clockOutMinute = 0,
                breakEndHour = 10,
                breakEndMinute = 30
            ),
            rules = paidAllowanceRules(dailyTargetMinutes = 239),
            now = time(13, 0)
        )

        assertEquals(0L, summary.paidBreakAllowanceMinutes)
        assertEquals(0L, summary.paidBreakMinutes)
    }

    @Test
    fun activeWorkUsesFixedNowForPresenceAndEffectiveWork() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = listOf(event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN)),
            rules = WorkLogAccountingRules(dailyTargetMinutes = 480),
            now = time(10, 30)
        )

        assertEquals(150L, summary.presenceMinutes)
        assertEquals(150L, summary.effectiveWorkMinutes)
        assertEquals(150L, summary.creditedWorkMinutes)
        assertEquals(-330L, summary.balanceMinutes)
    }

    @Test
    fun activeBreakUsesFixedNowForPresenceAndActualBreak() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.BREAK_START)
            ),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(10, 30)
        )

        assertEquals(150L, summary.presenceMinutes)
        assertEquals(120L, summary.effectiveWorkMinutes)
        assertEquals(30L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(150L, summary.creditedWorkMinutes)
    }

    @Test
    fun finishedDayUsesFixedFinalTotals() {
        val summary = WorkLogAccountingCalculator.calculate(
            events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(23, 0)
        )

        assertEquals(480L, summary.presenceMinutes)
        assertEquals(450L, summary.effectiveWorkMinutes)
        assertEquals(480L, summary.creditedWorkMinutes)
        assertEquals(0L, summary.balanceMinutes)
    }

    @Test
    fun unpaidFinishedTwoSessionsCreditsOnlyEffectiveWork() {
        val summary = calculateMultipleSessions(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 14, minute = 0, type = WorkEventType.CLOCK_OUT)
            ),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 480
            ),
            now = time(15, 0)
        )

        assertEquals(360L, summary.presenceMinutes)
        assertEquals(240L, summary.effectiveWorkMinutes)
        assertEquals(0L, summary.actualBreakMinutes)
        assertEquals(0L, summary.paidBreakMinutes)
        assertEquals(240L, summary.creditedWorkMinutes)
        assertEquals(-240L, summary.balanceMinutes)
    }

    @Test
    fun fullyPaidMultiSessionCreditsBreaksButNotOffSessionGap() {
        val summary = calculateMultipleSessions(
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

        assertEquals(360L, summary.presenceMinutes)
        assertEquals(210L, summary.effectiveWorkMinutes)
        assertEquals(30L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(240L, summary.creditedWorkMinutes)
        assertEquals(0L, summary.balanceMinutes)
    }

    @Test
    fun paidAllowanceCapsTotalBreaksAcrossSessionsPerDay() {
        val summary = calculateMultipleSessions(
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
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
                dailyTargetMinutes = 240,
                paidBreakBaseMinutesAt8h = 30,
                paidBreakProportionalEnabled = false,
                paidBreakMinimumThresholdMinutes = 0,
                allowanceBasis = BreakAllowanceBasis.DAILY_TARGET
            ),
            now = time(15, 0)
        )

        assertEquals(195L, summary.effectiveWorkMinutes)
        assertEquals(45L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakAllowanceMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(15L, summary.excessBreakMinutes)
        assertEquals(225L, summary.creditedWorkMinutes)
        assertEquals(-15L, summary.balanceMinutes)
    }

    @Test
    fun activeSecondSessionUsesLiveEffectiveWorkAndDaySpanPresence() {
        val summary = calculateMultipleSessions(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN)
            ),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = 480
            ),
            now = time(13, 30)
        )

        assertEquals(330L, summary.presenceMinutes)
        assertEquals(210L, summary.effectiveWorkMinutes)
        assertEquals(0L, summary.actualBreakMinutes)
        assertEquals(210L, summary.creditedWorkMinutes)
        assertEquals(-270L, summary.balanceMinutes)
    }

    @Test
    fun activeBreakInSecondSessionCountsBreakUntilNow() {
        val summary = calculateMultipleSessions(
            events = listOf(
                event(id = 1, hour = 8, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 2, hour = 10, minute = 0, type = WorkEventType.CLOCK_OUT),
                event(id = 3, hour = 12, minute = 0, type = WorkEventType.CLOCK_IN),
                event(id = 4, hour = 13, minute = 0, type = WorkEventType.BREAK_START)
            ),
            rules = WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
                dailyTargetMinutes = 240,
                paidBreakBaseMinutesAt8h = 30,
                paidBreakProportionalEnabled = false,
                paidBreakMinimumThresholdMinutes = 0,
                allowanceBasis = BreakAllowanceBasis.DAILY_TARGET
            ),
            now = time(13, 30)
        )

        assertEquals(330L, summary.presenceMinutes)
        assertEquals(180L, summary.effectiveWorkMinutes)
        assertEquals(30L, summary.actualBreakMinutes)
        assertEquals(30L, summary.paidBreakMinutes)
        assertEquals(210L, summary.creditedWorkMinutes)
        assertEquals(-30L, summary.balanceMinutes)
    }

    @Test
    fun emptySessionsStatePathMatchesEventBasedSingleSessionAccounting() {
        val events = finishedDayWithBreak(breakEndHour = 12, breakEndMinute = 30)
        val fromEvents = WorkLogAccountingCalculator.calculate(
            events = events,
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )
        val fromSingleSessionState = WorkLogAccountingCalculator.calculate(
            sessionState = WorkLogSessionStateResolver.resolve(events, now = time(17, 0)),
            rules = paidAllowanceRules(dailyTargetMinutes = 480),
            now = time(17, 0)
        )

        assertEquals(fromEvents, fromSingleSessionState)
    }

    private fun paidAllowanceRules(dailyTargetMinutes: Int): WorkLogAccountingRules {
        return WorkLogAccountingRules(
            breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
            dailyTargetMinutes = dailyTargetMinutes,
            paidBreakBaseMinutesAt8h = 30,
            paidBreakProportionalEnabled = true,
            paidBreakMinimumThresholdMinutes = 240,
            allowanceBasis = BreakAllowanceBasis.DAILY_TARGET
        )
    }

    private fun calculateMultipleSessions(
        events: List<WorkEvent>,
        rules: WorkLogAccountingRules,
        now: LocalTime
    ): WorkLogAccountingSummary {
        val sessionState = WorkLogSessionStateResolver.resolve(
            events = events,
            now = now,
            sessionMode = WorkLogDaySessionMode.MULTIPLE_SESSIONS_PER_DAY
        )

        return WorkLogAccountingCalculator.calculate(
            sessionState = sessionState,
            rules = rules,
            now = now
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
