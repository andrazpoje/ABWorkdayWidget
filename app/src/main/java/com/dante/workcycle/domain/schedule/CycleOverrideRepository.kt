package com.dante.workcycle.domain.schedule

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

class CycleOverrideRepository(
    context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOverrideLabel(date: LocalDate): String? {
        val json = JSONObject(prefs.getString(KEY_OVERRIDES_JSON, "{}") ?: "{}")
        return json.optString(date.toString(), "")
            .trim()
            .ifBlank { null }
    }

    fun setOverrideLabel(date: LocalDate, label: String?) {
        val json = JSONObject(prefs.getString(KEY_OVERRIDES_JSON, "{}") ?: "{}")
        val cleanLabel = label?.trim().orEmpty()

        if (cleanLabel.isBlank()) {
            json.remove(date.toString())
        } else {
            json.put(date.toString(), cleanLabel)
        }

        prefs.edit()
            .putString(KEY_OVERRIDES_JSON, json.toString())
            .apply()
    }

    fun removeOverride(date: LocalDate) {
        val json = JSONObject(prefs.getString(KEY_OVERRIDES_JSON, "{}") ?: "{}")
        json.remove(date.toString())

        prefs.edit()
            .putString(KEY_OVERRIDES_JSON, json.toString())
            .apply()
    }

    fun hasOverride(date: LocalDate): Boolean {
        return getOverrideLabel(date) != null
    }

    fun getAll(): Map<LocalDate, String> {
        val json = JSONObject(prefs.getString(KEY_OVERRIDES_JSON, "{}") ?: "{}")
        val result = linkedMapOf<LocalDate, String>()

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val label = json.optString(key, "").trim()

            if (label.isNotBlank()) {
                runCatching { LocalDate.parse(key) }
                    .getOrNull()
                    ?.let { date ->
                        result[date] = label
                    }
            }
        }

        return result
    }

    companion object {
        private const val PREFS_NAME = "cycle_override_prefs"
        private const val KEY_OVERRIDES_JSON = "cycle_overrides_json"
    }
}