package com.dante.workcycle.core.theme

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.data.prefs.AppPrefs

object CycleThemeManager {

    const val THEME_CLASSIC = "classic"
    const val THEME_PASTEL = "pastel"
    const val THEME_DARK = "dark"

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit {
                putString(AppPrefs.KEY_CYCLE_THEME, theme)
            }
    }

    fun loadTheme(context: Context): String {
        return context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getString(AppPrefs.KEY_CYCLE_THEME, THEME_CLASSIC) ?: THEME_CLASSIC
    }

}