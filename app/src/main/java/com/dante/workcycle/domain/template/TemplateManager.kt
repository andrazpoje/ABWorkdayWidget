package com.dante.workcycle.domain.template

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.data.prefs.TemplatePrefs
import com.dante.workcycle.domain.model.AssignmentCycleAdvanceMode
import com.dante.workcycle.domain.model.CycleMode
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.ManualScheduleRepository

object TemplateManager {

    private const val KEY_ASSIGNMENT_ALLOWED_PREFIXES = "template_assignment_allowed_prefixes"

    fun getActiveTemplate(context: Context): ScheduleTemplate? {
        val id = TemplatePrefs.getActiveTemplateId(context)
        return ScheduleTemplateProvider.getById(id)
    }

    fun getCurrentTemplateDisplayName(context: Context): String {
        val template = getActiveTemplate(context)
        return if (template != null) {
            context.getString(template.titleRes)
        } else {
            context.getString(R.string.template_none)
        }
    }

    fun isTemplateActive(context: Context): Boolean {
        return getActiveTemplate(context) != null
    }

    fun isCycleEditingLocked(context: Context): Boolean {
        return getActiveTemplate(context)?.locksCycleEditing == true
    }

    fun isRulesEditingLocked(context: Context): Boolean {
        return getActiveTemplate(context)?.locksRulesEditing == true
    }

    fun canEditStartDate(context: Context): Boolean {
        return getActiveTemplate(context)?.allowsStartDateEditing != false
    }

    fun allowsCycleOverrides(context: Context): Boolean {
        return getActiveTemplate(context)?.allowsCycleOverrides != false
    }

    fun getAssignmentConfig(context: Context): TemplateAssignmentConfig? {
        return getActiveTemplate(context)?.assignmentConfig
    }

    fun isAssignmentModeEditingLocked(context: Context): Boolean {
        return getAssignmentConfig(context)?.lockAssignmentModeEditing == true
    }

    fun applyTemplate(context: Context, templateId: String) {
        val previousTemplateId = TemplatePrefs.getActiveTemplateId(context)
        val template = ScheduleTemplateProvider.getById(templateId) ?: return
        val labelsPrefs = AssignmentLabelsPrefs(context)

        if (
            previousTemplateId == ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB &&
            previousTemplateId != templateId
        ) {
            labelsPrefs.removePostaTemplateLabels()
            ManualScheduleRepository(context).removeSecondaryManualLabelsByNames(
                listOf("O1", "Š1")
            )
        }

        TemplatePrefs.setActiveTemplateId(context, template.id)

        val fixedCycle = template.resolveFixedCycle(context)
        val fixedFirstCycleDay = template.resolveFixedFirstCycleDay(context)

        CycleManager.saveCycle(context, fixedCycle)
        CycleManager.saveStartDate(context, template.fixedStartDate)

        context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(AppPrefs.KEY_START_YEAR, template.fixedStartDate.year)
                putInt(AppPrefs.KEY_START_MONTH, template.fixedStartDate.monthValue)
                putInt(AppPrefs.KEY_START_DAY, template.fixedStartDate.dayOfMonth)
                putString(AppPrefs.KEY_FIRST_CYCLE_DAY, fixedFirstCycleDay)

                if (template.locksRulesEditing) {
                    putBoolean(AppPrefs.KEY_SKIP_SATURDAYS, template.skipSaturdays)
                    putBoolean(AppPrefs.KEY_SKIP_SUNDAYS, template.skipSundays)
                    putBoolean(AppPrefs.KEY_SKIP_HOLIDAYS, template.skipHolidays)
                }

                putString(
                    KEY_ASSIGNMENT_ALLOWED_PREFIXES,
                    template.assignmentConfig?.allowedPrefixes?.joinToString(",").orEmpty()
                )
            }

        applyAssignmentConfig(context, template.assignmentConfig)

        if (template.id == ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB) {
            labelsPrefs.ensurePostaTemplateLabels()
        }
    }

    fun clearTemplate(context: Context) {
        val activeTemplateId = TemplatePrefs.getActiveTemplateId(context)
        val labelsPrefs = AssignmentLabelsPrefs(context)

        if (activeTemplateId == ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB) {
            labelsPrefs.removePostaTemplateLabels()
            ManualScheduleRepository(context).removeSecondaryManualLabelsByNames(
                listOf("O1", "Š1")
            )
        }

        TemplatePrefs.clear(context)

        context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_ASSIGNMENT_ALLOWED_PREFIXES)
            }
    }

    private fun applyAssignmentConfig(
        context: Context,
        config: TemplateAssignmentConfig?
    ) {
        if (config == null) return

        val assignmentPrefs = AssignmentCyclePrefs(context)

        assignmentPrefs.setEnabled(config.enabled)

        if (config.manualOnly) {
            assignmentPrefs.setMode(CycleMode.MANUAL)
        }

        assignmentPrefs.setAdvanceMode(AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY)
    }
}
