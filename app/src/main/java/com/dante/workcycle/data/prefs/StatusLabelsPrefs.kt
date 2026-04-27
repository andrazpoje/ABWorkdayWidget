package com.dante.workcycle.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.core.status.StatusVisuals
import com.dante.workcycle.domain.model.StatusLabel
import org.json.JSONArray
import org.json.JSONObject

class StatusLabelsPrefs(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val defaultSystemLabels = listOf(
        StatusLabel(
            name = "Bolni\u0161ka",
            color = requireSystemColor("sick"),
            isSystem = true,
            isEnabled = true,
            iconKey = "sick"
        ),
        StatusLabel(
            name = "Dopust",
            color = requireSystemColor("vacation"),
            isSystem = true,
            isEnabled = true,
            iconKey = "vacation"
        ),
        StatusLabel(
            name = "De\u017eurstvo",
            color = requireSystemColor("standby"),
            isSystem = true,
            isEnabled = true,
            iconKey = "standby"
        ),
        StatusLabel(
            name = "Kr\u010denje",
            color = requireSystemColor("reduction"),
            isSystem = true,
            isEnabled = true,
            iconKey = "reduction"
        ),
        StatusLabel(
            name = "Nadome\u0161\u010danje",
            color = requireSystemColor("replacement"),
            isSystem = true,
            isEnabled = true,
            iconKey = "replacement"
        ),
        StatusLabel(
            name = "Sestanek",
            color = requireSystemColor("meeting"),
            isSystem = true,
            isEnabled = true,
            iconKey = "meeting"
        ),
        StatusLabel(
            name = "Teren",
            color = requireSystemColor("terrain"),
            isSystem = true,
            isEnabled = true,
            iconKey = "terrain"
        )
    )

    fun getLabels(): List<StatusLabel> {
        val saved = prefs.getString(KEY_LABELS, null)
        val parsed = if (saved.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { parseLabels(saved) }.getOrDefault(emptyList())
        }

        val base = parsed.ifEmpty { defaultSystemLabels }
        val merged = sortLabels(refreshSystemLabels(mergeMissingSystemLabels(base)))

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

        prefs.edit {
            putString(KEY_LABELS, json.toString())
        }
    }

    fun getDisplayName(label: StatusLabel): String {
        return StatusVisuals.getDisplayName(context, label)
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

    private fun refreshSystemLabels(labels: List<StatusLabel>): List<StatusLabel> {
        return labels.map { label ->
            val semanticColor = if (label.isSystem) {
                StatusVisuals.getDefaultColor(label.iconKey)
            } else {
                null
            }

            if (semanticColor != null && semanticColor != label.color) {
                label.copy(color = semanticColor)
            } else {
                label
            }
        }
    }

    private fun sortLabels(labels: List<StatusLabel>): List<StatusLabel> {
        fun systemOrderIndex(label: StatusLabel): Int {
            return when (label.iconKey) {
                "sick" -> 0
                "vacation" -> 1
                "standby" -> 2
                "reduction" -> 3
                "replacement" -> 4
                "meeting" -> 5
                "terrain" -> 6
                else -> Int.MAX_VALUE
            }
        }

        return labels.sortedWith(
            compareBy<StatusLabel> { !it.isSystem }
                .thenBy { systemOrderIndex(it) }
                .thenBy { it.name.lowercase() }
        )
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

        private fun requireSystemColor(iconKey: String): Int {
            return requireNotNull(StatusVisuals.getDefaultColor(iconKey)) {
                "Missing default status color for $iconKey"
            }
        }
    }
}
