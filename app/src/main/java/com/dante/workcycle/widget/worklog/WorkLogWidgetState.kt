package com.dante.workcycle.widget.worklog

/**
 * Display-only state consumed by [WorkLogWidgetRenderer].
 *
 * The state factory decides whether active-session minute refresh is needed;
 * the renderer should not infer that from text values or widget layout.
 */
data class WorkLogWidgetState(
    val title: String,
    val statusText: String,
    val primaryValueText: String,
    val secondaryValueText: String? = null,
    val tertiaryValueText: String? = null,
    val requiresMinuteRefresh: Boolean = false
)
