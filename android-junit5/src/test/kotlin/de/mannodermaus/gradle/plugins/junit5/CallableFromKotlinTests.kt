package de.mannodermaus.gradle.plugins.junit5

import org.junit.Test

class CallableFromKotlinTests {
  @Test
  fun callableSyntaxWorks() {
    val callable = Callable { 2 + 2 }
    assert(callable() == 4)
  }
}
