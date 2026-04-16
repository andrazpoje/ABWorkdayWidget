package com.dante.workcycle.domain.schedule

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

class StatusRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStatusLabel(date: LocalDate): String? {
        return prefs.getString(buildKey(date), null)
            ?.trim()
            ?.removeSuffix("*")
            ?.takeIf { it.isNotBlank() }
    }

    fun setStatusLabel(date: LocalDate, label: String?) {
        val key = buildKey(date)

        prefs.edit().apply {
            if (label.isNullOrBlank()) {
                remove(key)
            } else {
                putString(key, label.trim().removeSuffix("*"))
            }
        }.apply()
    }

    fun removeStatusLabel(date: LocalDate) {
        prefs.edit().remove(buildKey(date)).apply()
    }

    private fun buildKey(date: LocalDate): String {
        return "$STATUS_PREFIX$date"
    }

    companion object {
        private const val PREFS_NAME = "status_schedule_prefs"
        private const val STATUS_PREFIX = "status_"
    }
}