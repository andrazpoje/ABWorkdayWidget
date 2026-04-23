package com.dante.workcycle.domain.schedule

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.time.LocalDate

class StatusRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStatusLabel(date: LocalDate): String? {
        return getStatusTags(date).firstOrNull()
            ?: prefs.getString(buildLegacyKey(date), null)
            ?.trim()
            ?.removeSuffix("*")
            ?.takeIf { it.isNotBlank() }
    }

    fun setStatusLabel(date: LocalDate, label: String?) {
        setStatusTags(date, listOfNotNull(label))
    }

    fun getStatusTags(date: LocalDate): LinkedHashSet<String> {
        val saved = prefs.getString(buildTagsKey(date), null)
        val parsed = if (saved.isNullOrBlank()) {
            linkedSetOf()
        } else {
            runCatching { parseTags(saved) }.getOrDefault(linkedSetOf())
        }

        if (parsed.isNotEmpty()) {
            return parsed
        }

        return prefs.getString(buildLegacyKey(date), null)
            ?.trim()
            ?.removeSuffix("*")
            ?.takeIf { it.isNotBlank() }
            ?.let { linkedSetOf(it) }
            ?: linkedSetOf()
    }

    fun setStatusTags(date: LocalDate, labels: Collection<String>) {
        val legacyKey = buildLegacyKey(date)
        val tagsKey = buildTagsKey(date)
        val normalized = LinkedHashSet(
            labels.mapNotNull { label ->
                label.trim()
                    .removeSuffix("*")
                    .takeIf { it.isNotBlank() }
            }
        )

        prefs.edit().apply {
            if (normalized.isEmpty()) {
                remove(legacyKey)
                remove(tagsKey)
            } else {
                putString(legacyKey, normalized.first())
                putString(tagsKey, serializeTags(normalized))
            }
        }.apply()
    }

    private fun buildLegacyKey(date: LocalDate): String {
        return "$STATUS_PREFIX$date"
    }

    private fun buildTagsKey(date: LocalDate): String {
        return "$STATUS_TAGS_PREFIX$date"
    }

    private fun serializeTags(tags: Set<String>): String {
        return JSONArray().apply {
            tags.forEach { put(it) }
        }.toString()
    }

    private fun parseTags(raw: String): LinkedHashSet<String> {
        val result = linkedSetOf<String>()
        val json = JSONArray(raw)

        for (index in 0 until json.length()) {
            json.optString(index)
                ?.trim()
                ?.removeSuffix("*")
                ?.takeIf { it.isNotBlank() }
                ?.let(result::add)
        }

        return result
    }

    companion object {
        private const val PREFS_NAME = "status_schedule_prefs"
        private const val STATUS_PREFIX = "status_"
        private const val STATUS_TAGS_PREFIX = "status_tags_"
    }
}
