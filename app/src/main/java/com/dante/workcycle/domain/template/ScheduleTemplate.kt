package com.dante.workcycle.domain.template

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
    val skipHolidays: Boolean = false
)

data class TemplateAssignmentConfig(
    val enabled: Boolean,
    val manualOnly: Boolean,
    val allowedPrefixes: List<String>,
    val lockAssignmentModeEditing: Boolean,
    val lockAssignmentPrefixRules: Boolean
)