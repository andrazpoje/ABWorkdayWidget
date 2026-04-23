package com.dante.workcycle.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONObject

class WorkSettingsPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDailyTargetMinutes(): Int {
        return prefs.getInt(KEY_DAILY_TARGET_MINUTES, DEFAULT_DAILY_TARGET_MINUTES)
    }

    fun setDailyTargetMinutes(minutes: Int) {
        prefs.edit {
            putInt(KEY_DAILY_TARGET_MINUTES, minutes)
        }
    }

    fun getDefaultBreakMinutes(): Int {
        return prefs.getInt(KEY_DEFAULT_BREAK_MINUTES, DEFAULT_BREAK_MINUTES)
    }

    fun setDefaultBreakMinutes(minutes: Int) {
        prefs.edit {
            putInt(KEY_DEFAULT_BREAK_MINUTES, minutes)
        }
    }

    fun isOvertimeTrackingEnabled(): Boolean {
        return prefs.getBoolean(KEY_OVERTIME_TRACKING_ENABLED, DEFAULT_OVERTIME_TRACKING_ENABLED)
    }

    fun setOvertimeTrackingEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_OVERTIME_TRACKING_ENABLED, enabled)
        }
    }

    fun getWidgetInfoMode(): String {
        return prefs.getString(
            KEY_WIDGET_INFO_MODE,
            WIDGET_INFO_MODE_START_TIME
        ) ?: WIDGET_INFO_MODE_START_TIME
    }

    fun setWidgetInfoMode(mode: String) {
        val safeMode = when (mode) {
            WIDGET_INFO_MODE_WORKED_TODAY,
            WIDGET_INFO_MODE_START_TIME -> mode
            else -> WIDGET_INFO_MODE_START_TIME
        }

        prefs.edit {
            putString(KEY_WIDGET_INFO_MODE, safeMode)
        }
    }

    fun getExpectedStartConfig(label: String): ExpectedStartConfig? {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return null

        val json = prefs.getString(KEY_EXPECTED_STARTS_BY_LABEL, null).orEmpty()
        if (json.isBlank()) return null

        return runCatching {
            val root = JSONObject(json)
            if (!root.has(cleanedLabel)) return null

            val item = root.optJSONObject(cleanedLabel) ?: return null
            ExpectedStartConfig(
                enabled = item.optBoolean("enabled", false),
                startTime = item.optString("startTime", "").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun setExpectedStartEnabled(label: String, enabled: Boolean) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return

        val root = loadExpectedStartsRoot()
        val item = root.optJSONObject(cleanedLabel) ?: JSONObject()
        val startTime = item.optString("startTime", "").ifBlank { DEFAULT_EXPECTED_START_TIME }

        item.put("enabled", enabled)
        item.put("startTime", startTime)
        root.put(cleanedLabel, item)
        saveExpectedStartsRoot(root)
    }

    fun setExpectedStartTime(label: String, startTime: String) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank() || startTime.isBlank()) return

        val root = loadExpectedStartsRoot()
        val item = root.optJSONObject(cleanedLabel) ?: JSONObject()
        item.put("enabled", true)
        item.put("startTime", startTime)
        root.put(cleanedLabel, item)
        saveExpectedStartsRoot(root)
    }

    fun getExpectedStartTimeOrDefault(label: String): String {
        return getExpectedStartConfig(label)?.startTime ?: DEFAULT_EXPECTED_START_TIME
    }

    fun getExpectedEndConfig(label: String): ExpectedEndConfig? {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return null

        val json = prefs.getString(KEY_EXPECTED_STARTS_BY_LABEL, null).orEmpty()
        if (json.isBlank()) return null

        return runCatching {
            val root = JSONObject(json)
            if (!root.has(cleanedLabel)) return null

            val item = root.optJSONObject(cleanedLabel) ?: return null
            ExpectedEndConfig(
                enabled = item.optBoolean("endEnabled", false),
                endTime = item.optString("endTime", "").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun setExpectedEndEnabled(label: String, enabled: Boolean) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return

        val root = loadExpectedStartsRoot()
        val item = root.optJSONObject(cleanedLabel) ?: JSONObject()
        val endTime = item.optString("endTime", "").ifBlank { DEFAULT_EXPECTED_END_TIME }

        item.put("endEnabled", enabled)
        item.put("endTime", endTime)
        root.put(cleanedLabel, item)
        saveExpectedStartsRoot(root)
    }

    fun setExpectedEndTime(label: String, endTime: String) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank() || endTime.isBlank()) return

        val root = loadExpectedStartsRoot()
        val item = root.optJSONObject(cleanedLabel) ?: JSONObject()
        item.put("endEnabled", true)
        item.put("endTime", endTime)
        root.put(cleanedLabel, item)
        saveExpectedStartsRoot(root)
    }

    fun getExpectedEndTimeOrDefault(label: String): String {
        return getExpectedEndConfig(label)?.endTime ?: DEFAULT_EXPECTED_END_TIME
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "work_settings_prefs"
        const val KEY_DAILY_TARGET_MINUTES = "daily_target_minutes"
        const val KEY_DEFAULT_BREAK_MINUTES = "default_break_minutes"
        const val KEY_OVERTIME_TRACKING_ENABLED = "overtime_tracking_enabled"
        const val KEY_EXPECTED_STARTS_BY_LABEL = "expected_starts_by_label"
        const val KEY_WIDGET_INFO_MODE = "widget_info_mode"
        const val WIDGET_INFO_MODE_WORKED_TODAY = "worked_today"
        const val WIDGET_INFO_MODE_START_TIME = "start_time"

        const val DEFAULT_DAILY_TARGET_MINUTES = 480
        const val DEFAULT_BREAK_MINUTES = 30
        const val DEFAULT_OVERTIME_TRACKING_ENABLED = true
        const val DEFAULT_EXPECTED_START_TIME = "08:00"
        const val DEFAULT_EXPECTED_END_TIME = "16:00"

        val DAILY_TARGET_PRESETS = listOf(240, 360, 450, 480, 600, 720)
        val BREAK_PRESETS = listOf(0, 15, 20, 30, 45, 60)
    }

    data class ExpectedStartConfig(
        val enabled: Boolean,
        val startTime: String?
    )

    data class ExpectedEndConfig(
        val enabled: Boolean,
        val endTime: String?
    )

    private fun loadExpectedStartsRoot(): JSONObject {
        val saved = prefs.getString(KEY_EXPECTED_STARTS_BY_LABEL, null).orEmpty()
        return runCatching {
            if (saved.isBlank()) JSONObject() else JSONObject(saved)
        }.getOrElse { JSONObject() }
    }

    private fun saveExpectedStartsRoot(root: JSONObject) {
        prefs.edit {
            putString(KEY_EXPECTED_STARTS_BY_LABEL, root.toString())
        }
    }
}
