package com.dante.workcycle.domain.model

data class CycleResult(
    val layer: CycleLayer,
    val label: String,
    val isOverride: Boolean = false
)