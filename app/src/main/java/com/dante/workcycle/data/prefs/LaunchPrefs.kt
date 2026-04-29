package com.dante.workcycle.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.domain.schedule.CycleManager

class LaunchPrefs(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LAST_SEEN_WHATS_NEW_VERSION = "last_seen_whats_new_version"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit {
            putBoolean(KEY_ONBOARDING_COMPLETED, completed)
        }
    }

    fun migrateExistingUsersIfNeeded() {
        if (prefs.contains(KEY_ONBOARDING_COMPLETED)) return

        if (hasExistingSetupSignals()) {
            setOnboardingCompleted(true)
        }
    }

    fun shouldShowWhatsNew(): Boolean {
        val lastSeenVersion = prefs.getString(KEY_LAST_SEEN_WHATS_NEW_VERSION, null)
        return lastSeenVersion != BuildConfig.VERSION_NAME
    }

    fun markWhatsNewSeen() {
        prefs.edit {
            putString(KEY_LAST_SEEN_WHATS_NEW_VERSION, BuildConfig.VERSION_NAME)
        }
    }

    fun shouldShowTemplatesHint(): Boolean {
        return prefs.getBoolean("templates_hint_shown", false).not()
    }

    fun markTemplatesHintShown() {
        prefs.edit {
            putBoolean("templates_hint_shown", true)
        }
    }

    fun clearOnboardingTestStateForDebug() {
        if (!BuildConfig.DEBUG) return

        prefs.edit {
            remove(KEY_ONBOARDING_COMPLETED)
            remove(KEY_LAST_SEEN_WHATS_NEW_VERSION)
            remove("templates_hint_shown")
        }

        appContext.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_FIRST_LAUNCH)
                remove(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION)
                remove(AppPrefs.KEY_START_YEAR)
                remove(AppPrefs.KEY_START_MONTH)
                remove(AppPrefs.KEY_START_DAY)
                remove(AppPrefs.KEY_FIRST_CYCLE_DAY)
                remove(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX)
            }

        appContext.getSharedPreferences(CycleManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(CycleManager.KEY_CYCLE_DAYS)
                remove(CycleManager.KEY_CYCLE_START_DATE)
            }

        TemplatePrefs.clear(appContext)
    }

    private fun hasExistingSetupSignals(): Boolean {
        val legacyPrefs = appContext.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val cyclePrefs = appContext.getSharedPreferences(CycleManager.PREFS_NAME, Context.MODE_PRIVATE)

        return (
            legacyPrefs.contains(KEY_FIRST_LAUNCH) &&
                legacyPrefs.getBoolean(KEY_FIRST_LAUNCH, true).not()
            ) ||
            legacyPrefs.contains(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION) ||
            legacyPrefs.contains(AppPrefs.KEY_START_YEAR) ||
            legacyPrefs.contains(AppPrefs.KEY_START_MONTH) ||
            legacyPrefs.contains(AppPrefs.KEY_START_DAY) ||
            legacyPrefs.contains(AppPrefs.KEY_FIRST_CYCLE_DAY) ||
            cyclePrefs.contains(CycleManager.KEY_CYCLE_DAYS) ||
            cyclePrefs.contains(CycleManager.KEY_CYCLE_START_DATE) ||
            TemplatePrefs.getActiveTemplateId(appContext) != null
    }
}
