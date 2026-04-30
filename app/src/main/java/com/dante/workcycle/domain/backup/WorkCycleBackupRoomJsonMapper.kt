package com.dante.workcycle.domain.backup

import com.dante.workcycle.data.local.entity.WorkEventEntity
import com.dante.workcycle.data.local.entity.WorkLogEntity

/**
 * Backup foundation mapper for raw Room-backed WorkCycle data.
 *
 * This mapper serializes persisted entity fields into stable JSON segments for
 * full local backup export. It is not a restore layer and it must preserve raw
 * event audit metadata without mapping through UI or domain summaries.
 */
object WorkCycleBackupRoomJsonMapper {

    fun workEventsToJson(events: List<WorkEventEntity>): String {
        return WorkCycleBackupJsonWriter.arrayString(
            events.map { event ->
                WorkCycleBackupJsonWriter.objectString(
                    listOf(
                        "id" to WorkCycleBackupJsonWriter.literal(event.id),
                        "date" to WorkCycleBackupJsonWriter.literal(event.date.toString()),
                        "time" to WorkCycleBackupJsonWriter.literal(event.time.toString()),
                        "type" to WorkCycleBackupJsonWriter.literal(event.type.name),
                        "note" to WorkCycleBackupJsonWriter.literal(event.note),
                        "createdAt" to WorkCycleBackupJsonWriter.literal(event.createdAt),
                        "editAuditOldDate" to WorkCycleBackupJsonWriter.literal(event.editAuditOldDate?.toString()),
                        "editAuditOldTime" to WorkCycleBackupJsonWriter.literal(event.editAuditOldTime?.toString()),
                        "editAuditNewDate" to WorkCycleBackupJsonWriter.literal(event.editAuditNewDate?.toString()),
                        "editAuditNewTime" to WorkCycleBackupJsonWriter.literal(event.editAuditNewTime?.toString()),
                        "editAuditEditedAt" to event.editAuditEditedAt?.let {
                            WorkCycleBackupJsonWriter.literal(it)
                        }.orEmpty().ifBlank { "null" },
                        "editAuditWasFutureTime" to WorkCycleBackupJsonWriter.literal(event.editAuditWasFutureTime),
                        "editAuditSource" to WorkCycleBackupJsonWriter.literal(event.editAuditSource)
                    )
                )
            }
        )
    }

    fun workLogsToJson(logs: List<WorkLogEntity>): String {
        return WorkCycleBackupJsonWriter.arrayString(
            logs.map { log ->
                WorkCycleBackupJsonWriter.objectString(
                    listOf(
                        "id" to WorkCycleBackupJsonWriter.literal(log.id),
                        "date" to WorkCycleBackupJsonWriter.literal(log.date.toString()),
                        "startTime" to WorkCycleBackupJsonWriter.literal(log.startTime?.toString()),
                        "endTime" to WorkCycleBackupJsonWriter.literal(log.endTime?.toString()),
                        "breakMinutes" to WorkCycleBackupJsonWriter.literal(log.breakMinutes),
                        "note" to WorkCycleBackupJsonWriter.literal(log.note),
                        "primaryLabel" to WorkCycleBackupJsonWriter.literal(log.primaryLabel),
                        "secondaryLabel" to WorkCycleBackupJsonWriter.literal(log.secondaryLabel),
                        "statusLabel" to WorkCycleBackupJsonWriter.literal(log.statusLabel),
                        "createdAt" to WorkCycleBackupJsonWriter.literal(log.createdAt),
                        "updatedAt" to WorkCycleBackupJsonWriter.literal(log.updatedAt)
                    )
                )
            }
        )
    }
}
