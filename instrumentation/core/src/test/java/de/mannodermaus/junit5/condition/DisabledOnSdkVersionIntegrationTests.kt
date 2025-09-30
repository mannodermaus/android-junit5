package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [DisabledOnSdkVersionConditionTests].
 * The tests in here are intentionally disabled; the partner class will
 * drive them through reflection in order to assert the behavior of the condition
 */
class DisabledOnSdkVersionIntegrationTests {

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion
  @Test
  fun invalidBecauseNoValueGiven() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(from = 24)
  @Test
  fun disabledBecauseMinApiIsMatched() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(until = 35)
  @Test
  fun disabledBecauseMaxApiIsMatched() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(from = 24, until = 29)
  @Test
  fun disabledBecauseApiIsInValidRange() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(from = 27)
  @Test
  fun enabledBecauseMinApiLowEnough() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(until = 27)
  @Test
  fun disabledBecauseMaxApiHighEnough() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(from = 27, until = 29)
  @Test
  fun disabledBecauseApiIsInsideValidRange() {
  }
}
