package com.dante.workcycle.domain.backup

import com.dante.workcycle.data.local.entity.WorkEventEntity
import com.dante.workcycle.data.local.entity.WorkLogEntity
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCycleBackupRoomJsonMapperTest {

    @Test
    fun workEventsJsonContainsAllRawFields() {
        val json = WorkCycleBackupRoomJsonMapper.workEventsToJson(
            listOf(
                WorkEventEntity(
                    id = 7,
                    date = LocalDate.of(2026, 4, 30),
                    time = LocalTime.of(8, 15),
                    type = WorkEventType.CLOCK_IN,
                    note = "Start",
                    createdAt = 1714464900000L
                )
            )
        )

        assertTrue(json.contains(""""id": 7"""))
        assertTrue(json.contains(""""date": "2026-04-30""""))
        assertTrue(json.contains(""""time": "08:15""""))
        assertTrue(json.contains(""""type": "CLOCK_IN""""))
        assertTrue(json.contains(""""note": "Start""""))
        assertTrue(json.contains(""""createdAt": 1714464900000"""))
    }

    @Test
    fun auditMetadataIsPreserved() {
        val json = WorkCycleBackupRoomJsonMapper.workEventsToJson(
            listOf(
                WorkEventEntity(
                    id = 8,
                    date = LocalDate.of(2026, 4, 30),
                    time = LocalTime.of(12, 0),
                    type = WorkEventType.CLOCK_OUT,
                    createdAt = 1714478400000L,
                    editAuditOldDate = LocalDate.of(2026, 4, 29),
                    editAuditOldTime = LocalTime.of(11, 45),
                    editAuditNewDate = LocalDate.of(2026, 4, 30),
                    editAuditNewTime = LocalTime.of(12, 0),
                    editAuditEditedAt = 1714480000000L,
                    editAuditWasFutureTime = true,
                    editAuditSource = "MANUAL_EDIT"
                )
            )
        )

        assertTrue(json.contains(""""editAuditOldDate": "2026-04-29""""))
        assertTrue(json.contains(""""editAuditOldTime": "11:45""""))
        assertTrue(json.contains(""""editAuditNewDate": "2026-04-30""""))
        assertTrue(json.contains(""""editAuditNewTime": "12:00""""))
        assertTrue(json.contains(""""editAuditEditedAt": 1714480000000"""))
        assertTrue(json.contains(""""editAuditWasFutureTime": true"""))
        assertTrue(json.contains(""""editAuditSource": "MANUAL_EDIT""""))
    }

    @Test
    fun nullFieldsRemainExplicitJsonNull() {
        val json = WorkCycleBackupRoomJsonMapper.workEventsToJson(
            listOf(
                WorkEventEntity(
                    id = 9,
                    date = LocalDate.of(2026, 4, 30),
                    time = LocalTime.of(9, 0),
                    type = WorkEventType.BREAK_START,
                    note = null,
                    createdAt = 1714467600000L
                )
            )
        )

        assertTrue(json.contains(""""note": null"""))
        assertTrue(json.contains(""""editAuditOldDate": null"""))
        assertTrue(json.contains(""""editAuditEditedAt": null"""))
        assertTrue(json.contains(""""editAuditSource": null"""))
    }

    @Test
    fun utf8NoteIsPreserved() {
        val json = WorkCycleBackupRoomJsonMapper.workEventsToJson(
            listOf(
                WorkEventEntity(
                    id = 10,
                    date = LocalDate.of(2026, 4, 30),
                    time = LocalTime.of(10, 0),
                    type = WorkEventType.MEAL,
                    note = "Malica ščž",
                    createdAt = 1714471200000L
                )
            )
        )

        assertTrue(json.contains("Malica ščž"))
    }

    @Test
    fun workLogsJsonContainsLegacyAggregateFields() {
        val json = WorkCycleBackupRoomJsonMapper.workLogsToJson(
            listOf(
                WorkLogEntity(
                    id = 3,
                    date = LocalDate.of(2026, 4, 30),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    breakMinutes = 30,
                    note = "Legacy",
                    primaryLabel = "A",
                    secondaryLabel = "O1",
                    statusLabel = "Dopust",
                    createdAt = 1714464000000L,
                    updatedAt = 1714492800000L
                )
            )
        )

        assertTrue(json.contains(""""id": 3"""))
        assertTrue(json.contains(""""startTime": "08:00""""))
        assertTrue(json.contains(""""endTime": "16:00""""))
        assertTrue(json.contains(""""breakMinutes": 30"""))
        assertTrue(json.contains(""""primaryLabel": "A""""))
        assertTrue(json.contains(""""secondaryLabel": "O1""""))
        assertTrue(json.contains(""""statusLabel": "Dopust""""))
        assertTrue(json.contains(""""updatedAt": 1714492800000"""))
    }
}
