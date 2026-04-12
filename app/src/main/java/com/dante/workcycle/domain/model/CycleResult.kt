package com.dante.workcycle.domain.model

data class CycleResult(
    val layer: CycleLayer,
    val label: String,
    val source: String,
    val isOverride: Boolean = false
)