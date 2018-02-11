@file:Suppress("unused")

package de.mannodermaus.junit5.test

import de.mannodermaus.junit5.AndroidJUnit5Builder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.RunnerBuilder
import kotlin.reflect.KClass

class RunnerTests {

  private lateinit var runnerBuilder: RunnerBuilder

  @BeforeEach
  fun beforeEach() {
    this.runnerBuilder = AndroidJUnit5Builder()
  }

  @ParameterizedTest
  @MethodSource("runnerForClass")
  @DisplayName("runnerForClass is correctly determined")
  fun runnerForClass(klass: KClass<*>, expectSuccess: Boolean, expectExecutedTests: Int) {
    val runner = runnerBuilder.runnerForClass(klass.java)
    if (!expectSuccess) {
      assertThat(runner).isNull()

    } else {
      assertThat(runner).isNotNull()

      // Verify number of executed test cases as well
      val notifier = RunNotifier()
      val listener = CountingRunListener()
      notifier.addListener(listener)
      runner.run(notifier)

      assertThat(listener.count())
          .withFailMessage(
              "Executed ${listener.count()} instead of $expectExecutedTests tests: '${listener.methodNames()}'")
          .isEqualTo(expectExecutedTests)
    }
  }

  companion object {
    @JvmStatic
    fun runnerForClass() = listOf(
        Arguments.of(DoesntHaveTestMethods::class, false, 0),
        Arguments.of(HasTest::class, true, 1),
        Arguments.of(HasTestFactory::class, true, 2),
        Arguments.of(HasParameterizedTest::class, true, 2),
        Arguments.of(HasInnerClassWithTest::class, true, 1)
    )
  }
}
