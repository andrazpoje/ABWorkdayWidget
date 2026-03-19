package com.dante.abworkdaywidget

import android.content.Context
import androidx.core.graphics.ColorUtils

object CycleColorHelper {

    fun getBackgroundColor(
        context: Context,
        label: String,
        cycle: List<String>
    ): Int {
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

        if (index == -1) {
            return getSkippedDayColor(context)
        }

        return when (CycleThemeManager.loadTheme(context)) {
            CycleThemeManager.THEME_PASTEL -> getPastelColor(index)
            CycleThemeManager.THEME_DARK -> getDarkColor(index)
            else -> getClassicColor(index)
        }
    }

    private fun isSkippedOverrideActiveForLabel(context: Context, label: String): Boolean {
        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)
        val overrideEnabled = prefs.getBoolean("overrideSkippedDays", true)
        if (!overrideEnabled) return false

        val skippedOverrideLabel = prefs.getString("skippedDayLabel", "Prosto")
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: "Prosto"

        return label.trim().equals(skippedOverrideLabel, ignoreCase = true)
    }

    private fun getSkippedDayColor(context: Context): Int {
        return when (CycleThemeManager.loadTheme(context)) {
            CycleThemeManager.THEME_PASTEL -> android.graphics.Color.parseColor("#90A4AE")
            CycleThemeManager.THEME_DARK -> android.graphics.Color.parseColor("#455A64")
            else -> android.graphics.Color.parseColor("#78909C")
        }
    }

    private fun getClassicColor(index: Int): Int {
        val palette = listOf(
            "#1976D2",
            "#388E3C",
            "#F57C00",
            "#7B1FA2",
            "#D32F2F",
            "#00838F",
            "#5D4037",
            "#455A64"
        )
        return android.graphics.Color.parseColor(palette[index % palette.size])
    }

    private fun getPastelColor(index: Int): Int {
        val palette = listOf(
            "#64B5F6",
            "#81C784",
            "#FFB74D",
            "#BA68C8",
            "#E57373",
            "#4DD0E1",
            "#A1887F",
            "#90A4AE"
        )
        return android.graphics.Color.parseColor(palette[index % palette.size])
    }

    private fun getDarkColor(index: Int): Int {
        val palette = listOf(
            "#0D47A1",
            "#1B5E20",
            "#E65100",
            "#4A148C",
            "#B71C1C",
            "#006064",
            "#3E2723",
            "#263238"
        )
        return android.graphics.Color.parseColor(palette[index % palette.size])
    }

    fun getTextColorForBackground(backgroundColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        return if (luminance > 0.5) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
    }
}