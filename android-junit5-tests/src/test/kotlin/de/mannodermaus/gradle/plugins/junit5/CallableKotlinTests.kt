package de.mannodermaus.gradle.plugins.junit5

import org.junit.jupiter.api.Test

class CallableKotlinTests {
  @Test
  fun callable0() {
    val obj = Callable0 { 2 + 2 }
    assert(obj() == 4)
  }

  @Test
  fun callable1() {
    val obj = Callable1<Boolean, Int> { 2 + if (this) 1 else 0 }
    assert(obj(true) == 3)
    assert(obj(false) == 2)
  }
}
