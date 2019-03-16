package de.mannodermaus.junit5.test

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DoesntHaveTestMethods

class HasTest {
  @Test
  fun method() {
  }
}

class HasTestFactory {
  @TestFactory
  fun method() = listOf(
      dynamicTest("a") {},
      dynamicTest("b") {}
  )
}

class HasParameterizedTest {
  @ParameterizedTest
  @CsvSource("a", "b")
  fun method(param: String) {
  }
}

class HasInnerClassWithTest {
  @Nested
  inner class InnerClass {
    @Test
    fun method() {
    }
  }
}
