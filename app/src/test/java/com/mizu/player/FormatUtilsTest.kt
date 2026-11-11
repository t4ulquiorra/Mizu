package com.mizu.player

import com.mizu.player.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatDurationSeconds() {
        assertEquals("00:45", FormatUtils.formatDuration(45_000L))
    }

    @Test
    fun testFormatDurationMinutes() {
        assertEquals("03:25", FormatUtils.formatDuration(205_000L))
    }

    @Test
    fun testFormatDurationHours() {
        assertEquals("1:15:30", FormatUtils.formatDuration(4_530_000L))
    }

    @Test
    fun testFormatDurationZeroOrNegative() {
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("00:00", FormatUtils.formatDuration(-100L))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FormatUtils.formatFileSize(0L))
        assertEquals("500 B", FormatUtils.formatFileSize(500L))
        assertEquals("1.5 MB", FormatUtils.formatFileSize(1_572_864L))
        assertEquals("2 GB", FormatUtils.formatFileSize(2_147_483_648L))
    }

    @Test
    fun testFormatResolution() {
        assertEquals("4K", FormatUtils.formatResolution(3840, 2160))
        assertEquals("1080p", FormatUtils.formatResolution(1920, 1080))
        assertEquals("720p", FormatUtils.formatResolution(1280, 720))
        assertEquals("480p", FormatUtils.formatResolution(854, 480))
        assertEquals("", FormatUtils.formatResolution(0, 0))
    }

    @Test
    fun testCleanMediaTitle() {
        assertEquals(
            "Cowboy Bebop - 01",
            FormatUtils.cleanMediaTitle("[SubsPlease] Cowboy Bebop - 01 [1080p][x265][AAC].mkv")
        )
        assertEquals(
            "Steins Gate S01E05",
            FormatUtils.cleanMediaTitle("Steins.Gate.S01E05.1080p.WEBRip.x264-FLUX.mp4")
        )
        assertEquals(
            "Sample Video",
            FormatUtils.cleanMediaTitle("Sample_Video.mp4")
        )
    }

    @Test
    fun testMiddleEllipsis() {
        val longTitle = "Cowboy Bebop Extra Long Episode Name 24 Final Battle"
        val shortened = FormatUtils.middleEllipsis(longTitle, 25)
        assertEquals(25, shortened.length)
        assertEquals("Cowboy Bebop…Final Battle", shortened)
    }
}
