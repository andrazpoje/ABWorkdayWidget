package com.dante.workcycle.data.prefs

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import com.dante.workcycle.domain.model.AssignmentLabel
import org.json.JSONArray
import org.json.JSONObject
import com.dante.workcycle.R

class AssignmentLabelsPrefs(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val defaultSystemLabels = listOf(
        AssignmentLabel(
            name = "Teren",
            color = Color.parseColor("#00897B"),
            isSystem = true,
            isEnabled = false,
            iconKey = "field"
        )
    )

    private val defaultUserLabels = emptyList<AssignmentLabel>()
    private val defaultLabels = defaultSystemLabels + defaultUserLabels

    private val postaTemplateLabels = listOf(
        AssignmentLabel(
            name = "O1",
            color = Color.parseColor("#1E88E5"),
            isSystem = false,
            isEnabled = true,
            iconKey = null,
            usageCount = 0,
            lastUsedAt = null
        ),
        AssignmentLabel(
            name = "Š1",
            color = Color.parseColor("#6D4C41"),
            isSystem = false,
            isEnabled = true,
            iconKey = null,
            usageCount = 0,
            lastUsedAt = null
        )
    )

    fun getLabels(): List<AssignmentLabel> {
        val saved = prefs.getString(KEY_LABELS, null)
        val parsed = if (saved.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { parseLabels(saved) }.getOrDefault(emptyList())
        }

        val base = parsed.ifEmpty { defaultLabels }
        val merged = mergeMissingSystemLabels(base)
        val sorted = sortLabels(merged)

        if (sorted != parsed) {
            saveLabels(sorted)
        }

        return sorted
    }

    fun getSelectableLabels(): List<AssignmentLabel> {
        return getLabels().filter { it.isEnabled }
    }

    fun saveLabels(labels: List<AssignmentLabel>) {
        val json = JSONArray()
        labels.forEach { label ->
            json.put(
                JSONObject().apply {
                    put("name", label.name)
                    put("color", label.color)
                    put("isSystem", label.isSystem)
                    put("isEnabled", label.isEnabled)
                    put("iconKey", label.iconKey)
                    put("usageCount", label.usageCount)
                    put("lastUsedAt", label.lastUsedAt)
                }
            )
        }

        prefs.edit {
            putString(KEY_LABELS, json.toString())
        }
    }

    fun addLabel(label: AssignmentLabel) {
        val current = getLabels().toMutableList()

        if (current.any { it.name.equals(label.name, ignoreCase = true) }) {
            return
        }

        current.add(
            label.copy(
                isSystem = false,
                isEnabled = true,
                usageCount = 0,
                lastUsedAt = null
            )
        )
        saveLabels(sortLabels(current))
    }

    fun updateLabel(oldName: String, updated: AssignmentLabel) {
        val current = getLabels().toMutableList()
        val index = current.indexOfFirst { it.name.equals(oldName, ignoreCase = true) }
        if (index == -1) return

        val old = current[index]

        val safeUpdated = if (old.isSystem) {
            old.copy(
                color = updated.color,
                isEnabled = updated.isEnabled
            )
        } else {
            old.copy(
                name = updated.name,
                color = updated.color,
                isEnabled = updated.isEnabled
            )
        }

        current[index] = safeUpdated
        saveLabels(sortLabels(current))
    }

    fun setEnabled(name: String, enabled: Boolean) {
        val current = getLabels().toMutableList()
        val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index == -1) return

        current[index] = current[index].copy(isEnabled = enabled)
        saveLabels(sortLabels(current))
    }

    fun deleteLabel(name: String) {
        val current = getLabels()
        val target = current.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return

        if (target.isSystem) return

        saveLabels(
            sortLabels(
                current.filterNot { it.name.equals(name, ignoreCase = true) }
            )
        )
    }

    fun markLabelUsed(name: String) {
        val current = getLabels().toMutableList()
        val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index == -1) return

        val label = current[index]
        current[index] = label.copy(
            usageCount = label.usageCount + 1,
            lastUsedAt = System.currentTimeMillis()
        )

        saveLabels(sortLabels(current))
    }

    fun getLabelByName(name: String): AssignmentLabel? {
        return getLabels().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun ensurePostaTemplateLabels() {
        val current = getLabels().toMutableList()
        var changed = false

        postaTemplateLabels.forEach { templateLabel ->
            val exists = current.any { it.name.equals(templateLabel.name, ignoreCase = true) }
            if (!exists) {
                current.add(templateLabel)
                changed = true
            }
        }

        if (changed) {
            saveLabels(sortLabels(current))
        }
    }

    fun removePostaTemplateLabels() {
        val templateNames = postaTemplateLabels.map { it.name.lowercase() }.toSet()
        val current = getLabels()

        val filtered = current.filterNot { label ->
            !label.isSystem && label.name.lowercase() in templateNames
        }

        if (filtered.size != current.size) {
            saveLabels(sortLabels(filtered))
        }
    }

    private fun sortLabels(labels: List<AssignmentLabel>): List<AssignmentLabel> {
        fun systemOrderIndex(label: AssignmentLabel): Int {
            return when (label.iconKey) {
                "sick" -> 0
                "vacation" -> 1
                "standby" -> 2
                "field" -> 3
                else -> Int.MAX_VALUE
            }
        }

        val system = labels
            .filter { it.isSystem }
            .sortedBy { systemOrderIndex(it) }

        val manual = labels
            .filterNot { it.isSystem }
            .sortedWith(
                compareByDescending<AssignmentLabel> { it.isEnabled }
                    .thenByDescending { it.usageCount }
                    .thenByDescending { it.lastUsedAt ?: 0L }
                    .thenBy { it.name.lowercase() }
            )

        return system + manual
    }

    fun getDisplayName(label: AssignmentLabel): String {
        if (!label.isSystem) return label.name

        return when (label.iconKey) {
            "sick" -> context.getString(R.string.assignment_system_label_sick)
            "vacation" -> context.getString(R.string.assignment_system_label_vacation)
            "standby" -> context.getString(R.string.assignment_system_label_standby)
            "field" -> context.getString(R.string.assignment_system_label_field)
            else -> label.name
        }
    }

    fun getShortDisplayName(label: AssignmentLabel): String {
        if (!label.isSystem) return label.name

        return when (label.iconKey) {
            "sick" -> context.getString(R.string.assignment_system_label_sick_short)
            "vacation" -> context.getString(R.string.assignment_system_label_vacation_short)
            "standby" -> context.getString(R.string.assignment_system_label_standby_short)
            "field" -> context.getString(R.string.assignment_system_label_field_short)
            else -> label.name
        }
    }

    private fun mergeMissingSystemLabels(existing: List<AssignmentLabel>): List<AssignmentLabel> {
        val result = existing.toMutableList()
        val existingNames = existing.map { it.name.lowercase() }.toSet()

        defaultSystemLabels.forEach { systemLabel ->
            if (systemLabel.name.lowercase() !in existingNames) {
                result.add(systemLabel)
            }
        }

        return result
    }

    private fun parseLabels(jsonString: String): List<AssignmentLabel> {
        val json = JSONArray(jsonString)
        val list = mutableListOf<AssignmentLabel>()

        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)

            list.add(
                AssignmentLabel(
                    name = obj.getString("name"),
                    color = obj.getInt("color"),
                    isSystem = obj.optBoolean("isSystem", false),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    iconKey = obj.optString("iconKey", "").takeIf { it.isNotBlank() },
                    usageCount = obj.optInt("usageCount", 0),
                    lastUsedAt = if (obj.has("lastUsedAt") && !obj.isNull("lastUsedAt")) {
                        obj.optLong("lastUsedAt")
                    } else {
                        null
                    }
                )
            )
        }

        return list
    }

    companion object {
        private const val PREFS_NAME = "assignment_labels_prefs"
        private const val KEY_LABELS = "assignment_labels"
    }
}
