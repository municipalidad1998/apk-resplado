package com.streamvault.settings

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The backup file has to survive a reinstall, not only an in-app round trip. */
class SettingsBackupTest {

    private val settings = PlayerSettings(
        theme = "dark", crossfade = 30, skipSeconds = 15, detectSilence = false, thresholdDb = -60,
        minimumSilence = 0, autoScan = false, autoPlay = false, shuffle = true, repeat = 2, fades = false,
        animations = false, largeCovers = false, excludedFolders = "Ringtones\nNotifications",
        autoUpdate = false, dynamicColor = false, normalize = false, targetLoudnessDb = -22, compressor = "voice"
    )

    @Test fun exportsEverySettingWithItsOwnName() {
        val json = JSONObject(SettingsBackup.toJson(settings))
        assertEquals(SettingsBackup.FORMAT, json.getString("format"))
        assertEquals("dark", json.getString("theme"))
        assertEquals(30, json.getInt("crossfade"))
        assertEquals("voice", json.getString("compressor"))
        assertEquals(-22, json.getInt("targetLoudnessDb"))
        assertEquals("Ringtones\nNotifications", json.getString("excludedFolders"))
        assertTrue(json.getInt("version") >= 1)
    }

    @Test fun everyExportedValueIsUnderstoodOnImport() {
        val json = JSONObject(SettingsBackup.toJson(settings))
        val missing = json.keys().asSequence().toList()
            .filter { it !in SettingsBackup.fieldNames() && it != "format" && it != "version" }
        assertTrue("Valores que el importador no reconoce: $missing", missing.isEmpty())
        SettingsBackup.fieldNames().forEach { name -> assertTrue("Falta $name", json.has(name)) }
    }

    @Test fun rejectsFilesThatAreNotABackup() {
        assertFalse(SettingsBackup.isBackup("{}"))
        assertFalse(SettingsBackup.isBackup("no es json"))
        assertTrue(SettingsBackup.isBackup(SettingsBackup.toJson(PlayerSettings())))
    }

    /** Applies the file onto a fake editor to prove the historical key names are used. */
    @Test fun writesWithTheSharedPreferencesKeys() {
        val written = mutableMapOf<String, Any?>()
        val editor = FakeEditor(written)
        val applied = SettingsBackup.applyTo(editor, JSONObject(SettingsBackup.toJson(settings)))
        assertEquals(SettingsBackup.fieldNames().size, applied)
        assertEquals(30, written["crossfade"])       // same name
        assertEquals(15, written["skip"])            // skipSeconds -> skip
        assertEquals(false, written["silence"])      // detectSilence -> silence
        assertEquals(-60, written["threshold"])
        assertEquals(0, written["minimum"])
        assertEquals(false, written["autoplay"])
        assertEquals(false, written["covers"])
        assertEquals(true, written["shuffle"])
        assertEquals(2, written["repeat"])
        assertEquals(-22, written["targetLoudness"])
        assertEquals("voice", written["compressor"])
        assertEquals(false, written["normalize"])
    }

    private class FakeEditor(private val target: MutableMap<String, Any?>) : android.content.SharedPreferences.Editor {
        override fun putString(key: String, value: String?): android.content.SharedPreferences.Editor { target[key] = value; return this }
        override fun putInt(key: String, value: Int): android.content.SharedPreferences.Editor { target[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): android.content.SharedPreferences.Editor { target[key] = value; return this }
        override fun apply() = Unit
        override fun commit() = true
        override fun clear(): android.content.SharedPreferences.Editor = this
        override fun remove(key: String): android.content.SharedPreferences.Editor = this
        override fun putStringSet(key: String, values: MutableSet<String>?): android.content.SharedPreferences.Editor = this
        override fun putLong(key: String, value: Long): android.content.SharedPreferences.Editor = this
        override fun putFloat(key: String, value: Float): android.content.SharedPreferences.Editor = this
    }
}
