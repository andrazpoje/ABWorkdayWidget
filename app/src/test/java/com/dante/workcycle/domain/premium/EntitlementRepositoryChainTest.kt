package com.dante.workcycle.domain.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementRepositoryChainTest {

    @Test
    fun noneOverride_doesNotUnlockFeature() {
        val repository = OverrideEntitlementRepository(
            EntitlementOverride(
                mode = EntitlementOverrideMode.NONE,
                source = EntitlementSource.DEBUG_OVERRIDE
            )
        )

        assertEquals(
            EntitlementState.UNKNOWN,
            repository.getState(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            EntitlementSource.NONE,
            repository.getSource(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertFalse(repository.isUnlocked(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT))
    }

    @Test
    fun unlockAllPremium_unlocksOneTimePremiumFeature() {
        val repository = OverrideEntitlementRepository(
            EntitlementOverride(
                mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                source = EntitlementSource.DEBUG_OVERRIDE
            )
        )

        assertEquals(
            EntitlementState.UNLOCKED,
            repository.getState(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            EntitlementSource.DEBUG_OVERRIDE,
            repository.getSource(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertTrue(repository.isUnlocked(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT))
    }

    @Test
    fun unlockAllPremium_unlocksSubscriptionFeature_whenUsedForDebugOverride() {
        val repository = OverrideEntitlementRepository(
            EntitlementOverride(
                mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                source = EntitlementSource.DEBUG_OVERRIDE
            )
        )

        assertEquals(
            EntitlementState.UNLOCKED,
            repository.getState(PremiumFeature.CLOUD_SYNC)
        )
        assertEquals(
            EntitlementSource.DEBUG_OVERRIDE,
            repository.getSource(PremiumFeature.CLOUD_SYNC)
        )
        assertTrue(repository.isUnlocked(PremiumFeature.CLOUD_SYNC))
    }

    @Test
    fun lockAllPremium_locksFeature() {
        val repository = OverrideEntitlementRepository(
            EntitlementOverride(
                mode = EntitlementOverrideMode.LOCK_ALL_PREMIUM,
                source = EntitlementSource.TESTER_OVERRIDE
            )
        )

        assertEquals(
            EntitlementState.LOCKED,
            repository.getState(PremiumFeature.ADVANCED_REPORTS)
        )
        assertEquals(
            EntitlementSource.TESTER_OVERRIDE,
            repository.getSource(PremiumFeature.ADVANCED_REPORTS)
        )
        assertFalse(repository.isUnlocked(PremiumFeature.ADVANCED_REPORTS))
    }

    @Test
    fun featureSpecificOverride_unlocksOnlyListedFeature() {
        val repository = OverrideEntitlementRepository(
            EntitlementOverride(
                mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                source = EntitlementSource.TESTER_OVERRIDE,
                features = setOf(PremiumFeature.PDF_EXPORT)
            )
        )

        assertEquals(
            EntitlementState.UNLOCKED,
            repository.getState(PremiumFeature.PDF_EXPORT)
        )
        assertEquals(
            EntitlementSource.TESTER_OVERRIDE,
            repository.getSource(PremiumFeature.PDF_EXPORT)
        )
        assertTrue(repository.isUnlocked(PremiumFeature.PDF_EXPORT))

        assertEquals(
            EntitlementState.UNKNOWN,
            repository.getState(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            EntitlementSource.NONE,
            repository.getSource(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertFalse(repository.isUnlocked(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT))
    }

    @Test
    fun compositeRepository_respectsPriority() {
        val repository = CompositeEntitlementRepository(
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
            )
        )

        assertEquals(
            EntitlementState.LOCKED,
            repository.getState(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            EntitlementSource.DEBUG_OVERRIDE,
            repository.getSource(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertFalse(repository.isUnlocked(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT))
    }

    @Test
    fun compositeFallbackToFreeOnly_locksPremiumFeature() {
        val repository = CompositeEntitlementRepository(
            repositories = emptyList()
        )

        assertEquals(
            EntitlementState.LOCKED,
            repository.getState(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            EntitlementSource.NONE,
            repository.getSource(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertFalse(repository.isUnlocked(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT))
    }

    @Test
    fun unknownOrNoneRepository_doesNotStopChain() {
        val repository = CompositeEntitlementRepository(
            repositories = listOf(
                OverrideEntitlementRepository(
                    EntitlementOverride(
                        mode = EntitlementOverrideMode.NONE,
                        source = EntitlementSource.DEBUG_OVERRIDE
                    )
                ),
                OverrideEntitlementRepository(
                    EntitlementOverride(
                        mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                        source = EntitlementSource.TESTER_OVERRIDE
                    )
                )
            )
        )

        assertEquals(
            EntitlementState.UNLOCKED,
            repository.getState(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            EntitlementSource.TESTER_OVERRIDE,
            repository.getSource(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertTrue(repository.isUnlocked(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT))
    }

    @Test
    fun source_isCarriedIntoGateDecision() {
        val featureGate = FeatureGate(
            CompositeEntitlementRepository(
                repositories = listOf(
                    OverrideEntitlementRepository(
                        EntitlementOverride(
                            mode = EntitlementOverrideMode.UNLOCK_ALL_PREMIUM,
                            source = EntitlementSource.TESTER_OVERRIDE
                        )
                    )
                )
            )
        )

        val decision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        assertTrue(decision.allowed)
        assertEquals(EntitlementState.UNLOCKED, decision.state)
        assertEquals(EntitlementSource.TESTER_OVERRIDE, decision.source)
    }
}
