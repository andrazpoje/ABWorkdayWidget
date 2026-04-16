package com.dante.workcycle.domain.model

enum class SecondaryCycleAdvanceMode {
    ALL_DAYS,
    WORKING_DAYS_ONLY;

    companion object {
        fun fromString(value: String?): SecondaryCycleAdvanceMode {
            return try {
                valueOf(value ?: WORKING_DAYS_ONLY.name)
            } catch (_: Exception) {
                WORKING_DAYS_ONLY
            }
        }
    }
}