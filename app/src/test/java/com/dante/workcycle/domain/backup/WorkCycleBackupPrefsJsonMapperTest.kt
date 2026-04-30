package com.dante.workcycle.domain.backup

import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCycleBackupPrefsJsonMapperTest {

    @Test
    fun supportsPrimitivePreferenceTypes() {
        val json = WorkCycleBackupPrefsJsonMapper.toJson(
            prefsName = "manual_schedule_prefs",
            rawValues = linkedMapOf(
                "enabled" to true,
                "count" to 3,
                "timestamp" to 123456789L,
                "ratio" to 1.5f,
                "label" to "Teren"
            )
        )

        assertTrue(json!!.contains(""""enabled": true"""))
        assertTrue(json.contains(""""count": 3"""))
        assertTrue(json.contains(""""timestamp": 123456789"""))
        assertTrue(json.contains(""""ratio": 1.5"""))
        assertTrue(json.contains(""""label": "Teren""""))
    }

    @Test
    fun serializesStringSetAsJsonArray() {
        val json = WorkCycleBackupPrefsJsonMapper.toJson(
            prefsName = "status_schedule_prefs",
            rawValues = mapOf("status_tags_2026-04-30" to setOf("Dopust", "Teren"))
        )

        assertTrue(json!!.contains("["))
        assertTrue(json.contains(""""Dopust""""))
        assertTrue(json.contains(""""Teren""""))
    }

    @Test
    fun whitelistIncludesOnlyAllowedKeys() {
        val filtered = WorkCycleBackupPrefsSpec.filterValues(
            AppPrefs.NAME,
            mapOf(
                AppPrefs.KEY_APP_LANGUAGE to "sl",
                AppPrefs.KEY_APP_THEME to "dark",
                AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION to "3.0"
            )
        )

        assertEquals(setOf(AppPrefs.KEY_APP_LANGUAGE, AppPrefs.KEY_APP_THEME), filtered.keys)
    }

    @Test
    fun blacklistExcludesMixedPrefsDebugAndTransientKeys() {
        val json = WorkCycleBackupPrefsJsonMapper.toJson(
            prefsName = Prefs.PREFS_NAME,
            rawValues = linkedMapOf(
                Prefs.KEY_NOTIFICATIONS_ENABLED to true,
                "debug_developer_tools_unlocked" to true,
                "last_seen_whats_new_version" to "3.0",
                "templates_hint_shown" to true,
                Prefs.KEY_HOME_WIDGET_TIP_DISMISSED to true
            )
        )

        assertTrue(json!!.contains(""""notifications_enabled": true"""))
        assertFalse(json.contains("debug_developer_tools_unlocked"))
        assertFalse(json.contains("last_seen_whats_new_version"))
        assertFalse(json.contains("templates_hint_shown"))
        assertFalse(json.contains(Prefs.KEY_HOME_WIDGET_TIP_DISMISSED))
    }

    @Test
    fun workSessionSnapshotPrefsAreExcluded() {
        val json = WorkCycleBackupPrefsJsonMapper.toJson(
            prefsName = "work_session_snapshot_prefs",
            rawValues = mapOf("session_cycle_label" to "A")
        )

        assertNull(json)
    }

    @Test
    fun includedPrefsNamesExcludeWorkSessionSnapshot() {
        val names = WorkCycleBackupPrefsSpec.includedPrefsNames()

        assertFalse(names.contains("work_session_snapshot_prefs"))
        assertTrue(names.contains(Prefs.PREFS_NAME))
        assertTrue(names.contains("work_settings_prefs"))
    }

    @Test
    fun jsonEscapingPreservesCommaQuoteNewlineAndUtf8() {
        val json = WorkCycleBackupPrefsJsonMapper.toJson(
            prefsName = "manual_schedule_prefs",
            rawValues = mapOf(
                "note" to "Vejica, \"quote\"\nščž"
            )
        )

        assertTrue(json!!.contains("""Vejica, \"quote\"\nščž"""))
    }
}
