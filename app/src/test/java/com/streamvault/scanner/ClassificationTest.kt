package com.streamvault.scanner

import org.junit.Assert.*
import org.junit.Test

class ClassificationTest {
    @Test fun whatsappByFilenameOrFolder() {
        assertEquals("whatsapp", AudioScanner.classify("AUD-20260923-WA0001.opus", "Download"))
        assertEquals("whatsapp", AudioScanner.classify("voice.opus", "Android/media/com.whatsapp/WhatsApp Voice Notes"))
        assertEquals("whatsapp", AudioScanner.classify("PTT-20260923-WA0001.opus", "Download"))
    }
    @Test fun recordings() { assertEquals("recording", AudioScanner.classify("meeting.m4a", "Grabaciones")); assertEquals("music", AudioScanner.classify("song.mp3", "Music")) }
    @Test fun datesAreValidated() { assertNotNull(AudioScanner.dateFromName("AUD-20260923-WA0001.opus")); assertNull(AudioScanner.dateFromName("AUD-20269999-WA0001.opus")); assertNull(AudioScanner.dateFromName("music.mp3")) }
}
