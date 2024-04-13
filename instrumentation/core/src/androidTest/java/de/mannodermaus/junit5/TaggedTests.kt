package de.mannodermaus.junit5

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class TaggedTests {
    @Test
    fun includedTest() {
    }

    @Tag("nope")
    @Test
    fun taggedTestDisabledOnMethodLevel() {
        assertEquals(5, 2 + 2)
    }
}

@Tag("nope")
class TaggedTestsDisabledOnClassLevel {
    @Test
    fun excludedTest() {
        assertEquals(5, 2 + 2)
    }
}
