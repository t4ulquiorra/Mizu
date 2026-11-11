package com.mizu.player

import com.mizu.player.util.WatchState
import com.mizu.player.util.WatchStateUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchStateTest {

    @Test
    fun testUnwatchedWhenZeroOrNegative() {
        assertEquals(WatchState.UNWATCHED, WatchStateUtils.getWatchState(0L, 100_000L))
        assertEquals(WatchState.UNWATCHED, WatchStateUtils.getWatchState(-50L, 100_000L))
        assertEquals(WatchState.UNWATCHED, WatchStateUtils.getWatchState(100L, 0L))
    }

    @Test
    fun testUnwatchedUnderThreePercent() {
        // 2% of 100s is 2s (2000ms)
        assertEquals(WatchState.UNWATCHED, WatchStateUtils.getWatchState(2000L, 100_000L))
    }

    @Test
    fun testInProgressBetweenThreeAndNinetyFivePercent() {
        // 5% of 100s is 5000ms
        assertEquals(WatchState.IN_PROGRESS, WatchStateUtils.getWatchState(5000L, 100_000L))
        // 50%
        assertEquals(WatchState.IN_PROGRESS, WatchStateUtils.getWatchState(50_000L, 100_000L))
        // 94%
        assertEquals(WatchState.IN_PROGRESS, WatchStateUtils.getWatchState(94_000L, 100_000L))
    }

    @Test
    fun testWatchedAtOrAboveNinetyFivePercent() {
        // 95%
        assertEquals(WatchState.WATCHED, WatchStateUtils.getWatchState(95_000L, 100_000L))
        // 99%
        assertEquals(WatchState.WATCHED, WatchStateUtils.getWatchState(99_000L, 100_000L))
        // 100%
        assertEquals(WatchState.WATCHED, WatchStateUtils.getWatchState(100_000L, 100_000L))
    }

    @Test
    fun testProgressPercentage() {
        assertEquals(0, WatchStateUtils.getProgressPercentage(0L, 100_000L))
        assertEquals(50, WatchStateUtils.getProgressPercentage(50_000L, 100_000L))
        assertEquals(100, WatchStateUtils.getProgressPercentage(120_000L, 100_000L))
    }
}
