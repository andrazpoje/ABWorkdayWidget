package com.dante.workcycle

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dante.workcycle.data.prefs.AppPrefs

object AppLanguageManager {

    val supportedLanguageTags = listOf(
        "system",
        "sl",
        "en"
    )

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        return prefs.getString(
            AppPrefs.KEY_APP_LANGUAGE,
            AppPrefs.APP_LANGUAGE_SYSTEM
        ) ?: AppPrefs.APP_LANGUAGE_SYSTEM
    }

    fun applySavedLanguage(context: Context) {
        applyLanguage(getSavedLanguage(context))
    }

    fun applyLanguage(languageTag: String) {
        if (languageTag == AppPrefs.APP_LANGUAGE_SYSTEM) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.getEmptyLocaleList()
            )
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag)
            )
        }
    }

}
