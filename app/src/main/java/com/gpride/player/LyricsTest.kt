package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test
    fun parsesTimestampsAndSkipsMetadata() {
        val lines = parseLrc("[ar:Artis]\n[00:01.50]Satu\n[01:02.5]Dua\n[00:03]Tiga")
        assertEquals(listOf(1500L, 3000L, 62500L), lines.map { it.timeMs })
        assertEquals(listOf("Satu", "Tiga", "Dua"), lines.map { it.text })
    }

    @Test
    fun repeatedTimestampsExpandToMultipleLines() {
        val lines = parseLrc("[00:10.00][00:20.00]Reff")
        assertEquals(2, lines.size)
        assertEquals(listOf(10000L, 20000L), lines.map { it.timeMs })
    }

    @Test
    fun threeDigitFractionIsMilliseconds() {
        assertEquals(1234L, parseLrc("[00:01.234]x").single().timeMs)
    }

    @Test
    fun activeLineFollowsPosition() {
        val lines = parseLrc("[00:05.00]a\n[00:10.00]b\n[00:15.00]c")
        assertEquals(-1, activeLineIndex(lines, 1000))
        assertEquals(0, activeLineIndex(lines, 5000))
        assertEquals(1, activeLineIndex(lines, 12000))
        assertEquals(2, activeLineIndex(lines, 999999))
        assertEquals(-1, activeLineIndex(emptyList(), 1000))
    }

    @Test
    fun plainLyricsTrimsBlankEdges() {
        val lines = plainLyrics("\n\nbaris 1\n\nbaris 2\n\n")
        assertEquals(listOf("baris 1", "", "baris 2"), lines.map { it.text })
    }

    @Test
    fun cleanTitleRemovesNoise() {
        assertEquals("Judul Lagu", cleanTitle("Judul Lagu (Official Video)"))
        assertEquals("Judul Lagu", cleanTitle("Judul Lagu [Lyrics].mp3"))
        assertEquals("Lagu (Remix)", cleanTitle("Lagu (Remix)"))
    }

    @Test
    fun cleanArtistHandlesUnknown() {
        assertNull(cleanArtist("<unknown>"))
        assertNull(cleanArtist("  "))
        assertNull(cleanArtist(null))
        assertTrue(cleanArtist("Band") == "Band")
    }
}
