package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [EnabledOnApiConditionTests].
 * The tests in here are intentionally disabled; the partner class will
 * drive them through reflection in order to assert the behavior of the condition
 */
class EnabledOnApiIntegrationTests {

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi
  @Test
  fun invalidBecauseNoValueGiven() {
  }

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi(min = 24)
  @Test
  fun enabledBecauseMinApiIsMatched() {
  }

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi(max = 26)
  @Test
  fun enabledBecauseMaxApiIsMatched() {
  }

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi(min = 24, max = 29)
  @Test
  fun enabledBecauseApiIsInValidRange() {
  }

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi(min = 27)
  @Test
  fun disabledBecauseMinApiTooLow() {
  }

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi(max = 27)
  @Test
  fun disabledBecauseMaxApiTooHigh() {
  }

  @Disabled("Used by EnabledOnApiConditionTests only")
  @EnabledOnApi(min = 27, max = 29)
  @Test
  fun disabledBecauseApiIsOutsideValidRange() {
  }
}
