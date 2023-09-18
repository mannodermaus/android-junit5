package de.mannodermaus.sample

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Execution(ExecutionMode.CONCURRENT)
class TestRunningOnJUnit5 {
  @Test
  fun junit5_1() {
    Thread.sleep(1000)
    Assertions.assertEquals(4, 2 + 2)
  }

  @Disabled
  @Test
  fun junit5_2() {
    Thread.sleep(2000)
    Assertions.assertEquals(4, 2 + 2)
  }

  @Test
  fun junit5_3() {
    Thread.sleep(3000)
    Assertions.assertEquals(4, 2 + 2)
  }

  @ValueSource(ints = [1, 2, 3])
  @ParameterizedTest
  fun junit5_parameterized(value: Int) {
    Thread.sleep(value * 1000L)
  }
}
