package com.mizu.player.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object PlaybackLog {
    private const val TAG = "PlaybackLog"
    private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2 MB ring buffer
    private const val LOG_FILE_NAME = "playback_diagnostics.log"

    private var logFile: File? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        logFile = File(context.filesDir, LOG_FILE_NAME)
        log("PlaybackLog", "Diagnostic logger initialized. App start.")
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val timeStr = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val logLine = buildString {
            append("[$timeStr] [$threadName] [$tag] $message")
            if (throwable != null) {
                append("\n").append(Log.getStackTraceString(throwable))
            }
            append("\n")
        }

        // Also output to Android logcat
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }

        executor.execute {
            writeLogLine(logLine)
        }
    }

    fun logEngineChoice(engine: String, uri: Uri?, reason: String) {
        val scheme = uri?.scheme ?: "unknown"
        val path = uri?.path ?: "none"
        val ext = path.substringAfterLast('.', "")
        log("EngineSelection", "Engine: $engine | Scheme: $scheme | Ext: $ext | Reason: $reason | URI: $uri")
    }

    fun logSurfaceEvent(event: String, width: Int = 0, height: Int = 0) {
        log("SurfaceEvent", "Surface: $event (width=$width, height=$height)")
    }

    fun logDecoderInit(trackType: String, decoderName: String, mimeType: String?) {
        log("DecoderInit", "Decoder initialized: type=$trackType, decoder=$decoderName, mime=$mimeType")
    }

    fun logDecoderError(trackType: String, error: String, throwable: Throwable? = null) {
        log("DecoderError", "Decoder error: type=$trackType, error=$error", throwable)
    }

    fun logVlcEvent(eventName: String, details: String = "") {
        log("VLC_Event", "Event: $eventName ${if (details.isNotEmpty()) "($details)" else ""}")
    }

    private fun writeLogLine(line: String) {
        val file = logFile ?: return
        try {
            if (!file.exists()) {
                file.createNewFile()
            }

            // Check if file exceeds max size; if so, trim first half (ring buffer effect)
            if (file.length() > MAX_FILE_SIZE) {
                trimLogFile(file)
            }

            file.appendText(line)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    private fun trimLogFile(file: File) {
        try {
            val halfSize = MAX_FILE_SIZE / 2
            val raf = RandomAccessFile(file, "rw")
            raf.seek(halfSize.toLong())
            val buffer = ByteArray(halfSize)
            val bytesRead = raf.read(buffer)
            raf.setLength(0)
            if (bytesRead > 0) {
                raf.write(buffer, 0, bytesRead)
                raf.write("\n--- [LOG TRIMMED DUE TO SIZE LIMIT] ---\n".toByteArray())
            }
            raf.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming log file", e)
        }
    }

    fun getLogContent(): String {
        val file = logFile ?: return "Log file not initialized"
        return try {
            if (file.exists()) file.readText() else "No logs recorded yet."
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    fun getLogFile(): File? = logFile

    fun clear() {
        executor.execute {
            try {
                logFile?.writeText("")
                log("PlaybackLog", "Diagnostic log cleared by user.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear log", e)
            }
        }
    }
}
