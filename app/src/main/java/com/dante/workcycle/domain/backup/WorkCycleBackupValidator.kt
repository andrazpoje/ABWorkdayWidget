package com.dante.workcycle.domain.backup

import java.io.InputStream

/**
 * Validates and previews a full local WorkCycle backup ZIP.
 *
 * This is a pure Kotlin preflight/preview foundation for future restore/import
 * flows and diagnostics. It does not restore data, does not write local state,
 * and does not guarantee legal/accounting compliance of the backup contents.
 *
 * Any future restore/import flow must use this validator as a preflight step
 * before attempting database or SharedPreferences writes.
 */
object WorkCycleBackupValidator {

    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_WORK_EVENTS = "room/work_events.json"
    private const val ENTRY_WORK_LOGS = "room/work_logs.json"
    private const val PREFS_PREFIX = "prefs/"
    private const val PREFS_SUFFIX = ".json"

    private val requiredManifestFields = listOf(
        "backupFormatVersion",
        "createdAt",
        "appVersionName",
        "appVersionCode",
        "databaseVersion",
        "packageName"
    )

    private val requiredAuditKeys = listOf(
        "editAuditOldDate",
        "editAuditOldTime",
        "editAuditNewDate",
        "editAuditNewTime",
        "editAuditEditedAt",
        "editAuditWasFutureTime",
        "editAuditSource"
    )
    private val allowedPrefsSegmentNames = WorkCycleBackupPrefsSpec.includedPrefsNames().toSet()

