package de.mannodermaus.junit5.test

import android.app.Activity
import android.app.Instrumentation
import de.mannodermaus.junit5.DefaultTested
import de.mannodermaus.junit5.MissingTestedParameterException
import de.mannodermaus.junit5.ParameterType
import de.mannodermaus.junit5.UnexpectedActivityException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

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

  @Test
  @DisplayName("ParameterType Validation: Activity")
  internal fun parameterTypeValidationActivity() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterTypes = listOf(ParameterType.Activity))

    expectThat { tested.validateParameters() }
        .willReturn(true)
        .assert()
  }

  @Test
  @DisplayName("ParameterType Validation: Valid Tested<T>")
  internal fun parameterTypeValidationValidTestedT() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterTypes = listOf(ParameterType.ValidTestedWrapper))

    expectThat { tested.validateParameters() }
        .willReturn(true)
        .assert()
  }

  @Test
  @DisplayName("ParameterType Validation: Invalid Tested<T>")
  internal fun parameterTypeValidationInvalidTestedT() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterTypes = listOf(ParameterType.InvalidTestedWrapper(OtherActivity::class.java)))

    expectThat { tested.validateParameters() }
        .willThrow(UnexpectedActivityException::class.java)
        .withMessage(
            containing("Expected 'de.mannodermaus.junit5.test.SomeActivity'"),
            containing("was 'de.mannodermaus.junit5.test.OtherActivity'"))
        .assert()
  }

  @Test
  @DisplayName("ParameterType Validation: Unknown")
  internal fun parameterTypeValidationUnknown() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterTypes = listOf(ParameterType.Unknown))

    expectThat { tested.validateParameters() }
        .willReturn(false)
        .assert()
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

// "Shell Activity" classes
private open class SomeActivity : Activity()

private open class OtherActivity : Activity()
