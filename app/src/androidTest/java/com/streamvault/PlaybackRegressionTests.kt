@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.streamvault

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.streamvault.data.*
import com.streamvault.playback.LoudnessMath
import com.streamvault.playback.PlaybackEvents
import com.streamvault.ui.LibraryViewModel
import com.streamvault.ui.MainActivity
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class PlaybackRegressionTests {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as LuminaApp
    private val vm get() = ViewModelProvider(compose.activity)[LibraryViewModel::class.java]
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun wav(name: String, seconds: Int, silence: Int = 0): File {
        val rate = 16000; val n = rate * seconds
        val bytes = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        repeat(n) { i -> bytes.putShort(if (i < silence * rate) 0 else (sin(i * 2 * Math.PI * 440 / rate) * 9000).toInt().toShort()) }
        return File(app.cacheDir, name).apply { writeBytes(bytes.array()) }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val file = File(app.getExternalFilesDir(null), "screenshots/$name.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private suspend fun register(file: File, id: String, duration: Long): Track {
        val track = Track(id, Uri.fromFile(file).toString(), file.name, id, durationMs = duration)
        app.library.insert(track)
        app.library.location(AudioLocation(track.uri, id, "regression", file.length(), file.lastModified(), "test"))
        return track
    }
    private suspend fun waitFor(check: () -> Boolean) = withTimeout(25000) { while (!check()) delay(80) }

    @Test fun menuQueuePreviousNextElevenSecondIntroAndQueuePanel() = runBlocking<Unit> {
        val a = wav("regression-intro.wav", 60, 11); val b = wav("regression-next.wav", 60)
        val settings = app.preferences.state.value
        app.preferences.update { it.copy(crossfade = 0, repeat = 0, shuffle = false, detectSilence = true, autoScan = false) }
        val first = register(a, "Regression A", 60000); val second = register(b, "Regression B", 60000)
        try {
            // Default context-menu path, intentionally supplying a stale/unanalysed Track object.
            main { vm.play(first) }
            waitFor { vm.playback.value.currentId == first.id && vm.playback.value.playing }
            assertTrue("Initial silence was not skipped: ${vm.playback.value.position}", vm.playback.value.position >= 10_800)
            assertTrue(vm.playback.value.queue.size >= 2)
            main { vm.next() }
            waitFor { vm.playback.value.currentId == second.id && vm.playback.value.playing }
            main { vm.seek(5000) }
            delay(400)
            app.library.manualOffset(second.id, 15000)
            waitFor { vm.playback.value.position >= 15000 }
            main { vm.previous() }
            waitFor { vm.playback.value.currentId == first.id && vm.playback.value.playing }
            assertTrue(vm.playback.value.position >= 10_800)
            compose.onNodeWithTag("mini-player").performClick()
            compose.onNodeWithTag("open-queue").performClick()
            compose.onNodeWithTag("queue-panel").assertIsDisplayed()
            screenshot("queue-panel")
            compose.onNodeWithContentDescription("Volver al reproductor").assertIsDisplayed().performClick()
            compose.onNodeWithTag("queue-panel").assertDoesNotExist()
            compose.onNodeWithTag("open-queue").assertIsDisplayed()
            screenshot("player")
            compose.onNodeWithContentDescription("Cerrar reproductor").performClick()
        } finally {
            main { if (vm.playback.value.playing) vm.toggle() }
            app.library.hide(first.id); app.library.hide(second.id)
            app.preferences.update { settings }; a.delete(); b.delete()
        }
    }

    @Test fun selectedUsefulEndSkipsTwoMinuteOutroAndUsesPerTrackCrossfade() = runBlocking<Unit> {
        val a = wav("regression-outro.wav", 128); val b = wav("regression-after-outro.wav", 25)
        val settings = app.preferences.state.value
        app.preferences.update { it.copy(crossfade = 0, repeat = 0, shuffle = false, detectSilence = false, autoPlay = true) }
        val first = register(a, "Outro A", 128000); val second = register(b, "Outro B", 25000)
        try {
            main { vm.play(first, listOf(first, second)) }
            waitFor { vm.playback.value.currentId == first.id && vm.playback.value.playing }
            app.library.transition(first.id, 8000, 2) // Update the already-loaded queue, not only a future playback.
            waitFor { PlaybackEvents.mixing.value }
            waitFor { vm.playback.value.currentId == second.id && vm.playback.value.playing }
            assertTrue(vm.playback.value.position in 1000..5000)
        } finally {
            main { if (vm.playback.value.playing) vm.toggle() }
            app.library.hide(first.id); app.library.hide(second.id)
            app.preferences.update { settings }; a.delete(); b.delete()
        }
    }

    @Test fun measuresTheRealLoudnessOfAFile() = runBlocking<Unit> {
        val file = wav("regression-loudness.wav", 20, 0)
        try {
            val measured = withContext(Dispatchers.IO) { com.streamvault.analysis.LoudnessAnalyzer(app).measure(android.net.Uri.fromFile(file).toString()) }
            // 9000/32768 sine: peak about -11.2 dBFS, RMS about -14.2 dBFS.
            assertEquals(-14.2f, measured.rmsDb, 1.5f)
            assertTrue(measured.rmsDb > LoudnessMath.SILENCE_DB)
            assertEquals(0, LoudnessMath.boostMillibels(LoudnessMath.gainDb(-16f, measured.rmsDb))) // louder than target -> attenuate
            assertTrue(LoudnessMath.attenuation(LoudnessMath.gainDb(-16f, measured.rmsDb)) < 1f)
        } finally { file.delete() }
    }

    @Test fun loudnessColumnSurvivesUpgradeFromVersionTwo() = runBlocking<Unit> {
        val name = "loudness-migration.db"
        app.deleteDatabase(name)
        val db = Room.databaseBuilder(app, LibraryDatabase::class.java, name).build()
        db.library().insert(Track("kept", "content://test/2", "file.wav", "Conservar", playbackEndMs = 200000, crossfadeSeconds = 10))
        db.close()
        val raw = android.database.sqlite.SQLiteDatabase.openDatabase(app.getDatabasePath(name).path, null, 0)
        raw.execSQL("ALTER TABLE tracks DROP COLUMN loudnessDb")
        raw.execSQL("DROP TABLE room_master_table")
        raw.version = 2; raw.close()
        val upgraded = Room.databaseBuilder(app, LibraryDatabase::class.java, name)
            .addMigrations(LibraryDatabase.MIGRATION_1_2, LibraryDatabase.MIGRATION_2_3).build()
        try {
            val track = upgraded.library().track("kept")!!
            assertNull(track.loudnessDb)
            assertEquals(200000L, track.playbackEndMs)
            assertEquals(10, track.crossfadeSeconds)
            upgraded.library().loudness("kept", -21.5f)
            assertEquals(-21.5f, upgraded.library().track("kept")!!.loudnessDb!!, 0.01f)
        } finally { upgraded.close(); app.deleteDatabase(name) }
    }

    @Test fun migrationKeepsLibraryAndPlaylistWhenUpdatingFromVersionOne() = runBlocking<Unit> {
        val name = "migration-regression.db"
        app.deleteDatabase(name)
        val db = Room.databaseBuilder(app, LibraryDatabase::class.java, name).build()
        db.library().insert(Track("kept", "content://test/1", "file.wav", "Conservar", favorite = true, manualOffsetMs = 11000))
        db.library().playlist(Playlist("list", "Mi lista")); db.library().addToPlaylist("list", "kept")
        db.close()
        // Build a structurally identical v1 fixture from v2 without depending on an unversioned schema artifact.
        val raw = android.database.sqlite.SQLiteDatabase.openDatabase(app.getDatabasePath(name).path, null, 0)
        raw.execSQL("ALTER TABLE tracks DROP COLUMN playbackEndMs")
        raw.execSQL("ALTER TABLE tracks DROP COLUMN crossfadeSeconds")
        raw.execSQL("DROP TABLE room_master_table")
        raw.execSQL("ALTER TABLE tracks DROP COLUMN loudnessDb")
        raw.version = 1; raw.close()
        // The app registers every step (1->2->3); a real update from the oldest schema needs them all.
        val upgraded = Room.databaseBuilder(app, LibraryDatabase::class.java, name)
            .addMigrations(LibraryDatabase.MIGRATION_1_2, LibraryDatabase.MIGRATION_2_3).build()
        try {
            assertTrue(upgraded.library().track("kept")!!.favorite)
            assertEquals(11000L, upgraded.library().track("kept")!!.manualOffsetMs)
            assertNull(upgraded.library().track("kept")!!.playbackEndMs)
            assertNull(upgraded.library().track("kept")!!.loudnessDb)
            assertEquals(1, upgraded.library().getPlaylistTracks("list").size)
        } finally { upgraded.close(); app.deleteDatabase(name) }
    }
}
