package com.dante.workcycle.domain.premium

/**
 * SharedPreferences file names reserved for future entitlement-related storage.
 *
 * These names are kept central so backup/export filtering can exclude them explicitly.
 */
object PremiumEntitlementPrefsNames {
    const val DEBUG_OVERRIDE_PREFS_NAME = "premium_debug_override_prefs"
    const val TESTER_UNLOCK_PREFS_NAME = "premium_tester_unlock_prefs"
    const val ENTITLEMENT_CACHE_PREFS_NAME = "premium_entitlement_prefs"
}
