package com.dante.workcycle.domain.model

data class StatusLabel(
    val name: String,
    val color: Int,
    val isSystem: Boolean = true,
    val isEnabled: Boolean = true,
    val iconKey: String? = null
)