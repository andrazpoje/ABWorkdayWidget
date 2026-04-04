package com.dante.workcycle.ui.help

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class HelpSectionItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val contentRes: Int,
    var isExpanded: Boolean = false
)