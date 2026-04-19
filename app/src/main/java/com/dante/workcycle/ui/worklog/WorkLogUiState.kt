package com.dante.workcycle.ui.worklog

data class WorkLogUiState(
    val dateText: String = "",
    val startTimeText: String = "",
    val endTimeText: String = "",
    val breakMinutesText: String = "0",
    val noteText: String = "",
    val totalText: String = "—",
    val isExisting: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = false
)