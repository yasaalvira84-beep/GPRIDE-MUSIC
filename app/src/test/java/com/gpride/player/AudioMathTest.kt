package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMathTest {
    @Test
    fun presetMapsToBandCountWithinRange() {
        val levels = presetBandLevels(floatArrayOf(6f, 0f, 0f, 0f, -6f), 5, -1500, 1500)
        assertEquals(5, levels.size)
        assertEquals(600, levels.first())
        assertEquals(-600, levels.last())
    }

    @Test
    fun presetInterpolatesForOtherBandCounts() {
        val levels = presetBandLevels(floatArrayOf(0f, 0f, 0f, 0f, 4f), 3, -1500, 1500)
        assertEquals(3, levels.size)
        assertEquals(0, levels[0])
        assertEquals(0, levels[1])
        assertEquals(400, levels[2])
    }

    @Test
    fun presetIsClampedToDeviceRange() {
        val levels = presetBandLevels(floatArrayOf(12f, 12f), 2, -300, 300)
        assertTrue(levels.all { it == 300 })
    }

    @Test
    fun customLevelsArePaddedAndClamped() {
        val info = EqInfo(4, -500, 500, listOf(60, 230, 910, 3600))
        val levels = eqLevels(AudioPrefs(eqPreset = -1, eqCustomMb = listOf(900, -900)), info)
        assertEquals(listOf(500, -500, 0, 0), levels.toList())
    }

    @Test
    fun fadeFactorRampsAtStartAndEnd() {
        assertEquals(0f, fadeFactor(0, 200_000, 4_000), 0.001f)
        assertEquals(0.5f, fadeFactor(2_000, 200_000, 4_000), 0.001f)
        assertEquals(1f, fadeFactor(100_000, 200_000, 4_000), 0.001f)
        assertEquals(0.5f, fadeFactor(198_000, 200_000, 4_000), 0.001f)
    }

    @Test
    fun fadeOffOrUnknownDurationKeepsFullVolume() {
        assertEquals(1f, fadeFactor(0, 200_000, 0), 0.001f)
        assertEquals(1f, fadeFactor(0, 0, 4_000), 0.001f)
    }

    @Test
    fun sleepFadeRampsDownInLastWindow() {
        assertEquals(1f, sleepFadeFactor(20_000, 8_000), 0.001f)
        assertEquals(0.5f, sleepFadeFactor(4_000, 8_000), 0.001f)
        assertEquals(0f, sleepFadeFactor(0, 8_000), 0.001f)
    }

    @Test
    fun formatHzUsesKilohertz() {
        assertEquals("60 Hz", formatHz(60))
        assertEquals("1 kHz", formatHz(1000))
        assertEquals("14 kHz", formatHz(14000))
    }
}
