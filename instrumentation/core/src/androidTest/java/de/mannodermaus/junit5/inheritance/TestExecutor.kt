package de.mannodermaus.junit5.inheritance

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

abstract class TestExecutor {

    @Test
    fun executeTest() {
        assertNotNull(getFilename())
    }

    abstract fun getFilename(): String?
}