    fun validate(
        inputStream: InputStream,
        expectedPackageName: String,
        currentDatabaseVersion: Int,
        supportedBackupFormatVersions: Set<Int> = setOf(1)
    ): WorkCycleBackupValidationResult {
        val issues = mutableListOf<WorkCycleBackupIssue>()

        val zipContents = try {
            WorkCycleBackupZipReader.read(inputStream)
        } catch (_: Throwable) {
            return WorkCycleBackupValidationResult(
                preview = null,
                issues = listOf(
                    issue(
                        severity = WorkCycleBackupIssueSeverity.ERROR,
                        code = "INVALID_ZIP",
                        message = "Backup is not a valid ZIP archive."
                    )
                )
            )
        }

        issues += zipContents.issues
        issues += collectUnexpectedEntryWarnings(zipContents.entries.keys)

        val manifestJson = zipContents.entries[ENTRY_MANIFEST]
        if (manifestJson == null) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "MISSING_MANIFEST",
                message = "Backup is missing manifest.json."
            )
            return WorkCycleBackupValidationResult(preview = null, issues = issues)
        }

        val manifestObject = tryParseObject(
            manifestJson = manifestJson,
            segmentName = ENTRY_MANIFEST,
            issues = issues
        ) ?: return WorkCycleBackupValidationResult(preview = null, issues = issues)

        val missingManifestFields = requiredManifestFields.filterNot(manifestObject.fields::containsKey)
        if (missingManifestFields.isNotEmpty()) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "MISSING_MANIFEST_FIELDS",
                message = "Manifest is missing required fields: ${missingManifestFields.joinToString(", ")}."
            )
            return WorkCycleBackupValidationResult(preview = null, issues = issues)
        }

        val backupFormatVersion = manifestObject.getInt("backupFormatVersion")
        val createdAt = manifestObject.getLong("createdAt")
        val appVersionName = manifestObject.getString("appVersionName")
        val appVersionCode = manifestObject.getInt("appVersionCode")
        val databaseVersion = manifestObject.getInt("databaseVersion")
        val packageName = manifestObject.getString("packageName")

        if (backupFormatVersion !in supportedBackupFormatVersions) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "UNSUPPORTED_BACKUP_FORMAT_VERSION",
                message = "Unsupported backup format version: $backupFormatVersion."
            )
        }

        if (packageName != expectedPackageName) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "PACKAGE_NAME_MISMATCH",
                message = "Backup package name $packageName does not match expected package $expectedPackageName."
            )
        }

        if (databaseVersion != currentDatabaseVersion) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.WARNING,
                code = "DATABASE_VERSION_DIFFERENT",
                message = "Backup database version $databaseVersion differs from current version $currentDatabaseVersion."
            )
        }

        val workEventsJson = zipContents.entries[ENTRY_WORK_EVENTS]
        if (workEventsJson == null) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "MISSING_WORK_EVENTS",
                message = "Backup is missing room/work_events.json."
            )
        }

        val workLogsJson = zipContents.entries[ENTRY_WORK_LOGS]
        if (workLogsJson == null) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "MISSING_WORK_LOGS",
                message = "Backup is missing room/work_logs.json."
            )
        }

        val workEventsArray = workEventsJson?.let {
            tryParseArray(
                json = it,
                segmentName = ENTRY_WORK_EVENTS,
                issues = issues
            )
        }
        val workLogsArray = workLogsJson?.let {
            tryParseArray(
                json = it,
                segmentName = ENTRY_WORK_LOGS,
                issues = issues
            )
        }

        val prefsSegmentNames = mutableListOf<String>()
        zipContents.entries
            .filterKeys { it.startsWith(PREFS_PREFIX) }
            .toSortedMap()
            .forEach { (entryName, prefsJson) ->
                val prefsName = entryName.removePrefix(PREFS_PREFIX).removeSuffix(PREFS_SUFFIX)
                prefsSegmentNames += prefsName

                if (!entryName.endsWith(PREFS_SUFFIX)) {
                    issues += issue(
                        severity = WorkCycleBackupIssueSeverity.WARNING,
                        code = "UNEXPECTED_PREFS_ENTRY_NAME",
                        message = "Unexpected prefs entry name: $entryName."
                    )
                }

                tryParseObject(
                    manifestJson = prefsJson,
                    segmentName = entryName,
                    issues = issues
                )

                if (prefsName !in allowedPrefsSegmentNames) {
                    val issueCode = if (prefsName == "work_session_snapshot_prefs") {
                        "EXCLUDED_SESSION_PREFS_SEGMENT"
                    } else {
                        "UNEXPECTED_PREFS_SEGMENT"
                    }
                    issues += issue(
                        severity = WorkCycleBackupIssueSeverity.WARNING,
                        code = issueCode,
                        message = "Backup contains unexpected prefs segment: $prefsName."
                    )
                }
            }

        val workEventCount = workEventsArray?.items?.size ?: 0
        val workLogCount = workLogsArray?.items?.size ?: 0
        val hasWorkEventAuditFields = validateWorkEventAuditFields(workEventsArray, issues)

        if (workEventsArray != null && workEventsArray.items.isEmpty()) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.WARNING,
                code = "EMPTY_WORK_EVENTS",
                message = "Backup contains no Work Log events."
            )
        }

        if (workLogsArray != null && workLogsArray.items.isEmpty()) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.WARNING,
                code = "EMPTY_WORK_LOGS",
                message = "Backup contains no legacy Work Log aggregate rows."
            )
        }

        val preview = WorkCycleBackupPreview(
            backupFormatVersion = backupFormatVersion,
            createdAt = createdAt,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            databaseVersion = databaseVersion,
            packageName = packageName,
            workEventCount = workEventCount,
            workLogCount = workLogCount,
            prefsSegmentNames = prefsSegmentNames.sorted(),
            hasWorkEventAuditFields = hasWorkEventAuditFields,
            warnings = issues.filter { it.severity == WorkCycleBackupIssueSeverity.WARNING },
            errors = issues.filter { it.severity == WorkCycleBackupIssueSeverity.ERROR }
        )

        return WorkCycleBackupValidationResult(
            preview = preview,
            issues = issues
        )
    }

    private fun collectUnexpectedEntryWarnings(entryNames: Set<String>): List<WorkCycleBackupIssue> {
        return entryNames
            .filterNot(::isAllowedEntryPath)
            .sorted()
            .map { entryName ->
                issue(
                    severity = WorkCycleBackupIssueSeverity.WARNING,
                    code = "UNEXPECTED_ZIP_ENTRY",
                    message = "Backup contains unexpected ZIP entry: $entryName."
                )
            }
    }

    private fun isAllowedEntryPath(path: String): Boolean {
        return path == ENTRY_MANIFEST ||
            path == ENTRY_WORK_EVENTS ||
            path == ENTRY_WORK_LOGS ||
            (path.startsWith(PREFS_PREFIX) && path.endsWith(PREFS_SUFFIX))
    }

    private fun tryParseObject(
        manifestJson: String,
        segmentName: String,
        issues: MutableList<WorkCycleBackupIssue>
    ): WorkCycleBackupJsonObject? {
        return try {
            WorkCycleBackupJsonParser.parseObject(manifestJson)
        } catch (_: Throwable) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "INVALID_JSON_OBJECT",
                message = "Backup segment $segmentName is not a valid JSON object."
            )
            null
        }
    }

    private fun tryParseArray(
        json: String,
        segmentName: String,
        issues: MutableList<WorkCycleBackupIssue>
    ): WorkCycleBackupJsonArray? {
        return try {
            WorkCycleBackupJsonParser.parseArray(json)
        } catch (_: Throwable) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.ERROR,
                code = "INVALID_JSON_ARRAY",
                message = "Backup segment $segmentName is not a valid JSON array."
            )
            null
        }
    }

    private fun validateWorkEventAuditFields(
        workEventsArray: WorkCycleBackupJsonArray?,
        issues: MutableList<WorkCycleBackupIssue>
    ): Boolean {
        if (workEventsArray == null) return false

        var missingAuditKeyFound = false

        workEventsArray.items.forEachIndexed { index, item ->
            val eventObject = item as? WorkCycleBackupJsonObject
            if (eventObject == null) {
                issues += issue(
                    severity = WorkCycleBackupIssueSeverity.ERROR,
                    code = "INVALID_WORK_EVENT_ITEM",
                    message = "Work event at index $index is not a JSON object."
                )
                return@forEachIndexed
            }

            val missingKeys = requiredAuditKeys.filterNot(eventObject.fields::containsKey)
            if (missingKeys.isNotEmpty()) {
                missingAuditKeyFound = true
            }
        }

        if (missingAuditKeyFound) {
            issues += issue(
                severity = WorkCycleBackupIssueSeverity.WARNING,
                code = "MISSING_WORK_EVENT_AUDIT_FIELDS",
                message = "Some work event entries are missing manual edit audit keys."
            )
        }

        return !missingAuditKeyFound
    }

    private fun issue(
        severity: WorkCycleBackupIssueSeverity,
        code: String,
        message: String
    ): WorkCycleBackupIssue {
        return WorkCycleBackupIssue(
            severity = severity,
            code = code,
            message = message
        )
    }
}

private fun WorkCycleBackupJsonObject.getString(key: String): String {
    return (fields.getValue(key) as? WorkCycleBackupJsonString)?.value
        ?: error("Field $key is not a JSON string.")
}

private fun WorkCycleBackupJsonObject.getInt(key: String): Int {
    return (fields.getValue(key) as? WorkCycleBackupJsonNumber)?.rawValue?.toInt()
        ?: error("Field $key is not a JSON integer.")
}

private fun WorkCycleBackupJsonObject.getLong(key: String): Long {
    return (fields.getValue(key) as? WorkCycleBackupJsonNumber)?.rawValue?.toLong()
        ?: error("Field $key is not a JSON long.")
}
