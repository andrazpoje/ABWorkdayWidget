package com.dante.workcycle.domain.worklog.accounting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkLogAccountingRulesFactoryTest {

    @Test
    fun unpaidMappingPreservesCurrentAccountingDefaults() {
        val rules = WorkLogAccountingRulesFactory.fromValues(
            breakAccountingMode = BreakAccountingMode.UNPAID,
            dailyTargetMinutes = 480,
            defaultBreakMinutes = 30
        )

        assertEquals(BreakAccountingMode.UNPAID, rules.breakAccountingMode)
        assertEquals(480, rules.dailyTargetMinutes)
        assertEquals(0, rules.paidBreakBaseMinutesAt8h)
        assertFalse(rules.paidBreakProportionalEnabled)
        assertEquals(0, rules.paidBreakMinimumThresholdMinutes)
        assertEquals(BreakAllowanceBasis.DAILY_TARGET, rules.allowanceBasis)
    }

    @Test
    fun paidAllowanceUsesDefaultBreakAsBaseAllowance() {
        val rules = WorkLogAccountingRulesFactory.fromValues(
            breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
            dailyTargetMinutes = 480,
            defaultBreakMinutes = 45
        )

        assertEquals(BreakAccountingMode.PAID_ALLOWANCE, rules.breakAccountingMode)
        assertEquals(480, rules.dailyTargetMinutes)
        assertEquals(45, rules.paidBreakBaseMinutesAt8h)
        assertTrue(rules.paidBreakProportionalEnabled)
        assertEquals(240, rules.paidBreakMinimumThresholdMinutes)
        assertEquals(BreakAllowanceBasis.DAILY_TARGET, rules.allowanceBasis)
    }

    @Test
    fun paidAllowanceFallsBackToThirtyMinutesWhenDefaultBreakIsZero() {
        val rules = WorkLogAccountingRulesFactory.fromValues(
            breakAccountingMode = BreakAccountingMode.PAID_ALLOWANCE,
            dailyTargetMinutes = 480,
            defaultBreakMinutes = 0
        )

        assertEquals(30, rules.paidBreakBaseMinutesAt8h)
    }

    @Test
    fun customPolicyCurrentlyUsesPaidAllowanceDefaults() {
        val rules = WorkLogAccountingRulesFactory.fromValues(
            breakAccountingMode = BreakAccountingMode.EMPLOYER_POLICY_CUSTOM,
            dailyTargetMinutes = 360,
            defaultBreakMinutes = 20
        )

        assertEquals(BreakAccountingMode.EMPLOYER_POLICY_CUSTOM, rules.breakAccountingMode)
        assertEquals(360, rules.dailyTargetMinutes)
        assertEquals(20, rules.paidBreakBaseMinutesAt8h)
        assertTrue(rules.paidBreakProportionalEnabled)
        assertEquals(240, rules.paidBreakMinimumThresholdMinutes)
    }

    @Test
    fun fullyPaidKeepsValidIgnoredAllowanceFields() {
        val rules = WorkLogAccountingRulesFactory.fromValues(
            breakAccountingMode = BreakAccountingMode.FULLY_PAID,
            dailyTargetMinutes = 480,
            defaultBreakMinutes = 30
        )

        assertEquals(BreakAccountingMode.FULLY_PAID, rules.breakAccountingMode)
        assertEquals(480, rules.dailyTargetMinutes)
        assertEquals(30, rules.paidBreakBaseMinutesAt8h)
        assertFalse(rules.paidBreakProportionalEnabled)
    }
}
