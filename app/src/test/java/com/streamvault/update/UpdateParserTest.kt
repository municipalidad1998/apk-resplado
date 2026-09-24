package com.streamvault.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateParserTest {

    private fun release() = """
        {
          "tag_name": "v2.2.0",
          "name": "Reproductor de música Denilson 2.2.0",
          "body": "Cambios de esta versión",
          "assets": [
            { "name": "lumina-build-reports.zip", "browser_download_url": "https://example.com/a.zip", "size": 10 },
            { "name": "reproductor-denilson-2.2.0.apk", "browser_download_url": "https://github.com/o/r/releases/download/v2.2.0/reproductor-denilson-2.2.0.apk", "size": 21470528 }
          ]
        }
    """.trimIndent()

    @Test fun readsTheApkAssetOfTheLatestRelease() {
        val info = UpdateParser.parse(release())!!
        assertEquals("v2.2.0", info.tag)
        assertEquals("2.2.0", info.version)
        assertEquals("reproductor-denilson-2.2.0.apk", info.apkName)
        assertEquals(21_470_528L, info.sizeBytes)
        assertTrue(info.apkUrl.endsWith(".apk"))
    }

    @Test fun ignoresReleasesWithoutAnApk() {
        assertNull(UpdateParser.parse("""{"tag_name":"v2.2.0","assets":[]}"""))
        assertNull(UpdateParser.parse("""{"tag_name":"v2.2.0","assets":[{"name":"notes.txt","browser_download_url":"https://x/y.txt"}]}"""))
        assertNull(UpdateParser.parse("not json"))
        assertNull(UpdateParser.parse("""{"assets":[]}"""))
    }

    @Test fun avoidsNonHttpsDownloads() {
        val json = """{"tag_name":"v2.2.0","assets":[{"name":"a.apk","browser_download_url":"http://inseguro/a.apk"}]}"""
        assertNull(UpdateParser.parse(json))
    }

    @Test fun comparesVersionsNumerically() {
        assertTrue(UpdateParser.isNewer("2.2.0", "2.1.0"))
        assertTrue(UpdateParser.isNewer("2.10.0", "2.9.0"))
        assertTrue(UpdateParser.isNewer("v3.0.0", "2.9.9"))
        assertFalse(UpdateParser.isNewer("2.1.0", "2.1.0"))
        assertFalse(UpdateParser.isNewer("2.1.0", "2.2.0"))
        assertFalse(UpdateParser.isNewer("beta", "2.2.0"))
        assertFalse(UpdateParser.isNewer("2.2.0", ""))
    }
}
