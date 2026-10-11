package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcFormatTest {
    @Test
    fun formatsLrcTime() {
        assertEquals("00:00.00", formatLrcTime(0))
        assertEquals("01:02.50", formatLrcTime(62_500))
        assertEquals("100:00.00", formatLrcTime(6_000_000))
    }

    @Test
    fun segmentsRoundTripThroughParser() {
        val lines = listOf(LyricLine(1_500, "Satu"), LyricLine(65_230, "Dua\nbaris"))
        val parsed = parseLrc(segmentsToLrc(lines))
        assertEquals(listOf(1_500L, 65_230L), parsed.map { it.timeMs })
        assertEquals(listOf("Satu", "Dua baris"), parsed.map { it.text })
    }

    @Test
    fun detectsCommonHallucinations() {
        assertTrue(isLikelyHallucination("Terima kasih telah menonton!"))
        assertTrue(isLikelyHallucination("Thanks for watching"))
        assertFalse(isLikelyHallucination("Aku rindu padamu malam ini"))
    }

    @Test
    fun keyUsesSongIdOrStableHash() {
        assertEquals("42", lyricsKey(42L, "A", "B"))
        assertEquals(lyricsKey(null, "A", "B"), lyricsKey(null, "A", "B"))
        assertTrue(lyricsKey(null, "A", "B").startsWith("t"))
    }
}
