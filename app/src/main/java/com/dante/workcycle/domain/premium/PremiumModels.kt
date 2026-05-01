package com.dante.workcycle.domain.premium

/**
 * Defines the long-term monetization tiers used by the app.
 *
 * This foundation does not lock existing UI or runtime flows. It only provides a
 * central model for future gating once Play Billing and premium UX are added.
 */
enum class FeatureTier {
    FREE,
    PREMIUM_ONE_TIME,
    SUBSCRIPTION
}

/**
 * Enumerates premium candidates that may be gated later.
 *
 * Free core should remain generous. Split shifts / multiple sessions are intentionally
 * excluded because they are planned as free core functionality, not premium.
 */
enum class PremiumFeature {
    LOCAL_FULL_BACKUP_EXPORT,
    DATE_RANGE_CSV_EXPORT,
    PDF_EXPORT,
    MULTIPLE_ACTIVE_WORK_PROFILES,
    ADVANCED_STATISTICS,
    CUSTOM_TEMPLATES,
    ADVANCED_WIDGET_CUSTOMIZATION,
    ADVANCED_WORK_LOG_RULES,
    ADVANCED_REPORTS,
    CLOUD_SYNC,
    CLOUD_BACKUP,
    GPS_GEOFENCING_SUGGESTIONS,
    LIVE_WEB_SHARING
}

enum class EntitlementState {
    LOCKED,
    UNLOCKED,
    UNKNOWN
}

enum class EntitlementSource {
    NONE,
    DEBUG_OVERRIDE,
    TESTER_OVERRIDE,
    LOCAL_CACHE,
    PLAY_PURCHASE,
    PLAY_SUBSCRIPTION
}

data class GateDecision(
    val allowed: Boolean,
    val feature: PremiumFeature,
    val tier: FeatureTier,
    val state: EntitlementState,
    val source: EntitlementSource,
    val reason: String?
)

/**
 * Central abstraction for future entitlement lookup.
 *
 * Play Billing, tester unlocks, debug overrides, and cached purchases will be wired in
 * later. This interface intentionally has no Android dependency and no persistence
 * behavior in the initial foundation.
 */
interface EntitlementRepository {
    fun getState(feature: PremiumFeature): EntitlementState

    fun getSource(feature: PremiumFeature): EntitlementSource

    fun isUnlocked(feature: PremiumFeature): Boolean
}

/**
 * Default repository used until real entitlement sources exist.
 *
 * It does not use SharedPreferences, debug unlocks, tester unlocks, or Play Billing.
 * All premium and subscription features stay locked here, but this class is not
 * connected to existing UI yet, so current runtime behavior remains unchanged.
 */
class FreeOnlyEntitlementRepository : EntitlementRepository {
    override fun getState(feature: PremiumFeature): EntitlementState = EntitlementState.LOCKED

    override fun getSource(feature: PremiumFeature): EntitlementSource = EntitlementSource.NONE

    override fun isUnlocked(feature: PremiumFeature): Boolean = false
}
