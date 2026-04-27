package com.dante.workcycle.domain.template

import android.content.Context
import androidx.annotation.StringRes
import java.time.LocalDate

data class ScheduleTemplate(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val fixedCycle: List<String>,
    val fixedStartDate: LocalDate,
    val fixedFirstCycleDay: String,
    val locksCycleEditing: Boolean,
    val locksRulesEditing: Boolean,
    val allowsStartDateEditing: Boolean = true,
    val allowsCycleOverrides: Boolean = true,
    val assignmentConfig: TemplateAssignmentConfig? = null,
    val skipSaturdays: Boolean = false,
    val skipSundays: Boolean = false,
    val skipHolidays: Boolean = false,
    @StringRes val fixedCycleRes: List<Int> = emptyList(),
    @StringRes val fixedFirstCycleDayRes: Int? = null
) {
    fun resolveFixedCycle(context: Context): List<String> {
        return fixedCycleRes
            .takeIf { it.isNotEmpty() }
            ?.map(context::getString)
            ?: fixedCycle
    }

    fun resolveFixedFirstCycleDay(context: Context): String {
        return fixedFirstCycleDayRes?.let(context::getString) ?: fixedFirstCycleDay
    }
}

data class TemplateAssignmentConfig(
    val enabled: Boolean,
    val manualOnly: Boolean,
    val allowedPrefixes: List<String>,
    val lockAssignmentModeEditing: Boolean,
    val lockAssignmentPrefixRules: Boolean
)
