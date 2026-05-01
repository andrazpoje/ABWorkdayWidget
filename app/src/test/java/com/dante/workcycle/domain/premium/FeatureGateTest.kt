package com.dante.workcycle.domain.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateTest {

    private val repository = FreeOnlyEntitlementRepository()
    private val featureGate = FeatureGate(repository)

    @Test
    fun tierOf_returnsPremiumOneTime_forOneTimeFeatures() {
        assertEquals(
            FeatureTier.PREMIUM_ONE_TIME,
            featureGate.tierOf(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)
        )
        assertEquals(
            FeatureTier.PREMIUM_ONE_TIME,
            featureGate.tierOf(PremiumFeature.DATE_RANGE_CSV_EXPORT)
        )
        assertEquals(
            FeatureTier.PREMIUM_ONE_TIME,
            featureGate.tierOf(PremiumFeature.ADVANCED_WORK_LOG_RULES)
        )
    }

    @Test
    fun tierOf_returnsSubscription_forSubscriptionFeatures() {
        assertEquals(
            FeatureTier.SUBSCRIPTION,
            featureGate.tierOf(PremiumFeature.CLOUD_SYNC)
        )
        assertEquals(
            FeatureTier.SUBSCRIPTION,
            featureGate.tierOf(PremiumFeature.CLOUD_BACKUP)
        )
        assertEquals(
            FeatureTier.SUBSCRIPTION,
            featureGate.tierOf(PremiumFeature.LIVE_WEB_SHARING)
        )
    }

    @Test
    fun freeOnlyRepository_locksPremiumFeature() {
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
    fun canUse_returnsDeniedDecision_forLockedPremiumFeature() {
        val decision = featureGate.canUse(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT)

        assertFalse(decision.allowed)
        assertEquals(PremiumFeature.LOCAL_FULL_BACKUP_EXPORT, decision.feature)
        assertEquals(FeatureTier.PREMIUM_ONE_TIME, decision.tier)
        assertEquals(EntitlementState.LOCKED, decision.state)
        assertEquals(EntitlementSource.NONE, decision.source)
        assertEquals("Feature requires Premium one-time.", decision.reason)
    }

    @Test
    fun premiumFeatureEnum_doesNotContainSplitShiftOrMultipleSessionEntries() {
        val names = PremiumFeature.entries.map { it.name }.toSet()

        assertFalse(names.contains("SPLIT_SHIFTS"))
        assertFalse(names.contains("MULTIPLE_SESSIONS"))
        assertFalse(names.contains("MULTIPLE_WORK_SESSIONS"))
    }

    @Test
    fun subscriptionFeatures_returnSubscriptionTier() {
        val subscriptionFeatures = listOf(
            PremiumFeature.CLOUD_SYNC,
            PremiumFeature.CLOUD_BACKUP,
            PremiumFeature.GPS_GEOFENCING_SUGGESTIONS,
            PremiumFeature.LIVE_WEB_SHARING
        )

        assertTrue(subscriptionFeatures.all { featureGate.tierOf(it) == FeatureTier.SUBSCRIPTION })
    }

    @Test
    fun oneTimeFeatures_returnPremiumOneTimeTier() {
        val oneTimeFeatures = listOf(
            PremiumFeature.LOCAL_FULL_BACKUP_EXPORT,
            PremiumFeature.DATE_RANGE_CSV_EXPORT,
            PremiumFeature.PDF_EXPORT,
            PremiumFeature.MULTIPLE_ACTIVE_WORK_PROFILES,
            PremiumFeature.ADVANCED_STATISTICS,
            PremiumFeature.CUSTOM_TEMPLATES,
            PremiumFeature.ADVANCED_WIDGET_CUSTOMIZATION,
            PremiumFeature.ADVANCED_WORK_LOG_RULES,
            PremiumFeature.ADVANCED_REPORTS
        )

        assertTrue(oneTimeFeatures.all { featureGate.tierOf(it) == FeatureTier.PREMIUM_ONE_TIME })
    }
}
