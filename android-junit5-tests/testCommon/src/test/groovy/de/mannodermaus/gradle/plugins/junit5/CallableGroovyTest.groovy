package de.mannodermaus.gradle.plugins.junit5

import org.junit.Test

class CallableGroovyTest {
  @Test
  void syntaxWorks() {
    def callable = new Callable({ 2 + 2 })
    assert callable() == 4
  }
}
