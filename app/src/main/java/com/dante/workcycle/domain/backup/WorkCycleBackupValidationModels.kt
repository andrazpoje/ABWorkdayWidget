package com.dante.workcycle.domain.backup

/**
 * Severity used by backup validation diagnostics.
 *
 * Validation issues are meant for backup preview and future restore preflight.
 * They do not perform restore/import and they do not modify local data.
 */
enum class WorkCycleBackupIssueSeverity {
    WARNING,
    ERROR
}

/**
 * Structured backup validation issue for preview and diagnostics.
 */
data class WorkCycleBackupIssue(
    val severity: WorkCycleBackupIssueSeverity,
    val code: String,
    val message: String
)

/**
 * Parsed backup summary used by future preview and restore preflight flows.
 *
 * This is not restore logic. It summarizes what a backup appears to contain and
 * surfaces warnings/errors gathered during ZIP and JSON validation.
 */
data class WorkCycleBackupPreview(
    val backupFormatVersion: Int,
    val createdAt: Long,
    val appVersionName: String,
    val appVersionCode: Int,
    val databaseVersion: Int,
    val packageName: String,
    val workEventCount: Int,
    val workLogCount: Int,
    val prefsSegmentNames: List<String>,
    val hasWorkEventAuditFields: Boolean,
    val warnings: List<WorkCycleBackupIssue>,
    val errors: List<WorkCycleBackupIssue>
)

/**
 * Result of validating a full local WorkCycle backup ZIP.
 *
 * Validation is a preflight/preview foundation. It does not restore, import,
 * prove legal compliance, or write local data.
 */
data class WorkCycleBackupValidationResult(
    val preview: WorkCycleBackupPreview?,
    val issues: List<WorkCycleBackupIssue>
) {
    val warnings: List<WorkCycleBackupIssue>
        get() = issues.filter { it.severity == WorkCycleBackupIssueSeverity.WARNING }

    val errors: List<WorkCycleBackupIssue>
        get() = issues.filter { it.severity == WorkCycleBackupIssueSeverity.ERROR }

    val isValid: Boolean
        get() = errors.isEmpty()
}
