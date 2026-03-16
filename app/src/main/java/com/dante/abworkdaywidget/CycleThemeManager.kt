package com.dante.abworkdaywidget

import android.content.Context
import androidx.core.content.ContextCompat

object CycleThemeManager {

    private const val PREFS = "abprefs"
    private const val KEY_THEME = "cycle_theme"

    const val THEME_CLASSIC = "classic"
    const val THEME_PASTEL = "pastel"
    const val THEME_DARK = "dark"

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
    }

    fun loadTheme(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_CLASSIC) ?: THEME_CLASSIC
    }

    fun getColorForIndex(context: Context, index: Int): Int {

        val theme = loadTheme(context)

        return when (theme) {
            THEME_PASTEL -> pastelColors(index)
            THEME_DARK -> darkColors(index)
            else -> classicColors(context, index)
        }
    }

    private fun classicColors(context: Context, index: Int): Int {

        val colors = listOf(
            ContextCompat.getColor(context, R.color.shiftA),
            ContextCompat.getColor(context, R.color.shiftB),
            0xFFFF9800.toInt(),
            0xFF9C27B0.toInt(),
            0xFFD32F2F.toInt(),
            0xFF00897B.toInt()
        )

        return colors[index % colors.size]
    }

    private fun pastelColors(index: Int): Int {

        val colors = listOf(
            0xFF90CAF9.toInt(),
            0xFFA5D6A7.toInt(),
            0xFFFFCC80.toInt(),
            0xFFCE93D8.toInt(),
            0xFFEF9A9A.toInt(),
            0xFF80CBC4.toInt()
        )

        return colors[index % colors.size]
    }

    private fun darkColors(index: Int): Int {

        val colors = listOf(
            0xFF0D47A1.toInt(),
            0xFF1B5E20.toInt(),
            0xFFBF360C.toInt(),
            0xFF4A148C.toInt(),
            0xFFB71C1C.toInt(),
            0xFF004D40.toInt()
        )

        return colors[index % colors.size]
    }
}