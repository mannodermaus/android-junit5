package de.mannodermaus.gradle.plugins.junit5

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CallableGroovyTests {

  @Test
  @DisplayName("Callable0 can be invoked")
  void callable0() {
    def obj = new Callable0<Integer>({ 2 + 2 })
    assert obj() == 4
  }

  @Test
  @DisplayName("Callable1 can be invoked with parameter true")
  void callable1True() {
    def obj = new Callable1<Boolean, Integer>({ state -> 2 + (state ? 1 : 0) })
    assert obj(true) == 3
  }

  @Test
  @DisplayName("Callable1 can be invoked with parameter false")
  void callable1False() {
    def obj = new Callable1<Boolean, Integer>({ state -> 2 + (state ? 1 : 0) })
    assert obj(false) == 2
  }
}
