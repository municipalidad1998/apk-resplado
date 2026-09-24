@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.streamvault

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.streamvault.analysis.SilenceAnalyzer
import com.streamvault.data.*
import com.streamvault.playback.*
import com.streamvault.scanner.AudioScanner
import com.streamvault.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class DeviceTests {
    private val app get() = ApplicationProvider.getApplicationContext<LuminaApp>()
    private fun wav(name: String, seconds: Int, silentSeconds: Int = 0): File {
        val rate = 16000; val count = rate * seconds
        val data = ByteBuffer.allocate(44 + count * 2).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + count * 2).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(count * 2)
        repeat(count) { i -> data.putShort(if (i < rate * silentSeconds) 0 else (sin(i * 2 * Math.PI * 440 / rate) * 9000).toInt().toShort()) }
        return File(app.cacheDir, name).apply { writeBytes(data.array()) }
    }
    @Test fun roomPreservesEditsFavoritesPlaylistsAndQueue() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java).build()
        try {
            val dao = database.library()
            val track = Track("hash", "content://audio/1", "AUD-20260923-WA0001.opus", "Audio", durationMs = 10000)
            dao.insert(track); dao.update(track.copy(customName = "Mensaje de Juan", notes = "Reunión")); dao.favorite("hash")
            assertEquals(-1L, dao.insert(track)) // Re-scan cannot overwrite custom metadata.
            assertTrue(dao.track("hash")!!.favorite); assertEquals("Mensaje de Juan", dao.track("hash")!!.displayName)
            dao.playlist(Playlist("list", "Mis favoritas")); dao.addToPlaylist("list", "hash"); dao.addToPlaylist("list", "hash")
            assertEquals(1, dao.getPlaylistTracks("list").size)
            dao.saveQueue(listOf("hash", "hash")); assertEquals(2, dao.savedQueue().size)
            dao.location(AudioLocation(track.uri, "hash", "test", 1, 1, "scan1")); dao.reconcile(); assertTrue(dao.track("hash")!!.available)
            dao.prune("test", "scan2"); dao.reconcile(); assertFalse(dao.track("hash")!!.available)
            assertEquals("Mensaje de Juan", dao.track("hash")!!.displayName)
        } finally { database.close() }
    }
    @Test fun realDecoderFindsEightSecondsOfSilenceWithoutModifyingSource() = runBlocking {
        val file = wav("silence-test.wav", 10, 8)
        val original = file.readBytes()
        try {
            val result = SilenceAnalyzer(app).analyze(Uri.fromFile(file).toString(), -45, 1)
            assertTrue(result.foundSound); assertTrue("offset ${result.offsetMs}", result.offsetMs in 7800..8100)
            assertArrayEquals(original, file.readBytes()); assertTrue(result.waveform.isNotEmpty())
        } finally { file.delete() }
    }
    @Test fun mediaStoreScanningDeduplicatesAndIsIncremental() = runBlocking {
        if (Build.VERSION.SDK_INT < 29) return@runBlocking
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_AUDIO" else "android.permission.READ_EXTERNAL_STORAGE")
        val file = wav("scan-test.wav", 2)
        val resolver = app.contentResolver
        val uris = mutableListOf<Uri>()
        try {
            repeat(2) { n ->
                val values = ContentValues().apply { put(MediaStore.Audio.Media.DISPLAY_NAME, "lumina-device-test-$n.wav"); put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav"); put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/LuminaTest/"); put(MediaStore.Audio.Media.IS_PENDING, 1) }
                val uri = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)!!; uris += uri
                resolver.openOutputStream(uri)!!.use { it.write(file.readBytes()) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            }
            val warnings = AudioScanner(app) { _, _ -> }.scan()
            assertTrue("Scanner warnings: $warnings", warnings.isEmpty())
            val a = app.library.location(uris[0].toString())!!; val b = app.library.location(uris[1].toString())!!
            assertEquals(a.trackId, b.trackId)
            val t = app.library.track(a.trackId)!!; app.library.update(t.copy(customName = "Mi audio"))
            AudioScanner(app) { _, _ -> }.scan()
            assertEquals("Mi audio", app.library.track(a.trackId)!!.displayName)
            assertEquals(2, app.library.locations(a.trackId).size)
        } finally { uris.forEach { resolver.delete(it, null, null) }; file.delete(); AudioScanner(app) { _, _ -> }.scan() }
    }
    @Test fun mediaSessionPlaysSeeksCrossfadesAndSurvivesBackground() = runBlocking {
        val a = wav("crossfade-a.wav", 8); val b = wav("crossfade-b.wav", 8)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var controller: MediaController? = null
        try {
            app.preferences.update { it.copy(crossfade = 2, autoPlay = true, repeat = 0, shuffle = false) }
            lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
            instrumentation.runOnMainSync { future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync() }
            val c = future.get(15, TimeUnit.SECONDS); controller = c
            instrumentation.runOnMainSync {
                val settings = app.preferences.state.value
                c.setMediaItems(listOf(Track("test-a", Uri.fromFile(a).toString(), a.name, "Prueba A", durationMs = 8000).mediaItem(settings), Track("test-b", Uri.fromFile(b).toString(), b.name, "Prueba B", durationMs = 8000).mediaItem(settings)))
                c.prepare(); c.play()
            }
            withTimeout(15000) { PlaybackEvents.mixing.first { it } }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            withTimeout(10000) {
                while (true) {
                    var next = false
                    instrumentation.runOnMainSync { next = c.currentMediaItem?.mediaId == "test-b" && c.isPlaying }
                    if (next) break
                    delay(100)
                }
            }
            instrumentation.runOnMainSync { c.pause(); c.seekTo(1000) }
            delay(500)
            instrumentation.runOnMainSync { assertFalse(c.isPlaying); assertTrue(c.currentPosition in 900..1200) }
        } finally {
            instrumentation.runOnMainSync { controller?.stop(); controller?.clearMediaItems(); controller?.release() }
            scenario.close(); a.delete(); b.delete(); app.preferences.update { it.copy(crossfade = 5) }
        }
    }
}
