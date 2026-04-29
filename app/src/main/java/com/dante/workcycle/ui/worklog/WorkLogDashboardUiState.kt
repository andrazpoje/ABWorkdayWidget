package com.dante.workcycle.ui.worklog

import androidx.annotation.DrawableRes
import com.dante.workcycle.R
import com.dante.workcycle.domain.model.WorkEvent

enum class WorkLogSliderAction {
    START_WORK,
    FINISH_WORK,
    END_BREAK
}

enum class WorkLogDeviationTone {
    DEFAULT,
    ACCENT,
    ERROR
}

enum class WorkLogDashboardVisualState {
    NOT_STARTED,
    WORKING,
    BREAK,
    FINISHED
}

const val WORK_LOG_PLACEHOLDER = "-"

data class WorkEventListItem(
    val event: WorkEvent,
    val timeText: String,
    val titleText: String,
    val editBadgeText: String? = null,
    val detailText: String? = null,
    @DrawableRes val iconRes: Int
)

data class WorkLogStartWarning(
    val reasonText: String,
    val removableStatusLabels: List<String> = emptyList()
)

data class WorkLogDashboardUiState(
    val todayText: String = "",
    val stateText: String = "",
    val stateDetailText: String = "",
    val visualState: WorkLogDashboardVisualState = WorkLogDashboardVisualState.NOT_STARTED,
    val sliderAction: WorkLogSliderAction = WorkLogSliderAction.START_WORK,
    val sliderActionText: String = "",
    @DrawableRes val sliderIconRes: Int = R.drawable.ic_work_time_24,
    val showPrimaryAction: Boolean = true,
    val sliderEnabled: Boolean = true,
    val startWarning: WorkLogStartWarning? = null,
    val showSecondaryActions: Boolean = false,
    val showBreakActionButton: Boolean = false,
    val showExpectedStart: Boolean = false,
    val expectedStartText: String = WORK_LOG_PLACEHOLDER,
    val showExpectedEnd: Boolean = false,
    val expectedEndText: String = WORK_LOG_PLACEHOLDER,
    val showStartedAt: Boolean = false,
    val startedAtText: String = WORK_LOG_PLACEHOLDER,
    val showStartDeviation: Boolean = false,
    val startDeviationText: String = "",
    val startDeviationTone: WorkLogDeviationTone = WorkLogDeviationTone.DEFAULT,
    val showWorkedToday: Boolean = false,
    val workedTodayText: String = WORK_LOG_PLACEHOLDER,
    val showTarget: Boolean = false,
    val showBalance: Boolean = false,
    val targetWorkText: String = "8h 00m",
    val balanceText: String = WORK_LOG_PLACEHOLDER,
    val showCreditedTime: Boolean = false,
    val creditedTimeText: String = WORK_LOG_PLACEHOLDER,
    val breakActionText: String = "",
    val breakButtonEnabled: Boolean = false,
    val isOnBreak: Boolean = false,
    val showBreakInfo: Boolean = false,
    val breakStartedAtText: String = WORK_LOG_PLACEHOLDER,
    val breakDurationLabelText: String = "",
    val breakDurationText: String = WORK_LOG_PLACEHOLDER,
    val showEndDeviation: Boolean = false,
    val endDeviationText: String = "",
    val endDeviationTone: WorkLogDeviationTone = WorkLogDeviationTone.DEFAULT,
    val mealActionText: String = "",
    val mealButtonEnabled: Boolean = false,
    val recentEvents: List<WorkEventListItem> = emptyList(),
    val message: String? = null
)
