package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [EnabledOnSdkVersionConditionTests].
 * The tests in here are intentionally disabled; the partner class will
 * drive them through reflection in order to assert the behavior of the condition
 */
class EnabledOnSdkVersionIntegrationTests {

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion
  @Test
  fun invalidBecauseNoValueGiven() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(from = 24)
  @Test
  fun enabledBecauseMinApiIsMatched() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(until = 35)
  @Test
  fun enabledBecauseMaxApiIsMatched() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(from = 24, until = 36)
  @Test
  fun enabledBecauseApiIsInValidRange() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(from = 36)
  @Test
  fun disabledBecauseMinApiTooLow() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(until = 27)
  @Test
  fun disabledBecauseMaxApiTooHigh() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(from = 27, until = 29)
  @Test
  fun disabledBecauseApiIsOutsideValidRange() {
  }
}
