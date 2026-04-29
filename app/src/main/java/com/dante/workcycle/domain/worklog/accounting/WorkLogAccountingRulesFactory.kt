package com.dante.workcycle.domain.worklog.accounting

/**
 * Converts persisted Work Log settings values into accounting rules.
 *
 * This keeps policy defaults in one pure Kotlin place so UI, widgets, and
 * future onboarding/profile code can later share the same mapping without
 * depending on Android framework classes.
 */
object WorkLogAccountingRulesFactory {

    fun fromValues(
        breakAccountingMode: BreakAccountingMode,
        dailyTargetMinutes: Int,
        defaultBreakMinutes: Int
    ): WorkLogAccountingRules {
        val safeDailyTargetMinutes = dailyTargetMinutes.coerceAtLeast(0)
        val safeDefaultBreakMinutes = defaultBreakMinutes.coerceAtLeast(0)

        return when (breakAccountingMode) {
            BreakAccountingMode.UNPAID -> WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.UNPAID,
                dailyTargetMinutes = safeDailyTargetMinutes
            )

            BreakAccountingMode.FULLY_PAID -> WorkLogAccountingRules(
                breakAccountingMode = BreakAccountingMode.FULLY_PAID,
                dailyTargetMinutes = safeDailyTargetMinutes,
                paidBreakBaseMinutesAt8h = safeDefaultBreakMinutes
            )

            BreakAccountingMode.PAID_ALLOWANCE,
            BreakAccountingMode.EMPLOYER_POLICY_CUSTOM -> WorkLogAccountingRules(
                breakAccountingMode = breakAccountingMode,
                dailyTargetMinutes = safeDailyTargetMinutes,
                paidBreakBaseMinutesAt8h = safeDefaultBreakMinutes.takeIf { it > 0 } ?: 30,
                paidBreakProportionalEnabled = true,
                paidBreakMinimumThresholdMinutes = 240,
                allowanceBasis = BreakAllowanceBasis.DAILY_TARGET
            )
        }
    }
}
