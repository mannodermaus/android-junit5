package de.mannodermaus.gradle.plugins.junit5

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class CallableKotlinSpec : Spek({
  describe("a Callable0") {
    val callable by memoized { Callable0 { 2 + 2 } }

    on("invoking it through Kotlin syntax") {
      val result = callable()

      it("produces expected result") {
        assertThat(result).isEqualTo(4)
      }
    }
  }

  describe("a Callable1") {
    val callable by memoized { Callable1<Boolean, Int> { 2 + if (this) 1 else 0 } }

    on("invoking it through Kotlin syntax") {
      listOf(
          true to 3,
          false to 2).forEach { (bool, expected) ->
        it("produces expected result with parameter $bool") {
          assertThat(callable(bool)).isEqualTo(expected)
        }
      }
    }
  }
})
