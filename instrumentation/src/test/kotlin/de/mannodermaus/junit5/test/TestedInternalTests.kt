package de.mannodermaus.junit5.test

import android.app.Activity
import android.app.Instrumentation
import de.mannodermaus.junit5.ActivityTestExtension
import de.mannodermaus.junit5.MissingTestedParameterException
import de.mannodermaus.junit5.ParameterType
import de.mannodermaus.junit5.TestedInternal
import de.mannodermaus.junit5.UnexpectedActivityException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

class TestedInternalTests {

  private lateinit var mockInstrumentation: Instrumentation
  private lateinit var extension: ActivityTestExtension

  @BeforeEach
  fun beforeEach() {
    this.mockInstrumentation = Mockito.mock(Instrumentation::class.java)
    this.extension = ActivityTestExtension()
  }

  @Test
  @DisplayName("Execution: Works on Activity Parameter")
  fun worksWhenUsingActivityParameterType() {
    val tested = create(SomeActivity::class.java, ParameterType.Activity)
    tested.execute()
  }

  @Test
  @DisplayName("Execution: Works on Valid Tested Parameter")
  fun worksWhenUsingValidTestedParameterType() {
    val tested = create(SomeActivity::class.java, ParameterType.ValidTestedWrapper)
    tested.execute()
  }

  @Test
  @DisplayName("Execution: Throws when missing Tested Parameter in manual launch mode")
  fun throwsWhenMissingTestedParameterTypeOnManualLaunch() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        launchActivity = false)

    assertThrows(MissingTestedParameterException::class.java) {
      tested.execute()
    }
  }

  @Test
  @DisplayName("Execution: Throws when using Activity Parameter in manual launch mode")
  fun throwsWhenUsingActivityParameterTypeOnManualLaunch() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterType = ParameterType.Activity,
        launchActivity = false)

    assertThrows(MissingTestedParameterException::class.java) {
      tested.execute()
    }
  }

  @Test
  @DisplayName("ParameterType Validation: Activity")
  internal fun parameterTypeValidationActivity() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterType = ParameterType.Activity)

    expectThat { tested.validateParameters() }
        .willReturn(true)
        .assert()
  }

  @Test
  @DisplayName("ParameterType Validation: Valid Tested<T>")
  internal fun parameterTypeValidationValidTestedT() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterType = ParameterType.ValidTestedWrapper)

    expectThat { tested.validateParameters() }
        .willReturn(true)
        .assert()
  }

  @Test
  @DisplayName("ParameterType Validation: Invalid Tested<T>")
  internal fun parameterTypeValidationInvalidTestedT() {
    val tested = create(
        activityClass = SomeActivity::class.java,
        parameterType = ParameterType.InvalidTestedWrapper(OtherActivity::class.java))

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
        parameterType = ParameterType.Unknown)

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
      parameterType: ParameterType? = null,
      launchActivity: Boolean = true): TestedInternal<T> {
    val types = if (parameterType != null) listOf(parameterType) else emptyList()

    val tested = extension.delegateFactory.create(
        activityClass = activityClass,
        targetPackage = "de.mannodermaus.junit5",
        launchActivity = launchActivity,
        parameterTypes = types,
        launchFlags = 0,
        initialTouchMode = true)

    // Configure the Instrumentation mock
    val mockActivity = Mockito.mock(activityClass)
    Mockito.`when`(mockInstrumentation.startActivitySync(any())).thenReturn(mockActivity)
    tested.setInstrumentation(mockInstrumentation)
    return tested
  }

  private fun <T : Activity> TestedInternal<T>.execute() {
    onBeforeTestExecution()
    onAfterTestExecution()
  }
}

// "Shell Activity" classes, used purely as markers for validation
private open class SomeActivity : Activity()
private open class OtherActivity : Activity()
