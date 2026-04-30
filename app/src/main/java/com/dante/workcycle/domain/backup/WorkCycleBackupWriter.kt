package com.dante.workcycle.domain.backup

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a versioned full local backup ZIP from pre-built JSON payloads.
 *
 * This writer is export-only foundation code. It does not perform restore,
 * import, SAF access, Room reads, SharedPreferences reads, or user-data
 * filtering. Callers must provide already-filtered JSON segments so transient
 * and debug-only state stay out of the backup archive.
 */
object WorkCycleBackupWriter {

    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_ROOM_WORK_EVENTS = "room/work_events.json"
    private const val ENTRY_ROOM_WORK_LOGS = "room/work_logs.json"
    private val validPrefsNameRegex = Regex("[A-Za-z0-9._-]+")

    fun toByteArray(payload: WorkCycleBackupPayload): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writeTo(outputStream, payload)
        return outputStream.toByteArray()
    }

    fun writeTo(outputStream: OutputStream, payload: WorkCycleBackupPayload) {
        ZipOutputStream(outputStream, StandardCharsets.UTF_8).use { zipOutputStream ->
            writeEntry(zipOutputStream, ENTRY_MANIFEST, payload.manifest.toJsonString())
            writeEntry(zipOutputStream, ENTRY_ROOM_WORK_EVENTS, payload.roomWorkEventsJson)
            writeEntry(zipOutputStream, ENTRY_ROOM_WORK_LOGS, payload.roomWorkLogsJson)

            payload.prefsJsonByName
                .toSortedMap()
                .forEach { (prefsName, prefsJson) ->
                    writeEntry(
                        zipOutputStream,
                        "prefs/${toPrefsEntryName(prefsName)}",
                        prefsJson
                    )
                }
        }
    }

    private fun toPrefsEntryName(prefsName: String): String {
        require(prefsName.isNotBlank()) { "Preference segment name must not be blank." }
        require(validPrefsNameRegex.matches(prefsName)) {
            "Invalid preference segment name: $prefsName"
        }
        return "$prefsName.json"
    }

    private fun writeEntry(
        zipOutputStream: ZipOutputStream,
        path: String,
        content: String
    ) {
        val entry = ZipEntry(path).apply {
            time = 0L
        }

        zipOutputStream.putNextEntry(entry)
        zipOutputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        zipOutputStream.closeEntry()
    }
}
