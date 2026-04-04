package com.dante.workcycle.domain.template

import com.dante.workcycle.R
import java.time.LocalDate

object ScheduleTemplateProvider {

    const val TEMPLATE_POSTA_SLOVENIJE_AB = "posta_slovenije_ab"

    private val templates = listOf(
        ScheduleTemplate(
            id = TEMPLATE_POSTA_SLOVENIJE_AB,
            titleRes = R.string.template_posta_slovenije_title,
            descriptionRes = R.string.template_posta_slovenije_description,
            fixedCycle = listOf("A", "B"),
            fixedStartDate = LocalDate.of(2026, 1, 1),
            fixedFirstCycleDay = "A",
            locksCycleEditing = true,
            locksRulesEditing = true,
            allowsStartDateEditing = false,
            allowsCycleOverrides = false,
            assignmentConfig = TemplateAssignmentConfig(
                enabled = true,
                manualOnly = true,
                allowedPrefixes = listOf("O", "K", "Š"),
                lockAssignmentModeEditing = true,
                lockAssignmentPrefixRules = true
            ),
            skipSaturdays = true,
            skipSundays = true,
            skipHolidays = true
        )
    )

    fun getAll(): List<ScheduleTemplate> = templates

    fun getById(id: String?): ScheduleTemplate? {
        if (id.isNullOrBlank()) return null
        return templates.firstOrNull { it.id == id }
    }
}