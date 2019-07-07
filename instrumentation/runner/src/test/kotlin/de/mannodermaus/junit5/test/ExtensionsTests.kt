@file:Suppress("unused")

package de.mannodermaus.junit5.test

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.AndroidJUnit5
import de.mannodermaus.junit5.ParsedFilters
import de.mannodermaus.junit5.jupiterTestMethods
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.platform.launcher.TagFilter
import org.junit.runner.notification.RunNotifier

class ExtensionsTests {

  @ParameterizedTest
  @MethodSource("jupiterTestMethods")
  @DisplayName("jupiterTestMethods() has correct values & will execute expected number of tests")
  fun jupiterTestMethods(klass: Class<*>, expectExecutedTests: Int) {
    val methods = klass.jupiterTestMethods()
    if (expectExecutedTests == 0) {
      assertThat(methods).isEmpty()
    } else {
      assertThat(methods).isNotEmpty()

      // Verify number of executed test cases as well
      val notifier = RunNotifier()
      val listener = CountingRunListener()
      notifier.addListener(listener)
      AndroidJUnit5(klass, ParsedFilters.EMPTY).run(notifier)

      assertThat(listener.count())
          .named("Executed ${listener.count()} instead of $expectExecutedTests tests: '${listener.methodNames()}'")
          .isEqualTo(expectExecutedTests)
    }
  }

  @Test
  fun `tag filter works`() {
    val klass = HasTaggedTest::class.java
    val methods = klass.jupiterTestMethods()
    assertThat(methods).hasSize(1)

    // Verify number of executed test cases as well
    val notifier = RunNotifier()
    val listener = CountingRunListener()
    notifier.addListener(listener)

    val filters = listOf(TagFilter.excludeTags("slow"))
    AndroidJUnit5(klass, ParsedFilters(filters)).run(notifier)

    assertThat(listener.count())
        .named("Executed ${listener.count()} instead of 0 tests: '${listener.methodNames()}'")
        .isEqualTo(0)
  }

  companion object {
    @JvmStatic
    fun jupiterTestMethods() = listOf(
        Arguments.of(DoesntHaveTestMethods::class.java, 0),
        Arguments.of(HasTaggedTest::class.java, 1),
        Arguments.of(HasTest::class.java, 1),
        Arguments.of(HasRepeatedTest::class.java, 5),
        Arguments.of(HasTestFactory::class.java, 2),
        Arguments.of(HasTestTemplate::class.java, 2),
        Arguments.of(HasParameterizedTest::class.java, 2),
        Arguments.of(HasInnerClassWithTest::class.java, 1)
    )
  }
}
