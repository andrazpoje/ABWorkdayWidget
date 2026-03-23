package com.dante.abworkdaywidget

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppThemeManager {
    private const val PREFS = "abprefs"
    private const val KEY_APP_THEME = "app_theme"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun applyFromPreferences(context: Context) {
        apply(loadTheme(context))
    }

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_THEME, theme)
            .apply()
    }

    fun loadTheme(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_APP_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun apply(theme: String) {
        val mode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
