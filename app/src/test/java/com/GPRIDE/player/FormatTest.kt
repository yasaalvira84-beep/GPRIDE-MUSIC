package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun underOneHour() = assertEquals("3:05", formatTime(185_000))
    @Test fun overOneHour() = assertEquals("1:02:03", formatTime(3_723_000))
    @Test fun zeroAndNegative() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:00", formatTime(-5))
    }
}
