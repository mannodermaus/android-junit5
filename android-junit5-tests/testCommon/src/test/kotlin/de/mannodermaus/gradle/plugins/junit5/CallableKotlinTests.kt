package de.mannodermaus.gradle.plugins.junit5

import org.junit.Test

class CallableKotlinTests {
  @Test
  fun callable() {
    val obj = Callable { 2 + 2 }
    assert(obj() == 4)
  }

  @Test
  fun callable1() {
    val obj = Callable1<Boolean, Int> { state -> 2 + if (state) 1 else 0 }
    assert(obj(true) == 3)
    assert(obj(false) == 2)
  }
}
