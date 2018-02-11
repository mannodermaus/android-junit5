package de.mannodermaus.sample

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestRunningOnJUnit5 {
  @Test
  fun junit5() {
    Assertions.assertEquals(4, 2 + 2)
  }
}
