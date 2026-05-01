package com.dante.workcycle.domain.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugPremiumEntitlementPrefsTest {

    @Test
    fun debugFalse_getOverrideReturnsNone() {
        val prefs = DebugPremiumEntitlementPrefs(
            storage = InMemoryPremiumOverrideStorage(),
            isDebugBuild = false
        )

        val override = prefs.getOverride()

        assertEquals(EntitlementOverrideMode.NONE, override.mode)
        assertEquals(EntitlementSource.NONE, override.source)
        assertTrue(override.features.isEmpty())
    }

    @Test
    fun debugTrue_noneReturnsNone() {
        val prefs = DebugPremiumEntitlementPrefs(
            storage = InMemoryPremiumOverrideStorage(),
            isDebugBuild = true
        )

        val override = prefs.getOverride()

        assertEquals(EntitlementOverrideMode.NONE, override.mode)
        assertEquals(EntitlementSource.NONE, override.source)
    }

    @Test
    fun debugTrue_unlockAllReturnsUnlockOverride() {
        val prefs = DebugPremiumEntitlementPrefs(
            storage = InMemoryPremiumOverrideStorage(),
            isDebugBuild = true
        )

        prefs.setOverrideMode(EntitlementOverrideMode.UNLOCK_ALL_PREMIUM)

        val override = prefs.getOverride()

        assertEquals(EntitlementOverrideMode.UNLOCK_ALL_PREMIUM, override.mode)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, override.source)
    }

    @Test
    fun debugTrue_lockAllReturnsLockOverride() {
        val prefs = DebugPremiumEntitlementPrefs(
            storage = InMemoryPremiumOverrideStorage(),
            isDebugBuild = true
        )

        prefs.setOverrideMode(EntitlementOverrideMode.LOCK_ALL_PREMIUM)

        val override = prefs.getOverride()

        assertEquals(EntitlementOverrideMode.LOCK_ALL_PREMIUM, override.mode)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, override.source)
    }

    @Test
    fun clearResetsToNone() {
        val prefs = DebugPremiumEntitlementPrefs(
            storage = InMemoryPremiumOverrideStorage(),
            isDebugBuild = true
        )

        prefs.setOverrideMode(EntitlementOverrideMode.UNLOCK_ALL_PREMIUM)
        prefs.clear()

        val override = prefs.getOverride()

        assertEquals(EntitlementOverrideMode.NONE, override.mode)
        assertEquals(EntitlementSource.NONE, override.source)
    }

    @Test
    fun debugOverrideRepository_returnsUnknownNoneWhenDebugFalse() {
        val repository = DebugOverrideEntitlementRepository(
            DebugPremiumEntitlementPrefs(
                storage = InMemoryPremiumOverrideStorage(),
                isDebugBuild = false
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
    fun debugOverrideRepository_returnsUnlockedWhenDebugTrueAndUnlockAll() {
        val prefs = DebugPremiumEntitlementPrefs(
            storage = InMemoryPremiumOverrideStorage(),
            isDebugBuild = true
        )
        prefs.setOverrideMode(EntitlementOverrideMode.UNLOCK_ALL_PREMIUM)

        val repository = DebugOverrideEntitlementRepository(prefs)

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

    private class InMemoryPremiumOverrideStorage : PremiumOverrideStorage {
        private val values = linkedMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun clear() {
            values.clear()
        }
    }
}
