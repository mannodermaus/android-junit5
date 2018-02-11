package de.mannodermaus.sample

import org.junit.Assert
import org.junit.Test

class TestRunningOnJUnit4 {
  @Test
  fun junit4() {
    Assert.assertEquals(4, 2 + 2)
  }
}
