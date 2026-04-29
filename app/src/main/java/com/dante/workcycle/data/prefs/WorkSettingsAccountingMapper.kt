package com.dante.workcycle.data.prefs

import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingRules
import com.dante.workcycle.domain.worklog.accounting.WorkLogAccountingRulesFactory

fun WorkSettingsPrefs.toAccountingRules(): WorkLogAccountingRules {
    return WorkLogAccountingRulesFactory.fromValues(
        breakAccountingMode = getBreakAccountingMode(),
        dailyTargetMinutes = getDailyTargetMinutes(),
        defaultBreakMinutes = getDefaultBreakMinutes()
    )
}
