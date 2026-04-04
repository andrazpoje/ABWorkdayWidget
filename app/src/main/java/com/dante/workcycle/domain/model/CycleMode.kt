package com.dante.workcycle.domain.model

enum class CycleMode {
    CYCLIC,
    MANUAL;

    companion object {
        fun fromString(value: String?): CycleMode {
            return try {
                valueOf(value ?: CYCLIC.name)
            } catch (_: Exception) {
                CYCLIC
            }
        }
    }
}