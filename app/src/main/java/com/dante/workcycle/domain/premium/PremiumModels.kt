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

enum class EntitlementOverrideMode {
    NONE,
    UNLOCK_ALL_PREMIUM,
    LOCK_ALL_PREMIUM
}

/**
 * In-memory override definition for future debug / tester / founder entitlement flows.
 *
 * This is only a pure Kotlin foundation. It does not use Play Billing, UI wiring, or
 * production entitlement cache. When persistence is added later, that storage must stay
 * out of backup/export payloads.
 *
 * If [features] is empty, the override applies to all premium/subscription features.
 * When [features] is not empty, the override applies only to those listed entries.
 */
data class EntitlementOverride(
    val mode: EntitlementOverrideMode,
    val source: EntitlementSource,
    val features: Set<PremiumFeature> = emptySet()
)

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

/**
 * Pure Kotlin override repository for future debug/test/founder entitlement flows.
 *
 * This repository is intentionally storage-free and has no Play Billing dependency.
 * UI gating is not active yet, and this class is only a building block for the future
 * entitlement chain. When a persisted version is introduced later, it must not be
 * included in backup ZIP payloads.
 */
class OverrideEntitlementRepository(
    private val override: EntitlementOverride
) : EntitlementRepository {

    override fun getState(feature: PremiumFeature): EntitlementState {
        if (!appliesTo(feature)) return EntitlementState.UNKNOWN

        return when (override.mode) {
            EntitlementOverrideMode.NONE -> EntitlementState.UNKNOWN
            EntitlementOverrideMode.UNLOCK_ALL_PREMIUM -> EntitlementState.UNLOCKED
            EntitlementOverrideMode.LOCK_ALL_PREMIUM -> EntitlementState.LOCKED
        }
    }

    override fun getSource(feature: PremiumFeature): EntitlementSource {
        if (!appliesTo(feature)) return EntitlementSource.NONE

        return when (override.mode) {
            EntitlementOverrideMode.NONE -> EntitlementSource.NONE
            else -> override.source
        }
    }

    override fun isUnlocked(feature: PremiumFeature): Boolean {
        return getState(feature) == EntitlementState.UNLOCKED
    }

    private fun appliesTo(feature: PremiumFeature): Boolean {
        if (override.mode == EntitlementOverrideMode.NONE) return false
        return override.features.isEmpty() || feature in override.features
    }
}

/**
 * Priority-based entitlement chain for future debug/tester/billing composition.
 *
 * Repositories are evaluated in order. The first repository that returns either
 * `UNLOCKED` or `LOCKED` together with a non-`NONE` source wins. Repositories that
 * return `UNKNOWN` or `NONE` do not stop evaluation.
 *
 * If nothing decisive is found, the chain falls back to [FreeOnlyEntitlementRepository].
 */
class CompositeEntitlementRepository(
    private val repositories: List<EntitlementRepository>,
    private val fallback: EntitlementRepository = FreeOnlyEntitlementRepository()
) : EntitlementRepository {

    override fun getState(feature: PremiumFeature): EntitlementState {
        return resolve(feature).first
    }

    override fun getSource(feature: PremiumFeature): EntitlementSource {
        return resolve(feature).second
    }

    override fun isUnlocked(feature: PremiumFeature): Boolean {
        return resolve(feature).first == EntitlementState.UNLOCKED
    }

    private fun resolve(feature: PremiumFeature): Pair<EntitlementState, EntitlementSource> {
        repositories.forEach { repository ->
            val state = repository.getState(feature)
            val source = repository.getSource(feature)

            if (source == EntitlementSource.NONE) {
                return@forEach
            }

            if (state == EntitlementState.UNLOCKED || state == EntitlementState.LOCKED) {
                return state to source
            }
        }

        return fallback.getState(feature) to fallback.getSource(feature)
    }
}
