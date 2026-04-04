package com.dante.workcycle.domain.schedule

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.util.Locale

class ManualScheduleRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSecondaryManualLabel(date: LocalDate): String? {
        return prefs.getString(buildSecondaryKey(date), null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun setSecondaryManualLabel(date: LocalDate, label: String?) {
        val key = buildSecondaryKey(date)
        prefs.edit().apply {
            if (label.isNullOrBlank()) {
                remove(key)
            } else {
                putString(key, label.trim())
            }
        }.apply()
    }

    fun removeSecondaryManualLabelsByNames(names: List<String>) {
        if (names.isEmpty()) return

        val normalizedNames = names
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .toSet()

        if (normalizedNames.isEmpty()) return

        val editor = prefs.edit()
        var changed = false

        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(SECONDARY_PREFIX)) return@forEach

            val stored = (value as? String)
                ?.trim()
                ?.lowercase(Locale.getDefault())
                ?.takeIf { it.isNotBlank() }
                ?: return@forEach

            if (stored in normalizedNames) {
                editor.remove(key)
                changed = true
            }
        }

        if (changed) {
            editor.apply()
        }
    }

    private fun buildSecondaryKey(date: LocalDate): String {
        return "$SECONDARY_PREFIX$date"
    }

    companion object {
        private const val PREFS_NAME = "manual_schedule_prefs"
        private const val SECONDARY_PREFIX = "secondary_"
    }
}