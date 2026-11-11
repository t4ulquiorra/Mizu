package com.mizu.player

import com.mizu.player.util.SubtitleUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class SubtitleUtilsTest {

    @Test
    fun testParseTimestamp() {
        assertEquals(83_456L, SubtitleUtils.parseTimestamp("00:01:23,456"))
        assertEquals(3_661_789L, SubtitleUtils.parseTimestamp("01:01:01.789"))
        assertEquals(0L, SubtitleUtils.parseTimestamp("invalid"))
    }

    @Test
    fun testCleanSubtitleTextHtml() {
        val raw = "<i>Hello</i> <b>World</b>, <font color=\"#ff0000\">how are you?</font>"
        assertEquals("Hello World, how are you?", SubtitleUtils.cleanSubtitleText(raw))
    }

    @Test
    fun testCleanSubtitleTextAss() {
        val raw = "{\\an8\\pos(192,20)}Top subtitle text!{\\c&H0000FF&}"
        assertEquals("Top subtitle text!", SubtitleUtils.cleanSubtitleText(raw))
    }

    @Test
    fun testDetectCharsetBom() {
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(), 0x41)
        assertEquals(StandardCharsets.UTF_8, SubtitleUtils.detectCharset(utf8Bom))

        val utf16Le = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x41, 0x00)
        assertEquals(StandardCharsets.UTF_16LE, SubtitleUtils.detectCharset(utf16Le))

        val utf16Be = byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0x00, 0x41)
        assertEquals(StandardCharsets.UTF_16BE, SubtitleUtils.detectCharset(utf16Be))
    }
}
