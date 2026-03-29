package com.dante.workcycle

import android.content.Context
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt

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
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val overrideEnabled = prefs.getBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
        if (!overrideEnabled) return false

        val skippedOverrideLabel = prefs.getString(AppPrefs.KEY_SKIPPED_LABEL, AppPrefs.DEFAULT_SKIPPED_LABEL)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: AppPrefs.DEFAULT_SKIPPED_LABEL

        return label.trim().equals(skippedOverrideLabel, ignoreCase = true)
    }

    private fun getSkippedDayColor(context: Context): Int {
        return when (CycleThemeManager.loadTheme(context)) {
            CycleThemeManager.THEME_PASTEL -> "#90A4AE".toColorInt()
            CycleThemeManager.THEME_DARK -> "#455A64".toColorInt()
            else -> "#78909C".toColorInt()
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
        return palette[index % palette.size].toColorInt()
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
        return palette[index % palette.size].toColorInt()
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
        return palette[index % palette.size].toColorInt()
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