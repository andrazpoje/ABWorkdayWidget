package com.dante.workcycle.worklog.notification

data class WorkLogNotificationState(
    val status: Status,
    val title: String,
    val summaryText: String,
    val startTimeText: String? = null,
    val workedTodayText: String? = null,
    val breakText: String? = null
) {
    enum class Status {
        WORKING,
        ON_BREAK
    }
}