package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.util.AndroidBuildUtils.withApiLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [EnabledOnApiCondition].
 *
 * This works together with [EnabledOnApiIntegrationTests]: The test methods
 * in both classes MUST be named identical.
 */
class EnabledOnApiConditionTests : AbstractExecutionConditionTests() {

  override fun getExecutionCondition(): ExecutionCondition = EnabledOnApiCondition()

  override fun getTestClass(): Class<*> = EnabledOnApiIntegrationTests::class.java

  /**
   * @see [EnabledOnApiIntegrationTests.invalidBecauseNoValueGiven]
   */
  @Test
  fun invalidBecauseNoValueGiven() {
    val expected = assertThrows<PreconditionViolationException> {
      evaluateCondition()
    }

    assertThat(expected).hasMessageThat().contains("At least one value must be provided in @EnabledOnApi")
  }

  /**
   * @see [EnabledOnApiIntegrationTests.enabledBecauseMinApiIsMatched]
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
   * @see [EnabledOnApiIntegrationTests.enabledBecauseMaxApiIsMatched]
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
   * @see [EnabledOnApiIntegrationTests.enabledBecauseApiIsInValidRange]
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
   * @see [EnabledOnApiIntegrationTests.disabledBecauseMinApiTooLow]
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
   * @see [EnabledOnApiIntegrationTests.disabledBecauseMaxApiTooHigh]
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
   * @see [EnabledOnApiIntegrationTests.disabledBecauseApiIsOutsideValidRange]
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