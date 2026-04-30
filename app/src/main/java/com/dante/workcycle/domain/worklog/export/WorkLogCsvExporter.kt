package com.dante.workcycle.domain.worklog.export

import com.dante.workcycle.domain.model.WorkEvent

/**
 * Builds a raw-evidence CSV export from Work Log events.
 *
 * This export is intended for inspection, sharing, and external analysis. It is
 * not a full backup format and is not designed for complete restore. Derived
 * accounting summaries such as effective work, credited work, or balance are
 * intentionally excluded from this first CSV export so the file stays focused on
 * raw event evidence. Manual-edit audit metadata must remain included.
 *
 * Events are sorted by date, time, and id before export so the output is stable
 * even if callers provide an unsorted list.
 */
object WorkLogCsvExporter {

    private val headerColumns = listOf(
        "id",
        "date",
        "time",
        "type",
        "note",
        "createdAt",
        "editAuditOldDate",
        "editAuditOldTime",
        "editAuditNewDate",
        "editAuditNewTime",
        "editAuditEditedAt",
        "editAuditWasFutureTime",
        "editAuditSource"
    )

    fun export(events: List<WorkEvent>): String {
        val builder = StringBuilder()
        writeTo(builder, events)
        return builder.toString()
    }

    fun writeTo(appendable: Appendable, events: List<WorkEvent>) {
        appendable.appendLine(headerColumns.joinToString(","))

        events.sortedWith(
            compareBy<WorkEvent> { it.date }
                .thenBy { it.time }
                .thenBy { it.id }
        ).forEach { event ->
            val audit = event.editAudit
            val columns = listOf(
                event.id.toString(),
                event.date.toString(),
                event.time.toString(),
                event.type.name,
                event.note,
                event.createdAt.toString(),
                audit?.oldDate?.toString(),
                audit?.oldTime?.toString(),
                audit?.newDate?.toString(),
                audit?.newTime?.toString(),
                audit?.editedAt?.toString(),
                audit?.wasFutureTime?.toString(),
                audit?.source
            )

            appendable.appendLine(
                columns.joinToString(",") { escapeCsvField(it) }
            )
        }
    }

    private fun escapeCsvField(value: String?): String {
        if (value == null) return ""

        val mustQuote = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!mustQuote) return value

        return buildString {
            append('"')
            value.forEach { char ->
                if (char == '"') {
                    append("\"\"")
                } else {
                    append(char)
                }
            }
            append('"')
        }
    }
}
