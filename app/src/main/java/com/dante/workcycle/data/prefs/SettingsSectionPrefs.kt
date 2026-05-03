package com.dante.workcycle.data.prefs

import android.content.Context
import androidx.core.content.edit

/**
 * Persists Settings screen expand/collapse state.
 *
 * This is UI-only state used to keep the long Settings screen manageable
 * without changing any underlying settings logic or business behavior.
 */
class SettingsSectionPrefs(context: Context) {

    companion object {
        private const val PREFS_NAME = "settings_section_prefs"

        const val SECTION_PRIMARY_CYCLE = "primary_cycle"
        const val SECTION_RULES = "rules"
        const val SECTION_SECONDARY_CYCLE = "secondary_cycle"
        const val SECTION_STATUS_LABELS = "status_labels"
        const val SECTION_DISPLAY = "display"
        const val SECTION_COLORS = "colors"
        const val SECTION_APPEARANCE = "appearance"
        const val SECTION_WIDGETS = "widgets"
        const val SECTION_NOTIFICATIONS = "notifications"
        const val SECTION_BACKUP = "backup"
        const val SECTION_DEVELOPER_TOOLS = "developer_tools"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isExpanded(sectionKey: String, defaultExpanded: Boolean = false): Boolean {
        return prefs.getBoolean(sectionKey, defaultExpanded)
    }

    fun setExpanded(sectionKey: String, expanded: Boolean) {
        prefs.edit { putBoolean(sectionKey, expanded) }
    }
}
