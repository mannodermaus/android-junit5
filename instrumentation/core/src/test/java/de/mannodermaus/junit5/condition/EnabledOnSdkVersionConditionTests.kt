package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.internal.EnabledOnSdkVersionCondition
import de.mannodermaus.junit5.util.AndroidBuildUtils.withApiLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [EnabledOnSdkVersionCondition].
 *
 * This works together with [EnabledOnSdkVersionIntegrationTests]: The test methods
 * in both classes MUST be named identical.
 */
class EnabledOnSdkVersionConditionTests : AbstractExecutionConditionTests() {

  override fun getExecutionCondition(): ExecutionCondition = EnabledOnSdkVersionCondition()

  override fun getTestClass(): Class<*> = EnabledOnSdkVersionIntegrationTests::class.java

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.invalidBecauseNoValueGiven]
   */
  @Test
  fun invalidBecauseNoValueGiven() {
    val expected = assertThrows<PreconditionViolationException> {
      evaluateCondition()
    }

    assertThat(expected).hasMessageThat().contains("At least one value must be provided in @EnabledOnSdkVersion")
  }

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.enabledBecauseMinApiIsMatched]
   */
  @Test
  fun enabledBecauseMinApiIsMatched() {
    withApiLevel(26) {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on API 26")
    }
  }

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.enabledBecauseMaxApiIsMatched]
   */
  @Test
  fun enabledBecauseMaxApiIsMatched() {
    withApiLevel(24) {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on API 24")
    }
  }

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.enabledBecauseApiIsInValidRange]
   */
  @Test
  fun enabledBecauseApiIsInValidRange() {
    withApiLevel(26) {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on API 26")
    }
  }

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.disabledBecauseMinApiTooLow]
   */
  @Test
  fun disabledBecauseMinApiTooLow() {
    withApiLevel(26) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 26")
    }
  }

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.disabledBecauseMaxApiTooHigh]
   */
  @Test
  fun disabledBecauseMaxApiTooHigh() {
    withApiLevel(29) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 29")
    }
  }

  /**
   * @see [EnabledOnSdkVersionIntegrationTests.disabledBecauseApiIsOutsideValidRange]
   */
  @Test
  fun disabledBecauseApiIsOutsideValidRange() {
    withApiLevel(26) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 26")
    }
  }
}
