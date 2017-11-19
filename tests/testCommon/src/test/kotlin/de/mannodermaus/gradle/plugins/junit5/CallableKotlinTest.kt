package de.mannodermaus.gradle.plugins.junit5

import org.junit.Test

class CallableKotlinTest {
  @Test
  fun syntaxWorks() {
    val callable = Callable { 2 + 2 }
    assert(callable() == 4)
  }
}
