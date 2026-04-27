package com.dante.workcycle.core.util

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.style.WidgetStyleManager

object CycleColorHelper {

    fun getBackgroundColor(
        context: Context,
        label: String,
        cycle: List<String>
    ): Int {
        if (isOffDayLabel(context, label)) {
            return getSkippedDayColor(context)
        }

        if (isSkippedOverrideActiveForLabel(context, label)) {
            return getSkippedDayColor(context)
        }

        val cleanedCycle = cycle
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cleanedCycle.isEmpty()) {
            return getSkippedDayColor(context)
        }

        val normalizedLabel = label.trim()
        val index = cleanedCycle.indexOfFirst { it.equals(normalizedLabel, ignoreCase = true) }

// fallback na prvi element cikla namesto sive
        val safeIndex = if (index == -1) 0 else index

        val colors = WidgetStyleManager.getColors(context)
        return when (safeIndex) {
            0 -> colors.shiftAColor
            1 -> colors.shiftBColor
            2 -> colors.shiftCColor
            // TODO: Future: dynamic primary cycle color palette for 4+ primary cycle labels.
            else -> colors.shiftCColor
        }
    }

    private fun isOffDayLabel(context: Context, label: String): Boolean {
        val normalized = label.trim()
        if (normalized.isBlank()) return false

        return normalized.equals(context.getString(R.string.off_day_label), ignoreCase = true) ||
            normalized.equals("Prosto", ignoreCase = true) ||
            normalized.equals("Off", ignoreCase = true)
    }

    private fun isSkippedOverrideActiveForLabel(context: Context, label: String): Boolean {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val skippedOverrideLabel = prefs.getString(AppPrefs.KEY_SKIPPED_LABEL, AppPrefs.DEFAULT_SKIPPED_LABEL)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: AppPrefs.DEFAULT_SKIPPED_LABEL

        return label.trim().equals(skippedOverrideLabel, ignoreCase = true)
    }

    private fun getSkippedDayColor(context: Context): Int {
        return WidgetStyleManager.getColors(context).offDayColor
    }

    fun getTextColorForBackground(backgroundColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        return if (luminance > 0.5) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    fun getSoftStroke(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.18f)
    }

}
