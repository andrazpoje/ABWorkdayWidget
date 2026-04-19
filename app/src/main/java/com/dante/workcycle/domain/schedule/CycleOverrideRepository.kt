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

    fun hasOverrides(): Boolean {
        val raw = prefs.getString(KEY_OVERRIDES_JSON, null)
        return !raw.isNullOrBlank() && raw != "{}"
    }

    fun clearAllOverrides() {
        prefs.edit()
            .remove(KEY_OVERRIDES_JSON)
            .apply()
    }


    companion object {
        private const val PREFS_NAME = "cycle_override_prefs"
        private const val KEY_OVERRIDES_JSON = "cycle_overrides_json"
    }
}
