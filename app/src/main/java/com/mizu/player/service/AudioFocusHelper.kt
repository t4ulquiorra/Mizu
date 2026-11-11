package com.mizu.player.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.mizu.player.engine.PlayerEngine
import com.mizu.player.util.DiagnosticLogger

class AudioFocusHelper(
    private val context: Context,
    private val engine: PlayerEngine
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false
    private var isDucked = false
    private var originalVolume = 100

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctx = context ?: this@AudioFocusHelper.context
            when (intent?.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    DiagnosticLogger.log(ctx, "AudioFocusHelper", "Becoming noisy or BT disconnect -> pause")
                    if (engine.isPlaying) {
                        engine.pause()
                    }
                }
            }
        }
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        DiagnosticLogger.log(context, "AudioFocusHelper.focusChange", "focusChange=$focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent focus loss (e.g. another media app starts)
                resumeOnFocusGain = false
                if (engine.isPlaying) {
                    engine.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary focus loss (e.g. incoming call)
                if (engine.isPlaying) {
                    resumeOnFocusGain = true
                    engine.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Short notification sound -> lower volume
                if (engine.isPlaying) {
                    isDucked = true
                    originalVolume = engine.currentVolumePercent
                    engine.setVolumeBoost((originalVolume * 0.25f).toInt().coerceAtLeast(10))
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDucked) {
                    isDucked = false
                    engine.setVolumeBoost(originalVolume)
                }
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    engine.play()
                }
            }
        }
    }

    fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusChangeListener)
        }
        resumeOnFocusGain = false
        isDucked = false
    }

    fun registerNoisyReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        try {
            context.registerReceiver(noisyReceiver, filter)
        } catch (_: Exception) {}
    }

    fun unregisterNoisyReceiver() {
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
    }
}
