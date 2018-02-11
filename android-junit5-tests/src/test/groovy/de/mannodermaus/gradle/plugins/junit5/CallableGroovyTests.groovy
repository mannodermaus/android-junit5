package de.mannodermaus.gradle.plugins.junit5

import org.junit.Test

class CallableGroovyTests {
  @Test
  void callable0() {
    def obj = new Callable0<Integer>({ 2 + 2 })
    assert obj() == 4
  }

  @Test
  void callable1() {
    def obj = new Callable1<Boolean, Integer>({ state -> 2 + (state ? 1 : 0) })
    assert obj(true) == 3
    assert obj(false) == 2
  }
}
