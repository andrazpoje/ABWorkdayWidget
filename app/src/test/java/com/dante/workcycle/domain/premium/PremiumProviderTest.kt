package com.dante.workcycle.domain.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumProviderTest {

    @Test
    fun chainWithDebugUnlock_enablesOneTimePremiumFeature() {
        val featureGate = FeatureGate(
            PremiumProvider.createRepositoryChain(
                debugOverrideRepository = OverrideEntitlementRepository(
                    EntitlementOverride(
                        mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                        source = EntitlementSource.DEBUG_OVERRIDE
                    )
                )
            )
        )

        val decision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        assertTrue(decision.allowed)
        assertEquals(EntitlementState.UNLOCKED, decision.state)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, decision.source)
    }

    @Test
    fun chainWithDebugUnlock_enablesSubscriptionFeature() {
        val featureGate = FeatureGate(
            PremiumProvider.createRepositoryChain(
                debugOverrideRepository = OverrideEntitlementRepository(
                    EntitlementOverride(
                        mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                        source = EntitlementSource.DEBUG_OVERRIDE
                    )
                )
            )
        )

        val decision = featureGate.canUse(PremiumFeature.CLOUD_SYNC)

        assertTrue(decision.allowed)
        assertEquals(FeatureTier.SUBSCRIPTION, decision.tier)
        assertEquals(EntitlementState.UNLOCKED, decision.state)
    }

    @Test
    fun chainWithDebugLock_deniesPremiumFeature() {
        val featureGate = FeatureGate(
            PremiumProvider.createRepositoryChain(
                debugOverrideRepository = OverrideEntitlementRepository(
                    EntitlementOverride(
                        mode = EntitlementOverrideMode.LOCK_ALL_PREMIUM,
                        source = EntitlementSource.DEBUG_OVERRIDE
                    )
                )
            )
        )

        val decision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        assertFalse(decision.allowed)
        assertEquals(EntitlementState.LOCKED, decision.state)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, decision.source)
    }

    @Test
    fun noneDebugOverride_fallsBackToLockedFallback() {
        val featureGate = FeatureGate(
            PremiumProvider.createRepositoryChain(
                debugOverrideRepository = OverrideEntitlementRepository(
                    EntitlementOverride(
                        mode = EntitlementOverrideMode.NONE,
                        source = EntitlementSource.DEBUG_OVERRIDE
                    )
                )
            )
        )

        val decision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        assertFalse(decision.allowed)
        assertEquals(EntitlementState.LOCKED, decision.state)
        assertEquals(EntitlementSource.NONE, decision.source)
    }

    @Test
    fun repositoryChainPriorityRemainsCorrect() {
        val featureGate = FeatureGate(
            CompositeEntitlementRepository(
                repositories = listOf(
                    OverrideEntitlementRepository(
                        EntitlementOverride(
                            mode = EntitlementOverrideMode.LOCK_ALL_PREMIUM,
                            source = EntitlementSource.DEBUG_OVERRIDE
                        )
                    ),
                    OverrideEntitlementRepository(
                        EntitlementOverride(
                            mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                            source = EntitlementSource.TESTER_OVERRIDE
                        )
                    )
                ),
                fallback = FreeOnlyEntitlementRepository()
            )
        )

        val decision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        assertFalse(decision.allowed)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, decision.source)
    }

    @Test
    fun providerHelperDoesNotMemoizeResolvedDecisions() {
        val mutableRepository = MutableEntitlementRepository()
        val featureGate = FeatureGate(
            PremiumProvider.createRepositoryChain(
                debugOverrideRepository = mutableRepository
            )
        )

        val lockedDecision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        assertFalse(lockedDecision.allowed)

        mutableRepository.override = EntitlementOverride(
            mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
            source = EntitlementSource.DEBUG_OVERRIDE
        )

        val unlockedDecision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        assertTrue(unlockedDecision.allowed)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, unlockedDecision.source)
    }

    private class MutableEntitlementRepository : EntitlementRepository {
        var override: EntitlementOverride = EntitlementOverride(
            mode = EntitlementOverrideMode.NONE,
            source = EntitlementSource.DEBUG_OVERRIDE
        )

        override fun getState(feature: PremiumFeature): EntitlementState {
            return OverrideEntitlementRepository(override).getState(feature)
        }

        override fun getSource(feature: PremiumFeature): EntitlementSource {
            return OverrideEntitlementRepository(override).getSource(feature)
        }

        override fun isUnlocked(feature: PremiumFeature): Boolean {
            return OverrideEntitlementRepository(override).isUnlocked(feature)
        }
    }
}
