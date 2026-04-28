package com.dante.workcycle.core.status

import android.content.Context
import android.graphics.Color
import com.dante.workcycle.R
import com.dante.workcycle.domain.model.StatusLabel

/**
 * Presentation mapping for status labels.
 *
 * This object owns localized display names, icon resources, default semantic colors,
 * and visual ordering for built-in statuses. It must remain UI-only: business rules
 * such as non-working or exclusive status handling belong in StatusSemantics. Status
 * colors are intentionally separate from primary/secondary cycle colors so day status
 * badges do not inherit cycle palette behavior.
 */
object StatusVisuals {

    private val statusPriority = mapOf(
        "sick" to 0,
        "vacation" to 1,
        "standby" to 2,
        "reduction" to 3,
        "replacement" to 4,
        "meeting" to 5,
        "terrain" to 6
    )

    /**
     * Resolves the UI display name for a status label.
     *
     * System labels use stable icon keys and localized string resources. Custom labels
     * return the exact user-entered name so app language changes do not rewrite user
     * data.
     */
    fun getDisplayName(context: Context, label: StatusLabel): String {
        if (!label.isSystem) return label.name

        val resId = when (label.iconKey) {
            "sick" -> R.string.assignment_system_label_sick
            "vacation" -> R.string.assignment_system_label_vacation
            "standby" -> R.string.assignment_system_label_standby
            "reduction" -> R.string.status_system_label_reduction
            "replacement" -> R.string.status_system_label_replacement
            "meeting" -> R.string.status_system_label_meeting
            "terrain" -> R.string.status_system_label_terrain
            else -> null
        }

        return resId?.let(context::getString) ?: label.name
    }

    /**
     * Maps a stable status icon key to a drawable resource for chips, lists, and cells.
     */
    fun getIconRes(iconKey: String?): Int? {
        return when (iconKey) {
            "sick" -> R.drawable.ic_assignment_sick_24
            "vacation" -> R.drawable.ic_vacation_24
            "standby" -> R.drawable.ic_assignment_standby_24
            "reduction" -> R.drawable.ic_shrink_24
            "replacement" -> R.drawable.ic_replacement_24
            "meeting" -> R.drawable.ic_meeting_24
            "terrain" -> R.drawable.ic_terrain_24
            "field" -> R.drawable.ic_assignment_field_24
            else -> null
        }
    }

    /**
     * Returns the built-in default status color for a semantic icon key.
     *
     * These values are status presentation defaults only. Primary cycle colors and
     * widget style colors are resolved elsewhere and should not be mixed with this
     * status palette.
     */
    fun getDefaultColor(iconKey: String?): Int? {
        return when (iconKey) {
            "sick" -> Color.parseColor("#E53935")
            "vacation" -> Color.parseColor("#FFD54F")
            "standby" -> Color.parseColor("#7E57C2")
            "terrain" -> Color.parseColor("#00ACC1")
            "meeting" -> Color.parseColor("#3949AB")
            "replacement" -> Color.parseColor("#8D6E63")
            "reduction" -> Color.parseColor("#607D8B")
            else -> null
        }
    }

    /**
     * UI helper for showing explanatory text on exclusive built-in status chips.
     *
     * Do not use this for Work Log or schedule business decisions; use
     * StatusSemantics for semantic checks.
     */
    fun usesExclusiveHelper(label: StatusLabel): Boolean {
        return label.iconKey == "sick" || label.iconKey == "vacation"
    }

    /**
     * Orders built-in statuses for compact calendar/month and weekly preview displays.
     *
     * Priority is visual only. It should not decide which statuses are allowed,
     * exclusive, removable, or non-working.
     */
    fun sortByPriority(labels: List<StatusLabel>): List<StatusLabel> {
        return labels.sortedWith(
            compareBy<StatusLabel> { statusPriority[it.iconKey] ?: Int.MAX_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }
}
