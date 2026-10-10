package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerMathTest {
    @Test
    fun silenceGivesZeroBands() {
        val bands = computeBands(ByteArray(1024), 48)
        assertEquals(48, bands.size)
        assertTrue(bands.all { it == 0f })
    }

    @Test
    fun loudSignalStaysWithinRange() {
        val bands = computeBands(ByteArray(1024) { 100 }, 48, 1f)
        assertEquals(48, bands.size)
        assertTrue(bands.all { it in 0f..1f })
        assertTrue(bands.any { it > 0.5f })
    }

    @Test
    fun tooShortInputGivesZeros() {
        val bands = computeBands(ByteArray(8), 48)
        assertEquals(48, bands.size)
        assertTrue(bands.all { it == 0f })
    }
}
