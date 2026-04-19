package com.dante.workcycle.ui.worklog

data class WorkLogDashboardUiState(
    val todayText: String = "",
    val stateText: String = "",
    val stateDetailText: String = "",
    val startedAtText: String = "—",
    val workedTodayText: String = "—",
    val targetWorkText: String = "8h 00m",
    val balanceText: String = "—",
    val primaryButtonText: String = "",
    val breakActionText: String = "",
    val canBreak: Boolean = false,
    val breakButtonEnabled: Boolean = false,
    val isOnBreak: Boolean = false,
    val breakStartedAtText: String = "—",
    val breakDurationText: String = "—",
    val mealActionText: String = "",
    val mealButtonEnabled: Boolean = false,
    val recentEvents: List<String> = emptyList(),
    val message: String? = null
)