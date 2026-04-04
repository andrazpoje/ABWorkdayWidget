package com.dante.workcycle.domain.model

data class AssignmentLabel(
    val name: String,
    val color: Int,
    val isSystem: Boolean = false,
    val isEnabled: Boolean = true,
    val iconKey: String? = null,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null
)