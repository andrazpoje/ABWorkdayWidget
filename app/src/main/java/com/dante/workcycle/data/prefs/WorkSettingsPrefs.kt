package com.dante.workcycle.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dante.workcycle.domain.model.CycleLayer
import com.dante.workcycle.domain.worklog.accounting.BreakAccountingMode
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

    fun getBreakAccountingMode(): BreakAccountingMode {
        val rawValue = prefs.getString(
            KEY_BREAK_ACCOUNTING_MODE,
            DEFAULT_BREAK_ACCOUNTING_MODE.name
        )

        return runCatching {
            BreakAccountingMode.valueOf(rawValue.orEmpty())
        }.getOrDefault(DEFAULT_BREAK_ACCOUNTING_MODE)
    }

    fun setBreakAccountingMode(mode: BreakAccountingMode) {
        prefs.edit {
            putString(KEY_BREAK_ACCOUNTING_MODE, mode.name)
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

    /**
     * Controls whether Work Log can interpret multiple start/finish sessions
     * within the same day.
     *
     * The default false value preserves the previous single-session-per-day
     * behavior.
     */
    fun isMultipleWorkSessionsEnabled(): Boolean {
        return prefs.getBoolean(
            KEY_ALLOW_MULTIPLE_WORK_SESSIONS_PER_DAY,
            DEFAULT_ALLOW_MULTIPLE_WORK_SESSIONS_PER_DAY
        )
    }

    fun setMultipleWorkSessionsEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_ALLOW_MULTIPLE_WORK_SESSIONS_PER_DAY, enabled)
        }
    }

    fun getExpectedStartConfig(label: String): ExpectedStartConfig? {
        return getExpectedStartConfig(CycleLayer.PRIMARY, label)
    }

    fun getExpectedStartConfig(layer: CycleLayer, label: String): ExpectedStartConfig? {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return null

        val layerItem = getExpectedTimeItem(layer, cleanedLabel)
        val item = when {
            layerItem != null && hasExpectedStartFields(layerItem) -> layerItem
            layer == CycleLayer.PRIMARY -> getLegacyExpectedTimeItem(cleanedLabel)
            else -> null
        }
            ?: return null

        return runCatching {
            ExpectedStartConfig(
                enabled = item.optBoolean("enabled", false),
                startTime = item.optString("startTime", "").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun setExpectedStartEnabled(label: String, enabled: Boolean) {
        setExpectedStartEnabled(CycleLayer.PRIMARY, label, enabled)
    }

    fun setExpectedStartEnabled(layer: CycleLayer, label: String, enabled: Boolean) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return

        val root = loadExpectedTimesRoot()
        val item = getOrCreateExpectedTimeItem(root, layer, cleanedLabel)
        val startTime = item.optString("startTime", "").ifBlank { DEFAULT_EXPECTED_START_TIME }

        item.put("enabled", enabled)
        item.put("startTime", startTime)
        saveExpectedTimesRoot(root)
    }

    fun setExpectedStartTime(label: String, startTime: String) {
        setExpectedStartTime(CycleLayer.PRIMARY, label, startTime)
    }

    fun setExpectedStartTime(layer: CycleLayer, label: String, startTime: String) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank() || startTime.isBlank()) return

        val root = loadExpectedTimesRoot()
        val item = getOrCreateExpectedTimeItem(root, layer, cleanedLabel)
        item.put("enabled", true)
        item.put("startTime", startTime)
        saveExpectedTimesRoot(root)
    }

    fun getExpectedStartTimeOrDefault(label: String): String {
        return getExpectedStartConfig(label)?.startTime ?: DEFAULT_EXPECTED_START_TIME
    }

    fun getExpectedStartTimeOrDefault(layer: CycleLayer, label: String): String {
        return getExpectedStartConfig(layer, label)?.startTime ?: DEFAULT_EXPECTED_START_TIME
    }

    fun getExpectedEndConfig(label: String): ExpectedEndConfig? {
        return getExpectedEndConfig(CycleLayer.PRIMARY, label)
    }

    fun getExpectedEndConfig(layer: CycleLayer, label: String): ExpectedEndConfig? {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return null

        val layerItem = getExpectedTimeItem(layer, cleanedLabel)
        val item = when {
            layerItem != null && hasExpectedEndFields(layerItem) -> layerItem
            layer == CycleLayer.PRIMARY -> getLegacyExpectedTimeItem(cleanedLabel)
            else -> null
        }
            ?: return null

        return runCatching {
            ExpectedEndConfig(
                enabled = item.optBoolean("endEnabled", false),
                endTime = item.optString("endTime", "").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun setExpectedEndEnabled(label: String, enabled: Boolean) {
        setExpectedEndEnabled(CycleLayer.PRIMARY, label, enabled)
    }

    fun setExpectedEndEnabled(layer: CycleLayer, label: String, enabled: Boolean) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank()) return

        val root = loadExpectedTimesRoot()
        val item = getOrCreateExpectedTimeItem(root, layer, cleanedLabel)
        val endTime = item.optString("endTime", "").ifBlank { DEFAULT_EXPECTED_END_TIME }

        item.put("endEnabled", enabled)
        item.put("endTime", endTime)
        saveExpectedTimesRoot(root)
    }

    fun setExpectedEndTime(label: String, endTime: String) {
        setExpectedEndTime(CycleLayer.PRIMARY, label, endTime)
    }

    fun setExpectedEndTime(layer: CycleLayer, label: String, endTime: String) {
        val cleanedLabel = label.trim()
        if (cleanedLabel.isBlank() || endTime.isBlank()) return

        val root = loadExpectedTimesRoot()
        val item = getOrCreateExpectedTimeItem(root, layer, cleanedLabel)
        item.put("endEnabled", true)
        item.put("endTime", endTime)
        saveExpectedTimesRoot(root)
    }

    fun getExpectedEndTimeOrDefault(label: String): String {
        return getExpectedEndConfig(label)?.endTime ?: DEFAULT_EXPECTED_END_TIME
    }

    fun getExpectedEndTimeOrDefault(layer: CycleLayer, label: String): String {
        return getExpectedEndConfig(layer, label)?.endTime ?: DEFAULT_EXPECTED_END_TIME
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
        const val KEY_BREAK_ACCOUNTING_MODE = "break_accounting_mode"
        const val KEY_OVERTIME_TRACKING_ENABLED = "overtime_tracking_enabled"
        const val KEY_EXPECTED_TIMES_BY_LAYER_AND_LABEL = "expected_times_by_layer_and_label"
        const val KEY_EXPECTED_STARTS_BY_LABEL = "expected_starts_by_label"
        const val KEY_WIDGET_INFO_MODE = "widget_info_mode"
        const val KEY_ALLOW_MULTIPLE_WORK_SESSIONS_PER_DAY =
            "allow_multiple_work_sessions_per_day"
        const val WIDGET_INFO_MODE_WORKED_TODAY = "worked_today"
        const val WIDGET_INFO_MODE_START_TIME = "start_time"

        const val DEFAULT_DAILY_TARGET_MINUTES = 480
        const val DEFAULT_BREAK_MINUTES = 30
        val DEFAULT_BREAK_ACCOUNTING_MODE = BreakAccountingMode.UNPAID
        const val DEFAULT_OVERTIME_TRACKING_ENABLED = true
        const val DEFAULT_ALLOW_MULTIPLE_WORK_SESSIONS_PER_DAY = false
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

    private fun getExpectedTimeItem(layer: CycleLayer, label: String): JSONObject? {
        val root = loadExpectedTimesRoot()
        return root.optJSONObject(layer.name)?.optJSONObject(label)
    }

    private fun getLegacyExpectedTimeItem(label: String): JSONObject? {
        return loadExpectedStartsRoot().optJSONObject(label)
    }

    private fun getOrCreateExpectedTimeItem(
        root: JSONObject,
        layer: CycleLayer,
        label: String
    ): JSONObject {
        val layerRoot = root.optJSONObject(layer.name) ?: JSONObject().also {
            root.put(layer.name, it)
        }

        return layerRoot.optJSONObject(label) ?: JSONObject().also {
            layerRoot.put(label, it)
        }
    }

    private fun hasExpectedStartFields(item: JSONObject): Boolean {
        return item.has("enabled") || item.has("startTime")
    }

    private fun hasExpectedEndFields(item: JSONObject): Boolean {
        return item.has("endEnabled") || item.has("endTime")
    }

    private fun loadExpectedTimesRoot(): JSONObject {
        val saved = prefs.getString(KEY_EXPECTED_TIMES_BY_LAYER_AND_LABEL, null).orEmpty()
        return runCatching {
            if (saved.isBlank()) JSONObject() else JSONObject(saved)
        }.getOrElse { JSONObject() }
    }

    private fun saveExpectedTimesRoot(root: JSONObject) {
        prefs.edit {
            putString(KEY_EXPECTED_TIMES_BY_LAYER_AND_LABEL, root.toString())
        }
    }

    private fun loadExpectedStartsRoot(): JSONObject {
        val saved = prefs.getString(KEY_EXPECTED_STARTS_BY_LABEL, null).orEmpty()
        return runCatching {
            if (saved.isBlank()) JSONObject() else JSONObject(saved)
        }.getOrElse { JSONObject() }
    }
}
