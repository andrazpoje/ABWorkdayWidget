package com.dante.workcycle

import java.time.LocalDate

data class CalendarDayItem(
    val date: LocalDate?,
    val dayNumber: String = "",

    val effectiveCycleLabel: String = "",
    val assignmentLabel: String? = null,

    val cycleColor: Int? = null,
    val assignmentColor: Int? = null,

    val isOffDay: Boolean = false,
    val isToday: Boolean = false,
    val isCurrentMonth: Boolean = true,
    val isEmpty: Boolean = false,
    val isSelected: Boolean = false
) {
    val primaryLabel: String
        get() = effectiveCycleLabel

    val secondaryLabel: String?
        get() = assignmentLabel

    val primaryColor: Int?
        get() = cycleColor
}