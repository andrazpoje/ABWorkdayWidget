package com.dante.workcycle.domain.premium

import android.content.Context
import com.dante.workcycle.BuildConfig

/**
 * Central runtime assembly point for the app's premium gating chain.
 *
 * This provider does not lock features by itself. It only defines how a runtime
 * [FeatureGate] should be built so future UI gating does not assemble entitlement
 * chains ad hoc inside fragments.
 *
 * Current scope:
 * - debug override repository
 * - free-only fallback
 *
 * Not included yet:
 * - tester/founder repository
 * - Play Billing / cached entitlement repository
 * - active UI gating
 *
 * Deferred after the v3.0 release pass:
 * - add new entitlement sources only through a separate audit of chain order,
 *   release safety, and backup exclusion rules
 * - do not enable Play Billing or real UI gating from this file piecemeal
 */
object PremiumProvider {

    @Volatile
    private var cachedFeatureGate: FeatureGate? = null

    fun featureGate(context: Context): FeatureGate {
        return cachedFeatureGate ?: synchronized(this) {
            cachedFeatureGate ?: createFeatureGate(context.applicationContext).also {
                cachedFeatureGate = it
            }
        }
    }

    internal fun createFeatureGate(
        context: Context,
        isDebugBuild: Boolean = BuildConfig.DEBUG
    ): FeatureGate {
        val debugPrefs = DebugPremiumEntitlementPrefs.create(
            context = context,
            isDebugBuild = isDebugBuild
        )

        val repositories = createRepositoryChain(
            debugOverrideRepository = DebugOverrideEntitlementRepository(debugPrefs)
        )

        return FeatureGate(repositories)
    }

    internal fun createRepositoryChain(
        debugOverrideRepository: EntitlementRepository,
        fallbackRepository: EntitlementRepository = FreeOnlyEntitlementRepository()
    ): EntitlementRepository {
        return CompositeEntitlementRepository(
            repositories = listOf(
                debugOverrideRepository
                // TODO: Deferred after the v3.0 release pass. Insert the
                // tester/founder repository here only after a separate audit of
                // priority, release safety, and backup exclusion behavior.
                // TODO: Deferred after the v3.0 release pass. Insert the Play
                // Billing / local entitlement cache repository here only as
                // part of a dedicated entitlement audit. Do not enable billing
                // or real runtime gating piecemeal from this provider.
            ),
            fallback = fallbackRepository
        )
    }
}
