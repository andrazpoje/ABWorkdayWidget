package com.dante.workcycle.domain.model

enum class ScheduleMode {
    CYCLIC,
    MANUAL,
    COMBINED;

    companion object {
        fun fromString(value: String?): ScheduleMode {
            return try {
                valueOf(value ?: CYCLIC.name)
            } catch (e: Exception) {
                CYCLIC
            }
        }
    }
}