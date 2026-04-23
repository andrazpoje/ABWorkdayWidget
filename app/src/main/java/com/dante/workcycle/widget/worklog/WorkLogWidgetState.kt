package com.dante.workcycle.widget.worklog

data class WorkLogWidgetState(
    val title: String,
    val statusText: String,
    val primaryValueText: String,
    val secondaryValueText: String? = null,
    val tertiaryValueText: String? = null,
    val requiresMinuteRefresh: Boolean = false
)
