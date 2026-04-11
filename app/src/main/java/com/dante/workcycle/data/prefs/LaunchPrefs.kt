package com.dante.workcycle.data.prefs

import android.content.Context
import com.dante.workcycle.BuildConfig

class LaunchPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LAST_SEEN_WHATS_NEW_VERSION = "last_seen_whats_new_version"
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun shouldShowWhatsNew(): Boolean {
        val lastSeenVersion = prefs.getString(KEY_LAST_SEEN_WHATS_NEW_VERSION, null)
        return lastSeenVersion != BuildConfig.VERSION_NAME
    }

    fun markWhatsNewSeen() {
        prefs.edit()
            .putString(KEY_LAST_SEEN_WHATS_NEW_VERSION, BuildConfig.VERSION_NAME)
            .apply()
    }

    fun shouldShowTemplatesHint(): Boolean {
        return prefs.getBoolean("templates_hint_shown", false).not()
    }

    fun markTemplatesHintShown() {
        prefs.edit().putBoolean("templates_hint_shown", true).apply()
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean("first_launch", true)
    }

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean("first_launch", false).apply()
    }
}