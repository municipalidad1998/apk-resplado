package com.streamvault.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramImportTest {

    private val export = """
        {"name":"Música","type":"personal_chat","messages":[
          {"id":101,"type":"message","date":"2024-01-02T03:04:05","date_unixtime":"1704164645","file":"files/alabanza.mp3","mime_type":"audio/mpeg","media_type":"audio_file","file_size":"5242880","duration_seconds":214,"performer":"Alex Campos","title":"Alabanza"},
          {"id":102,"type":"message","date":"2024-01-02T03:10:00","date_unixtime":"1704165000","file":"files/nota.opus","mime_type":"audio/ogg","media_type":"voice_message","file_size":"102400","duration_seconds":32},
          {"id":103,"type":"message","date":"2024-01-02T03:20:00","date_unixtime":"1704165600","file":"files/foto.jpg","mime_type":"image/jpeg","media_type":"photo","file_size":"204800"},
          {"id":104,"type":"service","date":"2024-01-02T03:30:00"}
        ]}
    """.trimIndent()

    @Test fun readsAudioMessagesWithTheirIdsAndMetadata() {
        val files = TelegramImport.parse(export)
        assertEquals(2, files.size) // the photo and the service message are ignored
        val song = files.first { it.messageId == 101L }
        assertEquals("alabanza.mp3", song.name)
        assertEquals("Alex Campos", song.artist)
        assertEquals(214_000L, song.durationMs)
        assertEquals(5_242_880L, song.size)
        assertTrue(song.date > 0)
    }

    @Test fun anUnrelatedFileGivesNothing() {
        assertTrue(TelegramImport.parse("{\"otro\":1}").isEmpty())
        assertTrue(TelegramImport.parse("no es json").isEmpty())
    }
}
