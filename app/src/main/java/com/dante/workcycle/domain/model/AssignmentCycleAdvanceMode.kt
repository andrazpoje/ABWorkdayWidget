package com.dante.workcycle.domain.model

enum class AssignmentCycleAdvanceMode {
    ALL_DAYS,
    WORKING_DAYS_ONLY;

    companion object {
        fun fromString(value: String?): AssignmentCycleAdvanceMode {
            return entries.firstOrNull { it.name == value } ?: WORKING_DAYS_ONLY
        }
    }
}