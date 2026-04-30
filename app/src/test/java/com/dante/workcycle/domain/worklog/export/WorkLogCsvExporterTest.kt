package com.dante.workcycle.domain.worklog.export

import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventEditAudit
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkLogCsvExporterTest {

    @Test
    fun exportStartsWithStableHeaderRow() {
        val csv = WorkLogCsvExporter.export(emptyList())

        assertEquals(
            "id,date,time,type,note,createdAt,editAuditOldDate,editAuditOldTime,editAuditNewDate,editAuditNewTime,editAuditEditedAt,editAuditWasFutureTime,editAuditSource\n",
            csv
        )
    }

    @Test
    fun exportWritesBasicEventWithoutAuditFields() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(
                    id = 1,
                    date = date(2026, 4, 30),
                    time = time(8, 0),
                    type = WorkEventType.CLOCK_IN,
                    note = null,
                    createdAt = 1000L
                )
            )
        )

        assertEquals(
            listOf(
                "id,date,time,type,note,createdAt,editAuditOldDate,editAuditOldTime,editAuditNewDate,editAuditNewTime,editAuditEditedAt,editAuditWasFutureTime,editAuditSource",
                "1,2026-04-30,08:00,CLOCK_IN,,1000,,,,,,,"
            ).joinToString("\n", postfix = "\n"),
            csv
        )
    }

    @Test
    fun exportQuotesNoteContainingComma() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(
                    id = 1,
                    date = date(2026, 4, 30),
                    time = time(8, 0),
                    type = WorkEventType.NOTE,
                    note = "route, depot",
                    createdAt = 1000L
                )
            )
        )

        assertEquals(
            "1,2026-04-30,08:00,NOTE,\"route, depot\",1000,,,,,,,",
            csv.lineSequence().drop(1).first()
        )
    }

    @Test
    fun exportEscapesQuoteInsideNote() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(
                    id = 1,
                    date = date(2026, 4, 30),
                    time = time(8, 0),
                    type = WorkEventType.NOTE,
                    note = "shift \"A\"",
                    createdAt = 1000L
                )
            )
        )

        assertEquals(
            "1,2026-04-30,08:00,NOTE,\"shift \"\"A\"\"\",1000,,,,,,,",
            csv.lineSequence().drop(1).first()
        )
    }

    @Test
    fun exportPreservesNewlineInsideQuotedNote() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(
                    id = 1,
                    date = date(2026, 4, 30),
                    time = time(8, 0),
                    type = WorkEventType.NOTE,
                    note = "line 1\nline 2",
                    createdAt = 1000L
                )
            )
        )

        assertEquals(
            "id,date,time,type,note,createdAt,editAuditOldDate,editAuditOldTime,editAuditNewDate,editAuditNewTime,editAuditEditedAt,editAuditWasFutureTime,editAuditSource\n" +
                "1,2026-04-30,08:00,NOTE,\"line 1\nline 2\",1000,,,,,,,\n",
            csv
        )
    }

    @Test
    fun exportWritesAuditMetadataFields() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(
                    id = 9,
                    date = date(2026, 4, 30),
                    time = time(9, 15),
                    type = WorkEventType.CLOCK_OUT,
                    note = "edited",
                    createdAt = 2000L,
                    audit = WorkEventEditAudit(
                        oldDate = date(2026, 4, 29),
                        oldTime = time(16, 0),
                        newDate = date(2026, 4, 30),
                        newTime = time(9, 15),
                        editedAt = 3000L,
                        wasFutureTime = true,
                        source = "manual_edit"
                    )
                )
            )
        )

        assertEquals(
            "9,2026-04-30,09:15,CLOCK_OUT,edited,2000,2026-04-29,16:00,2026-04-30,09:15,3000,true,manual_edit",
            csv.lineSequence().drop(1).first()
        )
    }

    @Test
    fun exportSortsByDateThenTimeThenId() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(id = 3, date = date(2026, 5, 1), time = time(8, 0), type = WorkEventType.CLOCK_IN),
                event(id = 2, date = date(2026, 4, 30), time = time(8, 0), type = WorkEventType.CLOCK_IN),
                event(id = 1, date = date(2026, 4, 30), time = time(8, 0), type = WorkEventType.NOTE),
                event(id = 4, date = date(2026, 4, 30), time = time(9, 0), type = WorkEventType.CLOCK_OUT)
            )
        )

        val rows = csv.lineSequence().drop(1).filter { it.isNotBlank() }.toList()

        assertEquals("1,2026-04-30,08:00,NOTE,,0,,,,,,,", rows[0])
        assertEquals("2,2026-04-30,08:00,CLOCK_IN,,0,,,,,,,", rows[1])
        assertEquals("4,2026-04-30,09:00,CLOCK_OUT,,0,,,,,,,", rows[2])
        assertEquals("3,2026-05-01,08:00,CLOCK_IN,,0,,,,,,,", rows[3])
    }

    @Test
    fun exportPreservesUtf8CharactersInNote() {
        val csv = WorkLogCsvExporter.export(
            listOf(
                event(
                    id = 1,
                    date = date(2026, 4, 30),
                    time = time(8, 0),
                    type = WorkEventType.NOTE,
                    note = "ščž ŠČŽ",
                    createdAt = 1000L
                )
            )
        )

        assertEquals(
            "1,2026-04-30,08:00,NOTE,ščž ŠČŽ,1000,,,,,,,",
            csv.lineSequence().drop(1).first()
        )
    }

    private fun event(
        id: Long,
        date: LocalDate,
        time: LocalTime,
        type: WorkEventType,
        note: String? = null,
        createdAt: Long = 0L,
        audit: WorkEventEditAudit? = null
    ): WorkEvent {
        return WorkEvent(
            id = id,
            date = date,
            time = time,
            type = type,
            note = note,
            createdAt = createdAt,
            editAudit = audit
        )
    }

    private fun date(year: Int, month: Int, day: Int): LocalDate {
        return LocalDate.of(year, month, day)
    }

    private fun time(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour, minute)
    }
}
