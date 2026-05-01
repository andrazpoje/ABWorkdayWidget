package com.dante.workcycle.domain.backup

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

internal data class WorkCycleBackupZipReadResult(
    val entries: Map<String, String>,
    val issues: List<WorkCycleBackupIssue>
)

internal object WorkCycleBackupZipReader {

    fun read(inputStream: InputStream): WorkCycleBackupZipReadResult {
        val entries = linkedMapOf<String, String>()
        val issues = mutableListOf<WorkCycleBackupIssue>()

        ZipInputStream(inputStream, StandardCharsets.UTF_8).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val path = entry.name.orEmpty()

                when {
                    !isSafePath(path) -> {
                        issues += WorkCycleBackupIssue(
                            severity = WorkCycleBackupIssueSeverity.ERROR,
                            code = "INVALID_ENTRY_PATH",
                            message = "Invalid ZIP entry path: $path"
                        )
                    }
                    entries.containsKey(path) -> {
                        issues += WorkCycleBackupIssue(
                            severity = WorkCycleBackupIssueSeverity.ERROR,
                            code = "DUPLICATE_ENTRY",
                            message = "Duplicate ZIP entry: $path"
                        )
                    }
                    else -> {
                        entries[path] = zipInputStream.readBytes()
                            .toString(StandardCharsets.UTF_8)
                    }
                }

                zipInputStream.closeEntry()
            }
        }

        return WorkCycleBackupZipReadResult(
            entries = entries,
            issues = issues
        )
    }

    private fun isSafePath(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.startsWith("/") || path.startsWith("\\")) return false
        if (path.contains('\\')) return false

        val segments = path.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) {
            return false
        }

        return true
    }
}
