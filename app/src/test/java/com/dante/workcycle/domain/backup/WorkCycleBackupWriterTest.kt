package com.dante.workcycle.domain.backup

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCycleBackupWriterTest {

    @Test
    fun zipContainsManifestAndExpectedSegments() {
        val entries = readZipEntries(sampleBackupZip())

        assertTrue(entries.containsKey("manifest.json"))
        assertTrue(entries.containsKey("room/work_events.json"))
        assertTrue(entries.containsKey("room/work_logs.json"))
        assertTrue(entries.containsKey("prefs/work_settings_prefs.json"))
    }

    @Test
    fun manifestContainsRequiredMetadata() {
        val manifestJson = readZipEntries(sampleBackupZip()).getValue("manifest.json")

        assertTrue(manifestJson.contains(""""backupFormatVersion": 1"""))
        assertTrue(manifestJson.contains(""""createdAt": 1777000000000"""))
        assertTrue(manifestJson.contains(""""appVersionName": "3.0""""))
        assertTrue(manifestJson.contains(""""appVersionCode": 24"""))
        assertTrue(manifestJson.contains(""""databaseVersion": 3"""))
        assertTrue(manifestJson.contains(""""packageName": "com.dante.abworkdaywidget""""))
    }

    @Test
    fun roomSegmentsRemainReadableJson() {
        val entries = readZipEntries(sampleBackupZip())
        val workEvents = entries.getValue("room/work_events.json")
        val workLogs = entries.getValue("room/work_logs.json")

        assertTrue(workEvents.trim().startsWith("["))
        assertTrue(workEvents.trim().endsWith("]"))
        assertTrue(workEvents.contains(""""type": "CLOCK_IN""""))
        assertTrue(workLogs.trim().startsWith("["))
        assertTrue(workLogs.trim().endsWith("]"))
        assertTrue(workLogs.contains(""""primaryLabel": "A""""))
    }

    @Test
    fun prefsSegmentsRemainReadableJson() {
        val prefsJson = readZipEntries(sampleBackupZip()).getValue("prefs/work_settings_prefs.json")

        assertTrue(prefsJson.trim().startsWith("{"))
        assertTrue(prefsJson.trim().endsWith("}"))
        assertTrue(prefsJson.contains(""""daily_target_minutes": 480"""))
        assertTrue(prefsJson.contains(""""break_accounting_mode": "UNPAID""""))
    }

    @Test
    fun writerRejectsInvalidPreferenceSegmentName() {
        val payload = samplePayload(
            prefsJsonByName = mapOf(
                "../invalid" to """{"value":true}"""
            )
        )

        val error = runCatching {
            WorkCycleBackupWriter.toByteArray(payload)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Invalid preference segment name: ../invalid", error?.message)
    }

    @Test
    fun utf8ContentIsPreservedInsideZipEntries() {
        val payload = samplePayload(
            roomWorkEventsJson = """
                [
                  {
                    "id": 1,
                    "note": "ščž ŠČŽ"
                  }
                ]
            """.trimIndent()
        )

        val workEventsJson = readZipEntries(
            WorkCycleBackupWriter.toByteArray(payload)
        ).getValue("room/work_events.json")

        assertTrue(workEventsJson.contains("ščž ŠČŽ"))
    }

    @Test
    fun zipContentsAreDeterministicByEntryContent() {
        val firstEntries = readZipEntries(sampleBackupZip())
        val secondEntries = readZipEntries(sampleBackupZip())

        assertEquals(firstEntries, secondEntries)
    }

    private fun sampleBackupZip(): ByteArray {
        return WorkCycleBackupWriter.toByteArray(samplePayload())
    }

    private fun samplePayload(
        roomWorkEventsJson: String = """
            [
              {
                "id": 1,
                "date": "2026-04-30",
                "time": "08:00",
                "type": "CLOCK_IN",
                "editAuditSource": "manual_edit"
              }
            ]
        """.trimIndent(),
        roomWorkLogsJson: String = """
            [
              {
                "id": 1,
                "date": "2026-04-30",
                "primaryLabel": "A"
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
            manifest = WorkCycleBackupManifest(
                backupFormatVersion = 1,
                createdAt = 1_777_000_000_000L,
                appVersionName = "3.0",
                appVersionCode = 24,
                databaseVersion = 3,
                packageName = "com.dante.abworkdaywidget"
            ),
            roomWorkEventsJson = roomWorkEventsJson,
            roomWorkLogsJson = roomWorkLogsJson,
            prefsJsonByName = prefsJsonByName
        )
    }

    private fun readZipEntries(bytes: ByteArray): Map<String, String> {
        val entries = linkedMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(bytes), StandardCharsets.UTF_8).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                entries[entry.name] = zipInputStream.readBytes().toString(StandardCharsets.UTF_8)
                zipInputStream.closeEntry()
            }
        }

        return entries
    }
}
