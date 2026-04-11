package com.dante.workcycle.domain.template

import com.dante.workcycle.R
import java.time.LocalDate

object ScheduleTemplateProvider {

    const val TEMPLATE_AB = "template_ab"
    const val TEMPLATE_TWO_SHIFT = "template_two_shift"
    const val TEMPLATE_THREE_SHIFT = "template_three_shift"
    const val TEMPLATE_POSTA_SLOVENIJE_AB = "posta_slovenije_ab"

    const val TEMPLATE_PANAMA_223 = "template_panama_223"

    const val TEMPLATE_4_ON_4_OFF = "template_4_on_4_off"

    private val templates = listOf(

        // Splošni template-i
        ScheduleTemplate(
            id = TEMPLATE_AB,
            titleRes = R.string.template_ab_title,
            descriptionRes = R.string.template_ab_description,
            fixedCycle = listOf("A", "B"),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "A",
            locksCycleEditing = false,
            locksRulesEditing = false
        ),

        ScheduleTemplate(
            id = TEMPLATE_TWO_SHIFT,
            titleRes = R.string.template_two_shift_title,
            descriptionRes = R.string.template_two_shift_description,
            fixedCycle = listOf("Dopoldan", "Popoldan"),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Dopoldan",
            locksCycleEditing = false,
            locksRulesEditing = false
        ),

        ScheduleTemplate(
            id = TEMPLATE_THREE_SHIFT,
            titleRes = R.string.template_three_shift_title,
            descriptionRes = R.string.template_three_shift_description,
            fixedCycle = listOf("Dopoldan", "Popoldan", "Nočna"),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Dopoldan",
            locksCycleEditing = false,
            locksRulesEditing = false
        ),

        ScheduleTemplate(
            id = TEMPLATE_4_ON_4_OFF,
            titleRes = R.string.template_4on4off_title,
            descriptionRes = R.string.template_4on4off_description,
            fixedCycle = listOf(
                "Delo", "Delo", "Delo", "Delo",
                "Prosto", "Prosto", "Prosto", "Prosto"
            ),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Delo",
            locksCycleEditing = false,
            locksRulesEditing = true,
            allowsStartDateEditing = true,
            skipSaturdays = false,
            skipSundays = false,
            skipHolidays = false
        ),

        ScheduleTemplate(
            id = TEMPLATE_PANAMA_223,
            titleRes = R.string.template_panama_223_title,
            descriptionRes = R.string.template_panama_223_description,
            fixedCycle = listOf(
                "Delo", "Delo",
                "Prosto", "Prosto",
                "Delo", "Delo", "Delo",
                "Prosto", "Prosto",
                "Delo", "Delo",
                "Prosto", "Prosto", "Prosto"
            ),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Delo",
            locksCycleEditing = false,
            locksRulesEditing = true,
            allowsStartDateEditing = true,
            skipSaturdays = false,
            skipSundays = false,
            skipHolidays = false
        ),

        // Posebni / poklicni template
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