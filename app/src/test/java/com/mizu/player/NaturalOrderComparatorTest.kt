package com.mizu.player

import com.mizu.player.util.NaturalOrderComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderComparatorTest {

    @Test
    fun testNaturalOrderNumericSorting() {
        val list = listOf("Episode 10", "Episode 1", "Episode 2", "Episode 20", "Episode 3")
        val sorted = list.sortedWith(NaturalOrderComparator)
        assertEquals(listOf("Episode 1", "Episode 2", "Episode 3", "Episode 10", "Episode 20"), sorted)
    }

    @Test
    fun testCaseInsensitiveSorting() {
        val list = listOf("banana", "Apple", "cherry")
        val sorted = list.sortedWith(NaturalOrderComparator)
        assertEquals(listOf("Apple", "banana", "cherry"), sorted)
    }

    @Test
    fun testMultipleNumericChunks() {
        val list = listOf("v1.10.2", "v1.2.1", "v1.2.10", "v1.1.0")
        val sorted = list.sortedWith(NaturalOrderComparator)
        assertEquals(listOf("v1.1.0", "v1.2.1", "v1.2.10", "v1.10.2"), sorted)
    }

    @Test
    fun testNullSafety() {
        assertTrue(NaturalOrderComparator.compare(null, "a") < 0)
        assertTrue(NaturalOrderComparator.compare("a", null) > 0)
        assertEquals(0, NaturalOrderComparator.compare(null, null))
    }
}
