package com.mizu.player.util

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object SubtitleUtils {

    /**
     * Parses an SRT timestamp format (hh:mm:ss,mmm or hh:mm:ss.mmm) into milliseconds.
     */
    fun parseTimestamp(timestamp: String): Long {
        val parts = timestamp.trim().replace('.', ',').split(":", ",")
        if (parts.size < 4) return 0L
        val hours = parts[0].trim().toLongOrNull() ?: 0L
        val minutes = parts[1].trim().toLongOrNull() ?: 0L
        val seconds = parts[2].trim().toLongOrNull() ?: 0L
        val millis = parts[3].trim().toLongOrNull() ?: 0L
        return hours * 3600_000L + minutes * 60_000L + seconds * 1000L + millis
    }

    /**
     * Strips HTML formatting tags and ASS/SSA styling overrides from subtitle text.
     */
    fun cleanSubtitleText(rawText: String): String {
        var text = rawText
        // Strip HTML tags like <i>, <b>, <u>, <font color="...">, </font>
        text = text.replace(Regex("""<[^>]+>"""), "")
        // Strip ASS/SSA tags like {\an8}, {\pos(100,200)}, {\c&H0000FF&}
        text = text.replace(Regex("""\{[^}]+\}"""), "")
        // Collapse spaces and normalize newlines
        return text.trim()
    }

    /**
     * Detects charset of raw subtitle bytes using BOM or UTF-8 validation with Latin-1 fallback.
     */
    fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }

        // Try decoding as UTF-8
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            StandardCharsets.UTF_8
        } catch (e: Exception) {
            StandardCharsets.ISO_8859_1
        }
    }
}
