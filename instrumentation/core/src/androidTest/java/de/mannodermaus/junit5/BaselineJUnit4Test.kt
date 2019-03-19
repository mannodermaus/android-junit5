package de.mannodermaus.junit5

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * This JUnit 4 test class is here to validate
 * that older device, which ignore JUnit 5 tests,
 * still execute older ones.
 */
class BaselineJUnit4Test {

  @Test
  fun run() {
    assertEquals(4, 2 + 2)
  }
}
