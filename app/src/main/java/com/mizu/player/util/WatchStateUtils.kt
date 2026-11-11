package com.mizu.player.util

enum class WatchState {
    UNWATCHED,
    IN_PROGRESS,
    WATCHED
}

object WatchStateUtils {

    /**
     * Determines watch state according to spec:
     * - UNWATCHED: no history or pos < 3%
     * - IN_PROGRESS: 3% to 95%
     * - WATCHED: >= 95%
     */
    fun getWatchState(positionMs: Long, durationMs: Long): WatchState {
        if (durationMs <= 0 || positionMs <= 0) return WatchState.UNWATCHED
        val ratio = positionMs.toDouble() / durationMs.toDouble()
        return when {
            ratio < 0.03 -> WatchState.UNWATCHED
            ratio >= 0.95 -> WatchState.WATCHED
            else -> WatchState.IN_PROGRESS
        }
    }

    fun getProgressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0 || positionMs <= 0) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun getProgressPercentage(positionMs: Long, durationMs: Long): Int {
        return (getProgressFraction(positionMs, durationMs) * 100).toInt()
    }
}
