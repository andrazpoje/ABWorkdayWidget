package com.dante.workcycle.data.prefs

import android.content.Context
import androidx.core.content.edit

class WorkSessionPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSnapshot(snapshot: WorkSessionSnapshot) {
        prefs.edit {
            putString(KEY_SESSION_CYCLE_LABEL, snapshot.cycleLabel)
            putString(KEY_SESSION_EXPECTED_START, snapshot.expectedStart)
            putString(KEY_SESSION_EXPECTED_END, snapshot.expectedEnd)
        }
    }

    fun getSnapshot(): WorkSessionSnapshot? {
        val cycleLabel = prefs.getString(KEY_SESSION_CYCLE_LABEL, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return WorkSessionSnapshot(
            cycleLabel = cycleLabel,
            expectedStart = prefs.getString(KEY_SESSION_EXPECTED_START, null)
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            expectedEnd = prefs.getString(KEY_SESSION_EXPECTED_END, null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        )
    }

    fun clearSnapshot() {
        prefs.edit {
            remove(KEY_SESSION_CYCLE_LABEL)
            remove(KEY_SESSION_EXPECTED_START)
            remove(KEY_SESSION_EXPECTED_END)
        }
    }

    data class WorkSessionSnapshot(
        val cycleLabel: String,
        // TODO(expected-time-layers): When secondary assignment labels can provide expected
        // times, include the resolved layer/source in this snapshot so an active session keeps
        // using the same expected-time rule even if the day labels are edited later.
        val expectedStart: String?,
        val expectedEnd: String?
    )

    private companion object {
        const val PREFS_NAME = "work_session_snapshot_prefs"
        const val KEY_SESSION_CYCLE_LABEL = "session_cycle_label"
        const val KEY_SESSION_EXPECTED_START = "session_expected_start"
        const val KEY_SESSION_EXPECTED_END = "session_expected_end"
    }
}
