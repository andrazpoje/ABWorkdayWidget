package com.dante.workcycle.core.status

import com.dante.workcycle.domain.model.StatusLabel

/**
 * Business semantics for built-in status labels.
 *
 * This object describes what a status means, independent of icons, colors, localized
 * names, or chip rendering. Logic that decides whether a status is non-working,
 * exclusive, or otherwise special should use stable semantic keys here instead of UI
 * display text. Custom labels keep their user-entered names and should not be inferred
 * as built-in statuses unless they carry a known system key.
 */
object StatusSemantics {
    const val ICON_KEY_SICK = "sick"
    const val ICON_KEY_VACATION = "vacation"

    /**
     * Returns true for statuses that represent an exclusive non-working day.
     *
     * Work Log start warnings and conflict rules should depend on this semantic check,
     * not on StatusVisuals or localized labels such as "Dopust" / "Vacation".
     */
    fun isExclusiveNonWorkingStatus(label: StatusLabel): Boolean {
        return label.iconKey == ICON_KEY_SICK || label.iconKey == ICON_KEY_VACATION
    }
}
