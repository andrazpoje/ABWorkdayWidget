package com.dante.abworkdaywidget

import android.content.Context
import androidx.core.content.ContextCompat

object CycleColorHelper {

    private const val OFF_LABEL = "Prosto"

    fun getBackgroundColor(
        context: Context,
        label: String,
        cycle: List<String>
    ): Int {
        val normalizedLabel = label.trim()

        if (
            normalizedLabel.equals("X", ignoreCase = true) ||
            normalizedLabel.equals(OFF_LABEL, ignoreCase = true)
        ) {
            return ContextCompat.getColor(context, R.color.shiftOff)
        }

        val index = cycle.indexOf(normalizedLabel).coerceAtLeast(0)

        return CycleThemeManager.getColorForIndex(context, index)
    }
}