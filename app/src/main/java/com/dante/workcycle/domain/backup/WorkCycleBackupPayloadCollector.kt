package com.dante.workcycle.domain.backup

import android.content.Context
import com.dante.workcycle.data.local.db.AppDatabase

/**
 * Thin Android-dependent backup payload collector for full local export.
 *
 * This collector reads raw Room rows and raw SharedPreferences values, applies
 * explicit backup filtering rules, and returns a [WorkCycleBackupPayload]. It
 * does not perform ZIP writing, SAF access, restore/import, or any preference
 * mutation. Manual edit audit metadata must remain intact through the raw event
 * entity mapping path.
 */
class WorkCycleBackupPayloadCollector(
    context: Context,
    private val database: AppDatabase
) {

    private val appContext = context.applicationContext

    suspend fun collect(
        manifest: WorkCycleBackupManifest
    ): WorkCycleBackupPayload {
        val workEventsJson = WorkCycleBackupRoomJsonMapper.workEventsToJson(
            database.workEventDao().getAllOrdered()
        )
        val workLogsJson = WorkCycleBackupRoomJsonMapper.workLogsToJson(
            database.workLogDao().getAllOrdered()
        )

        val prefsJsonByName = WorkCycleBackupPrefsSpec.includedPrefsNames()
            .associateWith { prefsName ->
                val rawValues = appContext
                    .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    .all
                    .mapValues { it.value }

                WorkCycleBackupPrefsJsonMapper.toJson(
                    prefsName = prefsName,
                    rawValues = rawValues
                ) ?: "{}"
            }

        return WorkCycleBackupPayload(
            manifest = manifest,
            roomWorkEventsJson = workEventsJson,
            roomWorkLogsJson = workLogsJson,
            prefsJsonByName = prefsJsonByName
        )
    }
}
