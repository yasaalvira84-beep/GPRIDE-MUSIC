package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationTest {
    @Test
    fun routesAreUnique() {
        val routes = Destination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }
}
