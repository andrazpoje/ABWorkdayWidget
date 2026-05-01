package com.dante.workcycle.domain.premium

/**
 * Central entry point for future premium / pro gating decisions.
 *
 * This foundation is intentionally read-only from the application's perspective:
 * - no Play Billing dependency yet
 * - no UI wiring yet
 * - no locking of existing backup/export functionality yet
 *
 * The goal is to keep monetization logic out of fragments and feature flows once
 * premium gating is introduced later.
 */
class FeatureGate(
    private val entitlementRepository: EntitlementRepository
) {
    fun canUse(feature: PremiumFeature): GateDecision {
        val tier = tierOf(feature)
        if (tier == FeatureTier.FREE) {
            return GateDecision(
                allowed = true,
                feature = feature,
                tier = tier,
                state = EntitlementState.UNLOCKED,
                source = EntitlementSource.NONE,
                reason = null
            )
        }

        val state = entitlementRepository.getState(feature)
        val source = entitlementRepository.getSource(feature)
        val allowed = entitlementRepository.isUnlocked(feature) &&
            state == EntitlementState.UNLOCKED

        return GateDecision(
            allowed = allowed,
            feature = feature,
            tier = tier,
            state = state,
            source = source,
            reason = when {
                allowed -> null
                state == EntitlementState.UNKNOWN -> "Entitlement state is unknown."
                tier == FeatureTier.PREMIUM_ONE_TIME -> "Feature requires Premium one-time."
                else -> "Feature requires subscription."
            }
        )
    }

    fun tierOf(feature: PremiumFeature): FeatureTier {
        return when (feature) {
            PremiumFeature.LOCAL_FULL_BACKUP_EXPORT,
            PremiumFeature.DATE_RANGE_CSV_EXPORT,
            PremiumFeature.PDF_EXPORT,
            PremiumFeature.MULTIPLE_ACTIVE_WORK_PROFILES,
            PremiumFeature.ADVANCED_STATISTICS,
            PremiumFeature.CUSTOM_TEMPLATES,
            PremiumFeature.ADVANCED_WIDGET_CUSTOMIZATION,
            PremiumFeature.ADVANCED_WORK_LOG_RULES,
            PremiumFeature.ADVANCED_REPORTS -> FeatureTier.PREMIUM_ONE_TIME

            PremiumFeature.CLOUD_SYNC,
            PremiumFeature.CLOUD_BACKUP,
            PremiumFeature.GPS_GEOFENCING_SUGGESTIONS,
            PremiumFeature.LIVE_WEB_SHARING -> FeatureTier.SUBSCRIPTION
        }
    }
}
