package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.util.AndroidBuildUtils.withManufacturer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [DisabledOnManufacturerCondition].
 *
 * This works together with [DisabledOnManufacturerIntegrationTests]: The test methods
 * in both classes MUST be named identical.
 */
class DisabledOnManufacturerConditionTests : AbstractExecutionConditionTests() {

  override fun getExecutionCondition(): ExecutionCondition = DisabledOnManufacturerCondition()

  override fun getTestClass(): Class<*> = DisabledOnManufacturerIntegrationTests::class.java

  /**
   * @see [DisabledOnManufacturerIntegrationTests.invalidBecauseArrayIsEmpty]
   */
  @Test
  fun invalidBecauseArrayIsEmpty() {
    val expected = assertThrows<PreconditionViolationException> {
      evaluateCondition()
    }

    assertThat(expected).hasMessageThat().contains("You must declare at least one Manufacturer in @DisabledOnManufacturer")
  }

  /**
   * @see [DisabledOnManufacturerIntegrationTests.disabledBecauseValueMatchesExactly]
   */
  @Test
  fun disabledBecauseValueMatchesExactly() {
    withManufacturer("Samsung") {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on Manufacturer: Samsung")
    }
  }

  /**
   * @see [DisabledOnManufacturerIntegrationTests.disabledBecauseValueIsAmongTheValues]
   */
  @Test
  fun disabledBecauseValueIsAmongTheValues() {
    withManufacturer("Huawei") {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on Manufacturer: Huawei")
    }
  }

  /**
   * @see [DisabledOnManufacturerIntegrationTests.disabledBecauseValueMatchesWithOfIgnoreCase]
   */
  @Test
  fun disabledBecauseValueMatchesWithOfIgnoreCase() {
    withManufacturer("Samsung") {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("Disabled on Manufacturer: Samsung")
    }
  }

  /**
   * @see [DisabledOnManufacturerIntegrationTests.enabledBecauseValueDoesntMatchDueToIgnoreCase]
   */
  @Test
  fun enabledBecauseValueDoesntMatchDueToIgnoreCase() {
    withManufacturer("Samsung") {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on Manufacturer: Samsung")
    }
  }

  /**
   * @see [DisabledOnManufacturerIntegrationTests.enabledBecauseValueDoesntMatchAnyValue]
   */
  @Test
  fun enabledBecauseValueDoesntMatchAnyValue() {
    withManufacturer("Google") {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("Enabled on Manufacturer: Google")
    }
  }
}
