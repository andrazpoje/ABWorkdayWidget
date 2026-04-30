package com.dante.workcycle.domain.backup

import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.data.prefs.WorkSettingsPrefs
import com.dante.workcycle.domain.schedule.CycleManager

/**
 * Explicit backup include/exclude rules for SharedPreferences payload export.
 *
 * Backup export must include only user-facing persisted state. Debug-only and
 * transient keys must stay out of the archive, and mixed preference files must
 * use an explicit whitelist instead of a blanket export.
 */
object WorkCycleBackupPrefsSpec {

    private const val PREFS_TEMPLATE = "template_prefs"
    private const val PREFS_SECONDARY_CYCLE = "secondary_cycle_prefs"
    private const val PREFS_ASSIGNMENT_LABELS = "assignment_labels_prefs"
    private const val PREFS_STATUS_LABELS = "status_labels_prefs"
    private const val PREFS_MANUAL_SCHEDULE = "manual_schedule_prefs"
    private const val PREFS_CYCLE_OVERRIDE = "cycle_override_prefs"
    private const val PREFS_STATUS_SCHEDULE = "status_schedule_prefs"
    private const val PREFS_WIDGET_STYLE = "widget_style_prefs"
    private const val PREFS_WORK_SESSION_SNAPSHOT = "work_session_snapshot_prefs"

    private const val KEY_TEMPLATE_ASSIGNMENT_ALLOWED_PREFIXES =
        "template_assignment_allowed_prefixes"
    private const val KEY_CYCLE_OVERRIDES_JSON = "cycle_overrides_json"

    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_TEMPLATES_HINT_SHOWN = "templates_hint_shown"
    private const val KEY_DEBUG_DEVELOPER_TOOLS_UNLOCKED = "debug_developer_tools_unlocked"

    private val rulesByPrefsName = mapOf(
        AppPrefs.NAME to Rule(
            allowedKeys = setOf(
                AppPrefs.KEY_START_YEAR,
                AppPrefs.KEY_START_MONTH,
                AppPrefs.KEY_START_DAY,
                AppPrefs.KEY_FIRST_CYCLE_DAY,
                AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX,
                AppPrefs.KEY_SKIP_SATURDAYS,
                AppPrefs.KEY_SKIP_SUNDAYS,
                AppPrefs.KEY_SKIP_HOLIDAYS,
                AppPrefs.KEY_OVERRIDE_SKIPPED,
                AppPrefs.KEY_SKIPPED_LABEL,
                AppPrefs.KEY_HOLIDAY_COUNTRY,
                AppPrefs.KEY_COUNTRY_MANUAL,
                AppPrefs.KEY_APP_LANGUAGE,
                AppPrefs.KEY_APP_THEME,
                AppPrefs.KEY_CYCLE_THEME,
                KEY_TEMPLATE_ASSIGNMENT_ALLOWED_PREFIXES
            )
        ),
        CycleManager.PREFS_NAME to Rule(includeAll = true),
        PREFS_TEMPLATE to Rule(includeAll = true),
        PREFS_SECONDARY_CYCLE to Rule(includeAll = true),
        PREFS_ASSIGNMENT_LABELS to Rule(includeAll = true),
        PREFS_STATUS_LABELS to Rule(includeAll = true),
        PREFS_MANUAL_SCHEDULE to Rule(includeAll = true),
        PREFS_CYCLE_OVERRIDE to Rule(
            allowedKeys = setOf(KEY_CYCLE_OVERRIDES_JSON)
        ),
        PREFS_STATUS_SCHEDULE to Rule(includeAll = true),
        "work_settings_prefs" to Rule(
            allowedKeys = setOf(
                WorkSettingsPrefs.KEY_DAILY_TARGET_MINUTES,
                WorkSettingsPrefs.KEY_DEFAULT_BREAK_MINUTES,
                WorkSettingsPrefs.KEY_BREAK_ACCOUNTING_MODE,
                WorkSettingsPrefs.KEY_OVERTIME_TRACKING_ENABLED,
                WorkSettingsPrefs.KEY_EXPECTED_TIMES_BY_LAYER_AND_LABEL,
                WorkSettingsPrefs.KEY_EXPECTED_STARTS_BY_LABEL,
                WorkSettingsPrefs.KEY_WIDGET_INFO_MODE
            )
        ),
        PREFS_WIDGET_STYLE to Rule(includeAll = true),
        Prefs.PREFS_NAME to Rule(
            allowedKeys = setOf(
                Prefs.KEY_NOTIFICATIONS_ENABLED,
                Prefs.KEY_SILENT_NOTIFICATION,
                Prefs.KEY_WIDGET_STYLE,
                Prefs.KEY_SHOW_ASSIGNMENT_ICONS_CALENDAR,
                Prefs.KEY_SHOW_ASSIGNMENT_ICONS_WEEKLY
            ),
            excludedKeys = setOf(
                Prefs.KEY_HOME_WIDGET_TIP_DISMISSED,
                AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION,
                KEY_ONBOARDING_COMPLETED,
                KEY_FIRST_LAUNCH,
                KEY_TEMPLATES_HINT_SHOWN,
                KEY_DEBUG_DEVELOPER_TOOLS_UNLOCKED
            )
        ),
        PREFS_WORK_SESSION_SNAPSHOT to Rule(excluded = true)
    )

    fun isIncludedPrefsFile(prefsName: String): Boolean {
        return rulesByPrefsName[prefsName]?.excluded != true
    }

    fun includedPrefsNames(): List<String> {
        return rulesByPrefsName.keys
            .filter(::isIncludedPrefsFile)
            .sorted()
    }

    fun filterValues(
        prefsName: String,
        values: Map<String, Any?>
    ): Map<String, Any?> {
        val rule = rulesByPrefsName[prefsName] ?: return emptyMap()
        if (rule.excluded) return emptyMap()

        return values.entries
            .asSequence()
            .filter { entry ->
                if (entry.key in rule.excludedKeys) {
                    false
                } else if (rule.includeAll) {
                    true
                } else {
                    entry.key in rule.allowedKeys
                }
            }
            .sortedBy { it.key }
            .associate { it.key to it.value }
    }

    private data class Rule(
        val includeAll: Boolean = false,
        val allowedKeys: Set<String> = emptySet(),
        val excludedKeys: Set<String> = emptySet(),
        val excluded: Boolean = false
    )
}
