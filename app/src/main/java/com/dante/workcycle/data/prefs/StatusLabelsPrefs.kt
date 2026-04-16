package com.dante.workcycle.data.prefs

import android.content.Context
import android.graphics.Color
import com.dante.workcycle.R
import com.dante.workcycle.domain.model.StatusLabel
import org.json.JSONArray
import org.json.JSONObject

class StatusLabelsPrefs(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val defaultSystemLabels = listOf(
        StatusLabel(
            name = "Bolniška",
            color = Color.parseColor("#E53935"),
            isSystem = true,
            isEnabled = true,
            iconKey = "sick"
        ),
        StatusLabel(
            name = "Dopust",
            color = Color.parseColor("#F9A825"),
            isSystem = true,
            isEnabled = true,
            iconKey = "vacation"
        ),
        StatusLabel(
            name = "Dežurstvo",
            color = Color.parseColor("#8E24AA"),
            isSystem = true,
            isEnabled = true,
            iconKey = "standby"
        )
    )

    fun getLabels(): List<StatusLabel> {
        val saved = prefs.getString(KEY_LABELS, null)
        val parsed = if (saved.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { parseLabels(saved) }.getOrDefault(emptyList())
        }

        val base = if (parsed.isEmpty()) defaultSystemLabels else parsed
        val merged = mergeMissingSystemLabels(base)

        if (merged != parsed) {
            saveLabels(merged)
        }

        return merged
    }

    fun getSelectableLabels(): List<StatusLabel> {
        return getLabels().filter { it.isEnabled }
    }

    fun getLabelByName(name: String): StatusLabel? {
        return getLabels().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun saveLabels(labels: List<StatusLabel>) {
        val json = JSONArray()

        labels.forEach { label ->
            json.put(
                JSONObject().apply {
                    put("name", label.name)
                    put("color", label.color)
                    put("isSystem", label.isSystem)
                    put("isEnabled", label.isEnabled)
                    put("iconKey", label.iconKey)
                }
            )
        }

        prefs.edit().putString(KEY_LABELS, json.toString()).apply()
    }

    fun getDisplayName(label: StatusLabel): String {
        if (!label.isSystem) return label.name

        return when (label.iconKey) {
            "sick" -> context.getString(R.string.assignment_system_label_sick)
            "vacation" -> context.getString(R.string.assignment_system_label_vacation)
            "standby" -> context.getString(R.string.assignment_system_label_standby)
            else -> label.name
        }
    }

    fun getShortDisplayName(label: StatusLabel): String {
        if (!label.isSystem) return label.name

        return when (label.iconKey) {
            "sick" -> context.getString(R.string.assignment_system_label_sick_short)
            "vacation" -> context.getString(R.string.assignment_system_label_vacation_short)
            "standby" -> context.getString(R.string.assignment_system_label_standby_short)
            else -> label.name
        }
    }

    private fun mergeMissingSystemLabels(existing: List<StatusLabel>): List<StatusLabel> {
        val result = existing.toMutableList()
        val existingNames = existing.map { it.name.lowercase() }.toSet()

        defaultSystemLabels.forEach { systemLabel ->
            if (systemLabel.name.lowercase() !in existingNames) {
                result.add(systemLabel)
            }
        }

        return result
    }

    private fun parseLabels(jsonString: String): List<StatusLabel> {
        val json = JSONArray(jsonString)
        val list = mutableListOf<StatusLabel>()

        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)

            list.add(
                StatusLabel(
                    name = obj.getString("name"),
                    color = obj.getInt("color"),
                    isSystem = obj.optBoolean("isSystem", true),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    iconKey = obj.optString("iconKey", "").takeIf { it.isNotBlank() }
                )
            )
        }

        return list
    }

    companion object {
        private const val PREFS_NAME = "status_labels_prefs"
        private const val KEY_LABELS = "status_labels"
    }
}