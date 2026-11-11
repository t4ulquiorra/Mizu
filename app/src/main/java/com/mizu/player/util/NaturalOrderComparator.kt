package com.mizu.player.util

import java.math.BigInteger

/**
 * Natural (alphanumeric) comparator for strings.
 * E.g., sorts ["Episode 1", "Episode 2", "Episode 10"] correctly instead of
 * alphabetical order ["Episode 1", "Episode 10", "Episode 2"].
 */
object NaturalOrderComparator : Comparator<String> {

    private val CHUNK_REGEX = Regex("""(\d+|\D+)""")

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        val chunks1 = CHUNK_REGEX.findAll(s1).map { it.value }.toList()
        val chunks2 = CHUNK_REGEX.findAll(s2).map { it.value }.toList()

        val minSize = minOf(chunks1.size, chunks2.size)
        for (i in 0 until minSize) {
            val c1 = chunks1[i]
            val c2 = chunks2[i]

            val isDigit1 = c1.first().isDigit()
            val isDigit2 = c2.first().isDigit()

            val cmp = if (isDigit1 && isDigit2) {
                // Compare numeric chunks by numerical value
                try {
                    val num1 = BigInteger(c1)
                    val num2 = BigInteger(c2)
                    num1.compareTo(num2)
                } catch (e: Exception) {
                    c1.compareTo(c2)
                }
            } else {
                // Compare text chunks case-insensitively
                c1.compareTo(c2, ignoreCase = true)
            }

            if (cmp != 0) return cmp
        }

        return chunks1.size.compareTo(chunks2.size)
    }
}
