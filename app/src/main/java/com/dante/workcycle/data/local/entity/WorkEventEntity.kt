package com.dante.workcycle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room representation of a Work Log event.
 *
 * Manual-edit audit fields are nullable so pre-audit events can be read without
 * migration-side data synthesis. The event type must remain unchanged during a
 * time correction.
 */
@Entity(tableName = "work_events")
data class WorkEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val type: WorkEventType,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val editAuditOldDate: LocalDate? = null,
    val editAuditOldTime: LocalTime? = null,
    val editAuditNewDate: LocalDate? = null,
    val editAuditNewTime: LocalTime? = null,
    val editAuditEditedAt: Long? = null,
    val editAuditWasFutureTime: Boolean = false,
    val editAuditSource: String? = null
)
