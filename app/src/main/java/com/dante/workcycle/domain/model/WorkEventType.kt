package com.dante.workcycle.domain.model

/**
 * Ordered event types that describe the current Work Log timeline.
 *
 * Breaks, meals, and notes are stored as explicit user events only. There is no
 * timer, reminder, or automatic break/meal ending behavior in the current
 * implementation.
 */
enum class WorkEventType {
    CLOCK_IN,
    BREAK_START,
    BREAK_END,
    CLOCK_OUT,
    MEAL,
    NOTE
}
