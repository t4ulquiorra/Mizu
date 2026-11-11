package com.mizu.player.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {
    private const val TAG = "MizuDiagnostics"
    private const val LOG_FILE_NAME = "mizu_diagnostics.log"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private fun getLogFiles(context: Context): List<File> {
        val files = mutableListOf<File>()
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                files.add(File(extDir, LOG_FILE_NAME))
            }
        } catch (_: Exception) {}

        try {
            val sharedDir = File(Environment.getExternalStorageDirectory(), "Mizu")
            if (sharedDir.exists() || sharedDir.mkdirs()) {
                files.add(File(sharedDir, LOG_FILE_NAME))
            }
        } catch (_: Exception) {}

        return files
    }

    @Synchronized
    fun log(context: Context, event: String, details: String = "") {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] $event: $details\n"
        Log.i(TAG, logLine.trim())

        for (file in getLogFiles(context)) {
            try {
                FileWriter(file, true).use { writer ->
                    writer.write(logLine)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write diagnostic log to ${file.absolutePath}", e)
            }
        }
    }

    fun logFailsafeEvent(
        context: Context,
        error: String,
        codec: String = "",
        container: String = "",
        audioTrack: String = "",
        subtitleTrack: String = "",
        fallbackStep: Int,
        fixedByStep: Int? = null
    ) {
        val details = buildString {
            append("error=$error, fallbackStep=$fallbackStep")
            if (codec.isNotEmpty()) append(", codec=$codec")
            if (container.isNotEmpty()) append(", container=$container")
            if (audioTrack.isNotEmpty()) append(", audio=$audioTrack")
            if (subtitleTrack.isNotEmpty()) append(", sub=$subtitleTrack")
            if (fixedByStep != null) append(", RESOLVED_BY_STEP=$fixedByStep")
        }
        log(context, if (fixedByStep != null) "FAILSAFE_RESOLVED" else "FAILSAFE_RETRY", details)
    }

    fun init(context: Context) {
        // 1. Setup Global Uncaught Exception Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            log(context, "UNCAUGHT_EXCEPTION", "Thread: ${thread.name}\n$stackTrace")

            defaultHandler?.uncaughtException(thread, throwable)
        }

        log(context, "APP_LAUNCH", "PID: ${android.os.Process.myPid()}")

        // 2. Read ActivityManager.getHistoricalProcessExitReasons on every launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val exitReasons = am?.getHistoricalProcessExitReasons(context.packageName, 0, 5)
                if (!exitReasons.isNullOrEmpty()) {
                    val sb = StringBuilder()
                    sb.append("Found ${exitReasons.size} historical exit reasons:\n")
                    for ((i, reason) in exitReasons.withIndex()) {
                        val reasonStr = when (reason.reason) {
                            ApplicationExitInfo.REASON_ANR -> "ANR"
                            ApplicationExitInfo.REASON_CRASH -> "CRASH"
                            ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
                            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
                            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
                            ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
                            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
                            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
                            ApplicationExitInfo.REASON_OTHER -> "OTHER"
                            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
                            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
                            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
                            ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
                            else -> "UNKNOWN (${reason.reason})"
                        }
                        val exitTime = dateFormat.format(Date(reason.timestamp))
                        sb.append("  #$i: PID=${reason.pid}, Time=$exitTime, Reason=$reasonStr, Status=${reason.status}, Description=${reason.description}\n")

                        try {
                            reason.traceInputStream?.use { stream ->
                                val traceSnippet = stream.bufferedReader().useLines { lines ->
                                    lines.take(20).joinToString("\n")
                                }
                                if (traceSnippet.isNotBlank()) {
                                    sb.append("    Trace Snippet:\n$traceSnippet\n")
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    log(context, "HISTORICAL_EXIT_REASONS", sb.toString().trim())
                } else {
                    log(context, "HISTORICAL_EXIT_REASONS", "None found")
                }
            } catch (e: Exception) {
                log(context, "HISTORICAL_EXIT_REASONS_ERROR", e.message ?: "Unknown error")
            }
        }
    }
}
