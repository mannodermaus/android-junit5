package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.util.AndroidBuildUtils.withApiLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [DisabledOnApiCondition].
 *
 * This works together with [DisabledOnApiIntegrationTests]: The test methods
 * in both classes MUST be named identical.
 */
class DisabledOnApiConditionTests : AbstractExecutionConditionTests() {

  override fun getExecutionCondition(): ExecutionCondition = DisabledOnApiCondition()

  override fun getTestClass(): Class<*> = DisabledOnApiIntegrationTests::class.java

  /**
   * @see [DisabledOnApiIntegrationTests.invalidBecauseNoValueGiven]
   */
  @Test
  fun invalidBecauseNoValueGiven() {
    val expected = assertThrows<PreconditionViolationException> {
      evaluateCondition()
    }

    assertThat(expected).hasMessageThat().contains("At least one value must be provided in @DisabledOnApi")
  }

  /**
   * @see [DisabledOnApiIntegrationTests.disabledBecauseMinApiIsMatched]
   */
  @Test
  fun disabledBecauseMinApiIsMatched() {
    withApiLevel(26) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 26")
    }
  }

  /**
   * @see [DisabledOnApiIntegrationTests.disabledBecauseMaxApiIsMatched]
   */
  @Test
  fun disabledBecauseMaxApiIsMatched() {
    withApiLevel(24) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 24")
    }
  }

  /**
   * @see [DisabledOnApiIntegrationTests.disabledBecauseApiIsInValidRange]
   */
  @Test
  fun disabledBecauseApiIsInValidRange() {
    withApiLevel(26) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 26")
    }
  }

  /**
   * @see [DisabledOnApiIntegrationTests.enabledBecauseMinApiLowEnough]
   */
  @Test
  fun enabledBecauseMinApiLowEnough() {
    withApiLevel(26) {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on API 26")
    }
  }

  /**
   * @see [DisabledOnApiIntegrationTests.disabledBecauseMaxApiHighEnough]
   */
  @Test
  fun disabledBecauseMaxApiHighEnough() {
    withApiLevel(29) {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on API 29")
    }
  }

  /**
   * @see [DisabledOnApiIntegrationTests.disabledBecauseApiIsInsideValidRange]
   */
  @Test
  fun disabledBecauseApiIsInsideValidRange() {
    withApiLevel(28) {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on API 28")
    }
  }
}
