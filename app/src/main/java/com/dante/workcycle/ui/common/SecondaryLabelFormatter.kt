package com.dante.workcycle.ui.common

import java.util.Locale

object SecondaryLabelFormatter {

    fun shortLabel(label: String?): String? {
        if (label.isNullOrBlank()) return null

        return when (label.trim().lowercase(Locale.getDefault())) {
            "bolniška" -> "Bol."
            "dopust" -> "Dop."
            "dežurstvo" -> "Dež."
            "teren" -> "Ter."
            else -> label.trim()
        }
    }

    fun normalized(label: String?): String? {
        val value = label?.trim()
        return if (value.isNullOrEmpty()) null else value
    }
}