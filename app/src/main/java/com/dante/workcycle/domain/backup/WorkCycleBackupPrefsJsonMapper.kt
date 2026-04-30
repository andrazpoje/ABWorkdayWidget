package com.dante.workcycle.domain.backup

/**
 * Backup foundation mapper for raw SharedPreferences values.
 *
 * This mapper serializes already-read preference maps into stable JSON objects
 * for full local backup export. It is not a restore layer. Callers must pass
 * raw persisted values and rely on [WorkCycleBackupPrefsSpec] so debug-only and
 * transient state never enters the backup archive.
 */
object WorkCycleBackupPrefsJsonMapper {

    fun toJson(
        prefsName: String,
        rawValues: Map<String, Any?>
    ): String? {
        val filteredValues = WorkCycleBackupPrefsSpec.filterValues(prefsName, rawValues)
        if (!WorkCycleBackupPrefsSpec.isIncludedPrefsFile(prefsName)) {
            return null
        }

        val fields = filteredValues.map { (key, value) ->
            key to toJsonValue(value)
        }

        return WorkCycleBackupJsonWriter.objectString(fields)
    }

    private fun toJsonValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is Boolean -> WorkCycleBackupJsonWriter.literal(value)
            is Int -> WorkCycleBackupJsonWriter.literal(value)
            is Long -> WorkCycleBackupJsonWriter.literal(value)
            is Float -> WorkCycleBackupJsonWriter.literal(value)
            is Double -> WorkCycleBackupJsonWriter.literal(value)
            is String -> WorkCycleBackupJsonWriter.literal(value)
            is Set<*> -> {
                require(value.all { it is String }) {
                    "Unsupported preference set value type. Only Set<String> is supported."
                }

                WorkCycleBackupJsonWriter.arrayString(
                    value.map { it as String }
                        .sorted()
                        .map { WorkCycleBackupJsonWriter.literal(it) }
                )
            }
            else -> throw IllegalArgumentException(
                "Unsupported preference value type: ${value::class.qualifiedName}"
            )
        }
    }
}
