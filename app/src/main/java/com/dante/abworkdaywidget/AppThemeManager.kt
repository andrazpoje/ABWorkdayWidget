package com.dante.abworkdaywidget

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object AppThemeManager {

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun applyFromPreferences(context: Context) {
        apply(loadTheme(context))
    }

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE).edit {
            putString(AppPrefs.KEY_APP_THEME, theme)
        }
    }

    fun loadTheme(context: Context): String {
        return context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getString(AppPrefs.KEY_APP_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
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
