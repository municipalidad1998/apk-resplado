package com.streamvault.online

import com.streamvault.network.NetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityChooserTest {

    private val flac = OnlineStream("https://a/song.flac", StreamFormat.FLAC, 900, 96000)
    private val mp3High = OnlineStream("https://a/song.mp3", StreamFormat.MP3, 320)
    private val mp3Low = OnlineStream("https://a/song_vbr.mp3", StreamFormat.MP3, 128)
    private val ogg = OnlineStream("https://a/song.ogg", StreamFormat.OGG, 160)
    private val wifi = NetState(online = true, metered = false, slow = false, wifi = true)
    private val mobile = NetState(online = true, metered = true, slow = false)
    private val slow = NetState(online = true, metered = true, slow = true)
    private val offline = NetState(online = false)

    private fun choose(wanted: Quality, net: NetState, streams: List<OnlineStream> = listOf(flac, mp3High, ogg, mp3Low), wifiOnly: Boolean = false, mobileData: Boolean = true) =
        QualityChooser.choose(streams, wanted, net, wifiOnly, mobileData)

    @Test fun automaticPicksByConnection() {
        assertEquals(flac, choose(Quality.AUTO, wifi))
        // "Normal" is the highest compressed stream up to 256 kbps, not the 320 kbps one.
        assertEquals(ogg, choose(Quality.AUTO, mobile))
        assertEquals(mp3Low, choose(Quality.AUTO, slow))
    }

    @Test fun explicitQualityIsRespected() {
        assertEquals(flac, choose(Quality.HIGH, wifi))
        assertEquals(ogg, choose(Quality.NORMAL, wifi))
        assertEquals(mp3Low, choose(Quality.LOW, wifi))
    }

    @Test fun fallsBackWhenTheWantedTierDoesNotExist() {
        val onlyMp3 = listOf(mp3Low)
        assertEquals(mp3Low, choose(Quality.HIGH, wifi, onlyMp3))
        assertEquals(mp3Low, choose(Quality.NORMAL, wifi, onlyMp3))
        val onlyFlac = listOf(flac)
        assertEquals(flac, choose(Quality.LOW, wifi, onlyFlac))
    }

    @Test fun respectsTheDataSaverSwitches() {
        assertNull(choose(Quality.AUTO, offline))
        assertNull(choose(Quality.AUTO, mobile, wifiOnly = true))
        assertNull(choose(Quality.AUTO, mobile, mobileData = false))
        assertEquals(flac, choose(Quality.AUTO, wifi, wifiOnly = true))
    }

    @Test fun mapsConnectionToAutomaticQuality() {
        assertEquals(Quality.HIGH, QualityChooser.auto(wifi))
        assertEquals(Quality.NORMAL, QualityChooser.auto(mobile))
        assertEquals(Quality.LOW, QualityChooser.auto(slow))
    }
}
