package com.dante.workcycle.domain.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCycleBackupValidatorTest {

    @Test
    fun validBackupZipReturnsValidResultAndPreviewCounts() {
        val result = validate(sampleBackupZip())

        assertTrue(result.isValid)
        assertNotNull(result.preview)
        assertEquals(1, result.preview?.workEventCount)
        assertEquals(1, result.preview?.workLogCount)
        assertTrue(result.preview?.prefsSegmentNames?.contains("work_settings_prefs") == true)
        assertTrue(result.preview?.hasWorkEventAuditFields == true)
    }

    @Test
    fun missingManifestReturnsError() {
        val result = validate(
            zipOf(
                "room/work_events.json" to "[]",
                "room/work_logs.json" to "[]"
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "MISSING_MANIFEST" })
        assertEquals(null, result.preview)
    }

    @Test
    fun missingWorkEventsReturnsError() {
        val result = validate(
            zipOf(
                "manifest.json" to sampleManifest().toJsonString(),
                "room/work_logs.json" to "[]"
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "MISSING_WORK_EVENTS" })
    }

    @Test
    fun missingWorkLogsReturnsError() {
        val result = validate(
            zipOf(
                "manifest.json" to sampleManifest().toJsonString(),
                "room/work_events.json" to "[]"
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "MISSING_WORK_LOGS" })
    }

    @Test
    fun invalidJsonInWorkEventsReturnsError() {
        val result = validate(
            samplePayload(
                roomWorkEventsJson = "{not-json}",
                roomWorkLogsJson = "[]"
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "INVALID_JSON_ARRAY" })
    }

    @Test
    fun invalidJsonInPrefsReturnsError() {
        val result = validate(
            samplePayload(
                prefsJsonByName = mapOf("work_settings_prefs" to "[1,2,3]")
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "INVALID_JSON_OBJECT" })
    }

    @Test
    fun unsupportedBackupFormatVersionReturnsError() {
        val result = validate(
            samplePayload(
                manifest = sampleManifest(backupFormatVersion = 2)
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "UNSUPPORTED_BACKUP_FORMAT_VERSION" })
    }

    @Test
    fun packageMismatchReturnsError() {
        val result = WorkCycleBackupValidator.validate(
            inputStream = ByteArrayInputStream(sampleBackupZip()),
            expectedPackageName = "com.example.other",
            currentDatabaseVersion = 3
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "PACKAGE_NAME_MISMATCH" })
    }

    @Test
    fun databaseVersionDifferenceReturnsWarning() {
        val result = WorkCycleBackupValidator.validate(
            inputStream = ByteArrayInputStream(sampleBackupZip()),
            expectedPackageName = "com.dante.workcycle",
            currentDatabaseVersion = 4
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "DATABASE_VERSION_DIFFERENT" })
    }

    @Test
    fun utf8ContentRemainsReadable() {
        val result = validate(
            samplePayload(
                roomWorkEventsJson = """
                    [
                      {
                        "id": 1,
                        "date": "2026-04-30",
                        "time": "08:00",
                        "type": "CLOCK_IN",
                        "note": "ščž ŠČŽ",
                        "createdAt": 1714464000000,
                        "editAuditOldDate": null,
                        "editAuditOldTime": null,
                        "editAuditNewDate": null,
                        "editAuditNewTime": null,
                        "editAuditEditedAt": null,
                        "editAuditWasFutureTime": false,
                        "editAuditSource": null
                      }
                    ]
                """.trimIndent()
            )
        )

        assertTrue(result.isValid)
        assertEquals(1, result.preview?.workEventCount)
    }

    @Test
    fun emptyButValidWorkSegmentsProduceWarnings() {
        val result = validate(
            samplePayload(
                roomWorkEventsJson = "[]",
                roomWorkLogsJson = "[]"
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "EMPTY_WORK_EVENTS" })
        assertTrue(result.warnings.any { it.code == "EMPTY_WORK_LOGS" })
    }

    @Test
    fun unexpectedPrefsSegmentReturnsWarning() {
        val result = validate(
            samplePayload(
                prefsJsonByName = mapOf(
                    "unexpected_prefs" to "{}"
                )
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "UNEXPECTED_PREFS_SEGMENT" })
    }

    @Test
    fun workSessionSnapshotPrefsReturnsWarning() {
        val result = validate(
            samplePayload(
                prefsJsonByName = mapOf(
                    "work_session_snapshot_prefs" to "{}"
                )
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "EXCLUDED_SESSION_PREFS_SEGMENT" })
    }

    @Test
    fun missingAuditFieldsReturnWarningNotError() {
        val result = validate(
            samplePayload(
                roomWorkEventsJson = """
                    [
                      {
                        "id": 1,
                        "date": "2026-04-30",
                        "time": "08:00",
                        "type": "CLOCK_IN",
                        "createdAt": 1714464000000
                      }
                    ]
                """.trimIndent()
            )
        )

        assertTrue(result.isValid)
        assertFalse(result.preview?.hasWorkEventAuditFields ?: true)
        assertTrue(result.warnings.any { it.code == "MISSING_WORK_EVENT_AUDIT_FIELDS" })
    }

    private fun validate(bytes: ByteArray): WorkCycleBackupValidationResult {
        return WorkCycleBackupValidator.validate(
            inputStream = ByteArrayInputStream(bytes),
            expectedPackageName = "com.dante.workcycle",
            currentDatabaseVersion = 3
        )
    }

    private fun validate(payload: WorkCycleBackupPayload): WorkCycleBackupValidationResult {
        return validate(WorkCycleBackupWriter.toByteArray(payload))
    }

    private fun sampleBackupZip(): ByteArray {
        return WorkCycleBackupWriter.toByteArray(samplePayload())
    }

    private fun samplePayload(
        manifest: WorkCycleBackupManifest = sampleManifest(),
        roomWorkEventsJson: String = """
            [
              {
                "id": 1,
                "date": "2026-04-30",
                "time": "08:00",
                "type": "CLOCK_IN",
                "note": "Prihod",
                "createdAt": 1714464000000,
                "editAuditOldDate": null,
                "editAuditOldTime": null,
                "editAuditNewDate": null,
                "editAuditNewTime": null,
                "editAuditEditedAt": null,
                "editAuditWasFutureTime": false,
                "editAuditSource": null
              }
            ]
        """.trimIndent(),
        roomWorkLogsJson: String = """
            [
              {
                "id": 1,
                "date": "2026-04-30",
                "startTime": "08:00",
                "endTime": "16:00",
                "breakMinutes": 30,
                "note": null,
                "primaryLabel": "A",
                "secondaryLabel": null,
                "statusLabel": null,
                "createdAt": 1714464000000,
                "updatedAt": 1714492800000
              }
            ]
        """.trimIndent(),
        prefsJsonByName: Map<String, String> = mapOf(
            "work_settings_prefs" to """
                {
                  "daily_target_minutes": 480,
                  "break_accounting_mode": "UNPAID"
                }
            """.trimIndent()
        )
    ): WorkCycleBackupPayload {
        return WorkCycleBackupPayload(
            manifest = manifest,
            roomWorkEventsJson = roomWorkEventsJson,
            roomWorkLogsJson = roomWorkLogsJson,
            prefsJsonByName = prefsJsonByName
        )
    }

    private fun sampleManifest(
        backupFormatVersion: Int = 1
    ): WorkCycleBackupManifest {
        return WorkCycleBackupManifest(
            backupFormatVersion = backupFormatVersion,
            createdAt = 1_777_000_000_000L,
            appVersionName = "3.0",
            appVersionCode = 24,
            databaseVersion = 3,
            packageName = "com.dante.workcycle"
        )
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output, StandardCharsets.UTF_8).use { zipOutputStream ->
            entries.forEach { (name, content) ->
                zipOutputStream.putNextEntry(ZipEntry(name).apply { time = 0L })
                zipOutputStream.write(content.toByteArray(StandardCharsets.UTF_8))
                zipOutputStream.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
