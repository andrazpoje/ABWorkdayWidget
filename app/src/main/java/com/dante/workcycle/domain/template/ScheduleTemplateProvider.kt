package com.dante.workcycle.domain.template

import com.dante.workcycle.R
import java.time.LocalDate

/**
 * Registry of built-in WorkCycle schedule templates.
 *
 * Template IDs are stable keys used by preferences, onboarding, Settings, and future
 * migration logic. Built-in label resources provide localized labels when a template
 * is applied, while custom user labels remain user data and must not be auto-translated
 * after creation. Keep new built-in templates here so grouping and lock behavior stay
 * explicit and auditable.
 */
object ScheduleTemplateProvider {

    const val TEMPLATE_SINGLE_SHIFT = "template_single_shift"
    const val TEMPLATE_AB = "template_ab"
    const val TEMPLATE_TWO_SHIFT = "template_two_shift"
    const val TEMPLATE_THREE_SHIFT = "template_three_shift"
    const val TEMPLATE_POSTA_SLOVENIJE_AB = "posta_slovenije_ab"

    const val TEMPLATE_PANAMA_223 = "template_panama_223"

    const val TEMPLATE_4_ON_4_OFF = "template_4_on_4_off"

    /**
     * Built-in template definitions and their editing constraints.
     *
     * `locksCycleEditing`, `locksRulesEditing`, `allowsStartDateEditing`, and
     * `allowsCycleOverrides` are UI/business capabilities. They should restrict editing
     * through TemplateManager and Settings, not change DefaultScheduleResolver's core
     * schedule calculation rules.
     */
    private val templates = listOf(

        // Splošni template-i
        ScheduleTemplate(
            id = TEMPLATE_SINGLE_SHIFT,
            titleRes = R.string.template_single_shift_title,
            descriptionRes = R.string.template_single_shift_description,
            fixedCycle = listOf("Dopoldne"),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Dopoldne",
            locksCycleEditing = false,
            locksRulesEditing = false,
            fixedCycleRes = listOf(R.string.template_label_single_shift),
            fixedFirstCycleDayRes = R.string.template_label_single_shift
        ),

        ScheduleTemplate(
            id = TEMPLATE_TWO_SHIFT,
            titleRes = R.string.template_two_shift_title,
            descriptionRes = R.string.template_two_shift_description,
            fixedCycle = listOf("Dopoldan", "Popoldan"),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Dopoldan",
            locksCycleEditing = false,
            locksRulesEditing = false,
            fixedCycleRes = listOf(
                R.string.template_label_two_shift_morning,
                R.string.template_label_two_shift_afternoon
            ),
            fixedFirstCycleDayRes = R.string.template_label_two_shift_morning
        ),

        ScheduleTemplate(
            id = TEMPLATE_THREE_SHIFT,
            titleRes = R.string.template_three_shift_title,
            descriptionRes = R.string.template_three_shift_description,
            fixedCycle = listOf("Dopoldan", "Popoldan", "Nočna"),
            fixedStartDate = LocalDate.now(),
            fixedFirstCycleDay = "Dopoldan",
            locksCycleEditing = false,
            locksRulesEditing = false,
            fixedCycleRes = listOf(
                R.string.template_label_three_shift_morning,
                R.string.template_label_three_shift_afternoon,
                R.string.template_label_night
            ),
            fixedFirstCycleDayRes = R.string.template_label_three_shift_morning
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
            skipHolidays = false,
            fixedCycleRes = listOf(
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.off_day_label,
                R.string.off_day_label,
                R.string.off_day_label,
                R.string.off_day_label
            ),
            fixedFirstCycleDayRes = R.string.template_label_work
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
            skipHolidays = false,
            fixedCycleRes = listOf(
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.off_day_label,
                R.string.off_day_label,
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.off_day_label,
                R.string.off_day_label,
                R.string.template_label_work,
                R.string.template_label_work,
                R.string.off_day_label,
                R.string.off_day_label,
                R.string.off_day_label
            ),
            fixedFirstCycleDayRes = R.string.template_label_work
        ),

        // Posebni / poklicni template
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

    fun getById(id: String?): ScheduleTemplate? {
        if (id.isNullOrBlank()) return null
        return templates.firstOrNull { it.id == id }
    }

    fun getGeneralTemplates(): List<ScheduleTemplate> {
        return templates.filter { template ->
            template.id in setOf(
                TEMPLATE_SINGLE_SHIFT,
                TEMPLATE_TWO_SHIFT,
                TEMPLATE_THREE_SHIFT,
                TEMPLATE_4_ON_4_OFF,
                TEMPLATE_PANAMA_223
            )
        }
    }

    fun getSpecialTemplates(): List<ScheduleTemplate> {
        return templates.filter { template ->
            template.id in setOf(
                TEMPLATE_AB,
                TEMPLATE_POSTA_SLOVENIJE_AB
            )
        }
    }
}
