package com.dante.workcycle.domain.premium

import android.content.Context
import android.content.SharedPreferences
import com.dante.workcycle.BuildConfig

/**
 * Debug-only local storage wrapper for future premium gating tests.
 *
 * This is not production entitlement storage, does not replace Play Billing, and must
 * never be included in backup/export payloads. Its only purpose is to support future
 * debug/testing flows for premium UI gating.
 */
class DebugPremiumEntitlementPrefs(
    private val storage: PremiumOverrideStorage,
    private val isDebugBuild: Boolean
) {
    fun getOverride(): EntitlementOverride {
        if (!isDebugBuild) {
            return EntitlementOverride(
                mode = EntitlementOverrideMode.NONE,
                source = EntitlementSource.NONE
            )
        }

        val mode = storage.getString(KEY_OVERRIDE_MODE)
            ?.let { raw -> runCatching { EntitlementOverrideMode.valueOf(raw) }.getOrNull() }
            ?: EntitlementOverrideMode.NONE

        return EntitlementOverride(
            mode = mode,
            source = if (mode == EntitlementOverrideMode.NONE) {
                EntitlementSource.NONE
            } else {
                EntitlementSource.DEBUG_OVERRIDE
            }
        )
    }

    fun setOverrideMode(mode: EntitlementOverrideMode) {
        if (!isDebugBuild) return

        storage.putString(KEY_OVERRIDE_MODE, mode.name)
    }

    fun clear() {
        if (!isDebugBuild) return

        storage.clear()
    }

    companion object {
        const val PREFS_NAME = PremiumEntitlementPrefsNames.DEBUG_OVERRIDE_PREFS_NAME
        private const val KEY_OVERRIDE_MODE = "override_mode"

        fun create(
            context: Context,
            isDebugBuild: Boolean = BuildConfig.DEBUG
        ): DebugPremiumEntitlementPrefs {
            val prefs = context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            return DebugPremiumEntitlementPrefs(
                storage = SharedPreferencesPremiumOverrideStorage(prefs),
                isDebugBuild = isDebugBuild
            )
        }
    }
}

/**
 * Abstraction that keeps debug override storage testable without Robolectric.
 */
interface PremiumOverrideStorage {
    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun clear()
}

private class SharedPreferencesPremiumOverrideStorage(
    private val prefs: SharedPreferences
) : PremiumOverrideStorage {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
