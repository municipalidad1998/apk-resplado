package com.streamvault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Installing a new APK over the previous one only works while three things stay true:
 * the package name never changes, the version code always grows and every build is signed with
 * the same key. The first two are guarded here; the key is guarded by the CI
 * (docs/FIRMA_Y_ACTUALIZACIONES.md).
 */
class VersioningTest {

    /** Changing this id turns the next APK into a different app: Android refuses to update. */
    @Test fun applicationIdNeverChanges() {
        assertEquals("com.streamvault", BuildConfig.APPLICATION_ID)
    }

    @Test fun versionCodeGrowsWithEveryRelease() {
        assertTrue("versionCode must be greater than the previous release (8)",
            BuildConfig.VERSION_CODE > 8)
    }

    @Test fun versionNameIsSemantic() {
        assertTrue("versionName like 2.7.0, got ${BuildConfig.VERSION_NAME}",
            Regex("\\d+\\.\\d+\\.\\d+").matches(BuildConfig.VERSION_NAME))
    }

    @Test fun updatesAreRecognisedFromTheReleaseTag() {
        val current = BuildConfig.VERSION_NAME
        assertTrue(com.streamvault.update.UpdateParser.isNewer("2.8.0", current))
        assertTrue(!com.streamvault.update.UpdateParser.isNewer(current, current))
    }
}
