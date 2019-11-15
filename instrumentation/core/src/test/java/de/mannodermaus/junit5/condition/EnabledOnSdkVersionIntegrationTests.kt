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
  @EnabledOnSdkVersion(min = 24)
  @Test
  fun enabledBecauseMinApiIsMatched() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(max = 26)
  @Test
  fun enabledBecauseMaxApiIsMatched() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(min = 24, max = 29)
  @Test
  fun enabledBecauseApiIsInValidRange() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(min = 27)
  @Test
  fun disabledBecauseMinApiTooLow() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(max = 27)
  @Test
  fun disabledBecauseMaxApiTooHigh() {
  }

  @Disabled("Used by EnabledOnSdkVersionConditionTests only")
  @EnabledOnSdkVersion(min = 27, max = 29)
  @Test
  fun disabledBecauseApiIsOutsideValidRange() {
  }
}
