package com.dante.workcycle.core.status

import com.dante.workcycle.domain.model.StatusLabel

object StatusSemantics {
    const val ICON_KEY_SICK = "sick"
    const val ICON_KEY_VACATION = "vacation"

    fun isExclusiveNonWorkingStatus(label: StatusLabel): Boolean {
        return label.iconKey == ICON_KEY_SICK || label.iconKey == ICON_KEY_VACATION
    }
}
