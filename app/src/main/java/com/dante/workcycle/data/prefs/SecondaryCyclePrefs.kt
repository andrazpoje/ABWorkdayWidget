package com.dante.workcycle.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.domain.model.AssignmentCycleAdvanceMode
import com.dante.workcycle.domain.model.CycleMode
import java.time.LocalDate

class SecondaryCyclePrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_ENABLED, enabled)
        }
    }

    fun getMode(): CycleMode {
        val saved = prefs.getString(KEY_MODE, CycleMode.MANUAL.name)
        return CycleMode.fromString(saved)
    }

    fun setMode(mode: CycleMode) {
        prefs.edit {
            putString(KEY_MODE, mode.name)
        }
    }

    fun getCycle(): List<String> {
        val saved = prefs.getString(KEY_CYCLE, null)

        val parsed = saved
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        return parsed.ifEmpty { defaultCycle() }
    }

    fun setCycle(labels: List<String>) {
        val cleaned = labels
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val finalCycle = cleaned.ifEmpty { defaultCycle() }
        prefs.edit {
            putString(KEY_CYCLE, finalCycle.joinToString(","))
        }
    }

    fun getStartDate(): LocalDate {
        val saved = prefs.getString(KEY_START_DATE, null)

        return try {
            if (saved.isNullOrBlank()) DateProvider.today() else LocalDate.parse(saved)
        } catch (_: Exception) {
            DateProvider.today()
        }
    }

    fun setStartDate(date: LocalDate) {
        prefs.edit {
            putString(KEY_START_DATE, date.toString())
        }
    }

    fun getFirstCycleDay(): String {
        val cycle = getCycle()
        val fallback = cycle.firstOrNull().orEmpty()

        return prefs.getString(KEY_FIRST_CYCLE_DAY, fallback)
            ?.trim()
            .orEmpty()
            .ifBlank { fallback }
    }

    fun setFirstCycleDay(label: String) {
        prefs.edit {
            putString(KEY_FIRST_CYCLE_DAY, label.trim())
        }
    }

    fun getAdvanceMode(): AssignmentCycleAdvanceMode {
        val saved = prefs.getString(
            KEY_ADVANCE_MODE,
            AssignmentCycleAdvanceMode.WORKING_DAYS_ONLY.name
        )

        return AssignmentCycleAdvanceMode.fromString(saved)
    }

    fun setAdvanceMode(mode: AssignmentCycleAdvanceMode) {
        prefs.edit {
            putString(KEY_ADVANCE_MODE, mode.name)
        }
    }

    private fun defaultCycle(): List<String> {
        return listOf("O1", "O2", "O3", "O4", "O5")
    }

    companion object {
        private const val PREFS_NAME = "secondary_cycle_prefs"

        private const val KEY_ENABLED = "secondary_enabled"
        private const val KEY_MODE = "secondary_mode"
        private const val KEY_CYCLE = "secondary_cycle"
        private const val KEY_START_DATE = "secondary_start_date"
        private const val KEY_FIRST_CYCLE_DAY = "secondary_first_cycle_day"
        private const val KEY_ADVANCE_MODE = "secondary_advance_mode"
    }
}
