package de.mannodermaus.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinPaidDebugTest {
  @Test
  fun test() {
    val adder = Adder()
    assertEquals(4, adder.add(2, 2), "This should succeed!")
  }
}
