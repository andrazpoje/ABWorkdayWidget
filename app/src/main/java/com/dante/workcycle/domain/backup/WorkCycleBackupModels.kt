package com.dante.workcycle.domain.backup

/**
 * Versioned metadata for a full local WorkCycle backup export.
 *
 * This manifest describes the backup container itself, not a CSV-style report.
 * The backup format is intended for full local export/restore flows, while CSV
 * remains a separate raw-evidence reporting format. When Work Log events are
 * wired into this payload, their manual edit audit metadata must remain intact.
 */
data class WorkCycleBackupManifest(
    val backupFormatVersion: Int,
    val createdAt: Long,
    val appVersionName: String,
    val appVersionCode: Int,
    val databaseVersion: Int,
    val packageName: String
) {
    fun toJsonString(): String {
        return buildString {
            appendLine("{")
            appendLine("""  "backupFormatVersion": $backupFormatVersion,""")
            appendLine("""  "createdAt": $createdAt,""")
            appendLine("""  "appVersionName": "${escapeJson(appVersionName)}",""")
            appendLine("""  "appVersionCode": $appVersionCode,""")
            appendLine("""  "databaseVersion": $databaseVersion,""")
            appendLine("""  "packageName": "${escapeJson(packageName)}"""")
            append('}')
        }
    }

    private fun escapeJson(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}

/**
 * Pre-collected backup payload for export-only ZIP writing.
 *
 * This layer does not read Room or SharedPreferences on its own. Higher layers
 * are responsible for collecting raw Room rows and filtering out debug-only or
 * transient preferences before constructing this payload. Restore/import flows
 * are intentionally out of scope for this foundation.
 */
data class WorkCycleBackupPayload(
    val manifest: WorkCycleBackupManifest,
    val roomWorkEventsJson: String = "[]",
    val roomWorkLogsJson: String = "[]",
    val prefsJsonByName: Map<String, String> = emptyMap()
)
