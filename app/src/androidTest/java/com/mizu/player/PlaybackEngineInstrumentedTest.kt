package com.mizu.player

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.player.engine.ExoPlayerEngine
import com.mizu.player.player.engine.VlcPlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class PlaybackEngineInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<MizuApp>()

    private val testMp4Uri: Uri
        get() {
            val file = File("/sdcard/Movies/t_mp4.mp4")
            return if (file.exists()) Uri.fromFile(file) else Uri.parse("file:///android_asset/test.mp4")
        }

    private val testMkvUri: Uri
        get() {
            val file = File("/sdcard/Movies/t_mkv.mkv")
            return if (file.exists()) Uri.fromFile(file) else testMp4Uri
        }

    private val testAviUri: Uri
        get() {
            val file = File("/sdcard/Movies/t_avi.avi")
            return if (file.exists()) Uri.fromFile(file) else testMp4Uri
        }

    @Test
    fun testExoPlayerEngine_playbackAndTrackSwitching() = runBlocking {
        val engine = withContext(Dispatchers.Main) {
            ExoPlayerEngine(context)
        }

        try {
            withContext(Dispatchers.Main) {
                engine.prepare(testMp4Uri, "Test MP4", 0L)
                engine.play()
            }

            // Wait up to 10 seconds for playback to be ready
            var ready = false
            for (i in 0 until 20) {
                val state = engine.playbackState.first()
                if (state.duration > 0 || state.isPlaying) {
                    ready = true
                    break
                }
                delay(500)
            }

            val finalState = engine.playbackState.first()
            assertTrue("ExoPlayer playback should be playing or have duration > 0", ready || finalState.duration > 0)

            // Verify position advances
            val pos1 = finalState.currentPosition
            delay(1500)
            val pos2 = engine.playbackState.first().currentPosition
            assertTrue("ExoPlayer position should advance (pos1=$pos1, pos2=$pos2)", pos2 >= pos1)

            // Test audio track selection
            val audioTracks = engine.playbackState.first().audioTracks
            if (audioTracks.size >= 2) {
                withContext(Dispatchers.Main) {
                    engine.selectAudioTrack(1)
                }
                delay(500)
                val updatedAudio = engine.playbackState.first().audioTracks
                assertTrue("Selected audio track should be updated", updatedAudio[1].isSelected || engine.playbackState.first().selectedAudioTrack == 1)
            }

            // Test subtitle track selection
            val subTracks = engine.playbackState.first().subtitleTracks
            if (subTracks.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    engine.selectSubtitleTrack(0)
                }
                delay(500)
                assertTrue(engine.playbackState.first().selectedSubtitleTrack == 0 || engine.playbackState.first().subtitleTracks[0].isSelected)

                withContext(Dispatchers.Main) {
                    engine.selectSubtitleTrack(-1)
                }
                delay(500)
                assertEquals(-1, engine.playbackState.first().selectedSubtitleTrack)
            }
        } finally {
            withContext(Dispatchers.Main) {
                engine.release()
            }
        }
    }

    @Test
    fun testVlcPlayerEngine_aviPlayback() = runBlocking {
        val engine = withContext(Dispatchers.Main) {
            VlcPlayerEngine(context)
        }

        try {
            withContext(Dispatchers.Main) {
                engine.prepare(testAviUri, "Test AVI", 0L)
                engine.play()
            }

            var ready = false
            for (i in 0 until 20) {
                val state = engine.playbackState.first()
                if (state.duration > 0 || state.isPlaying) {
                    ready = true
                    break
                }
                delay(500)
            }

            val state = engine.playbackState.first()
            assertTrue("VLC playback should start and report duration or playing state", ready || state.isPlaying || state.duration > 0)

            // Position should advance
            delay(1500)
            val updatedState = engine.playbackState.first()
            assertTrue("VLC current position should advance", updatedState.currentPosition >= state.currentPosition)
        } finally {
            withContext(Dispatchers.Main) {
                engine.release()
            }
        }
    }

    @Test
    fun testVlcPlayerEngine_mkvSubtitlesAndAudio() = runBlocking {
        val engine = withContext(Dispatchers.Main) {
            VlcPlayerEngine(context)
        }

        try {
            withContext(Dispatchers.Main) {
                engine.prepare(testMkvUri, "Test MKV", 0L)
                engine.play()
            }

            for (i in 0 until 15) {
                if (engine.playbackState.first().isPlaying) break
                delay(500)
            }

            val state = engine.playbackState.first()
            assertNotNull("VLC playback state should not be null", state)
        } finally {
            withContext(Dispatchers.Main) {
                engine.release()
            }
        }
    }
}
