package com.dante.workcycle.domain.premium

/**
 * Debug-only repository adapter over [DebugPremiumEntitlementPrefs].
 *
 * This repository is a foundation only. It is not wired into app runtime yet, has no
 * Play Billing behavior, and must not be treated as production entitlement state.
 */
class DebugOverrideEntitlementRepository(
    private val prefs: DebugPremiumEntitlementPrefs
) : EntitlementRepository {

    override fun getState(feature: PremiumFeature): EntitlementState {
        return delegate().getState(feature)
    }

    override fun getSource(feature: PremiumFeature): EntitlementSource {
        return delegate().getSource(feature)
    }

    override fun isUnlocked(feature: PremiumFeature): Boolean {
        return delegate().isUnlocked(feature)
    }

    private fun delegate(): OverrideEntitlementRepository {
        return OverrideEntitlementRepository(prefs.getOverride())
    }
}
