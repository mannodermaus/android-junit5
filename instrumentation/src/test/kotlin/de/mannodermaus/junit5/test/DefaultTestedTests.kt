package de.mannodermaus.junit5.test

import android.app.Activity
import android.app.Instrumentation
import de.mannodermaus.junit5.DefaultTested
import de.mannodermaus.junit5.MissingTestedParameterException
import de.mannodermaus.junit5.ParameterType
import de.mannodermaus.junit5.ParameterType.InvalidTestedWrapper
import de.mannodermaus.junit5.ParameterType.Unknown
import de.mannodermaus.junit5.ParameterType.ValidTestedWrapper
import de.mannodermaus.junit5.UnexpectedActivityException
import de.mannodermaus.junit5.test.ExpectedResult.Bool
import de.mannodermaus.junit5.test.ExpectedResult.Throws
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.util.stream.Stream

class DefaultTestedTests {

  private lateinit var mockInstrumentation: Instrumentation

  @BeforeEach
  fun beforeEach() {
    this.mockInstrumentation = Mockito.mock(Instrumentation::class.java)
  }

  @Test
  @DisplayName("Execution: Works on Activity Parameter")
  fun worksWhenUsingActivityParameterType() {
    val tested = create(SomeActivity::class.java, listOf(ParameterType.Activity))

    tested.onBeforeTestExecution()
    tested.onAfterTestExecution()
  }

  @Test
  @DisplayName("Execution: Works on Valid Tested Parameter")
  fun worksWhenUsingValidTestedParameterType() {
    val tested = create(SomeActivity::class.java, listOf(ParameterType.ValidTestedWrapper))

    tested.onBeforeTestExecution()
    tested.onAfterTestExecution()
  }

  @Test
  @DisplayName("Execution: Throws when missing Tested Parameter in manual launch mode")
  fun throwsWhenMissingTestedParameterTypeOnManualLaunch() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterTypes = emptyList(),
        launchActivity = false)

    assertThrows(MissingTestedParameterException::class.java) {
      tested.onBeforeTestExecution()
      tested.onAfterTestExecution()
    }
  }

  @Test
  @DisplayName("Execution: Throws when using Activity Parameter in manual launch mode")
  fun throwsWhenUsingActivityParameterTypeOnManualLaunch() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterTypes = listOf(ParameterType.Activity),
        launchActivity = false)

    assertThrows(MissingTestedParameterException::class.java) {
      tested.onBeforeTestExecution()
      tested.onAfterTestExecution()
    }
  }

  @ParameterizedTest
  @MethodSource("parameterTypes")
  @DisplayName("ParameterType Validation")
  internal fun validatesParameterTypesAsExpected(type: ParameterType, expected: ExpectedResult) {
    val tested = create(SomeActivity::class.java, listOf(type))

    when (expected) {
      is Throws<*> -> {
        // Expect an Exception to be raised
        Assertions.assertThrows(expected.throwableClass) {
          tested.validateParameterOrThrow(type)
        }
      }

      is Bool -> {
        Assertions.assertEquals(expected.value, tested.validateParameterOrThrow(type))
      }
    }
  }

  @Suppress("unused")
  companion object {
    @JvmStatic
    fun parameterTypes(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                ParameterType.Activity,
                Bool(true)),
            Arguments.of(
                ValidTestedWrapper,
                Bool(true)),
            Arguments.of(
                InvalidTestedWrapper(String::class.java),
                Throws(UnexpectedActivityException::class.java)),
            Arguments.of(
                Unknown,
                Bool(false)))
  }

  /* Private */

  /**
   * Creates a new DefaultTested object with the appropriate mocks in place.
   */
  private fun <T : Activity> create(
      activityClass: Class<T>,
      parameterTypes: List<ParameterType>,
      launchActivity: Boolean = true): DefaultTested<T> {
    val tested = DefaultTested(
        activityClass = activityClass,
        targetPackage = "de.mannodermaus.junit5",
        launchActivity = launchActivity,
        parameterTypes = parameterTypes)

    // Configure the Instrumentation mock
    val mockActivity = Mockito.mock(activityClass)
    Mockito.`when`(mockInstrumentation.startActivitySync(any())).thenReturn(mockActivity)
    tested.setInstrumentation(mockInstrumentation)
    return tested
  }
}

/* Helper Types */

/**
 * "Shell Activity" used purely for its class.
 */
private open class SomeActivity : Activity()

/**
 * Abstraction over expected assertion results
 */
internal sealed class ExpectedResult {
  class Bool(val value: Boolean) : ExpectedResult()
  class Throws<T : Throwable>(val throwableClass: Class<T>) : ExpectedResult()
}
