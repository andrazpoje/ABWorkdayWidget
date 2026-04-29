package com.dante.workcycle.widget.worklog

import com.dante.workcycle.domain.worklog.WorkLogSessionState
import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingCalculator
import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingRules
import java.time.LocalTime

/**
 * Pure widget-facing helper for Work Time balance parity.
 *
 * The widget keeps its own compact UI formatting, but balance minutes should
 * come from the shared accounting layer so the widget stays consistent with the
 * dashboard across break accounting modes.
 */
object WorkLogWidgetBalanceCalculator {

    fun calculateBalanceMinutes(
        sessionState: WorkLogSessionState,
        rules: WorkLogAccountingRules,
        now: LocalTime = LocalTime.now()
    ): Long {
        return WorkLogAccountingCalculator.calculate(
            sessionState = sessionState,
            rules = rules,
            now = now
        ).balanceMinutes
    }
}
