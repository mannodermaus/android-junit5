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
  @DisabledOnSdkVersion(min = 24)
  @Test
  fun disabledBecauseMinApiIsMatched() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(max = 26)
  @Test
  fun disabledBecauseMaxApiIsMatched() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(min = 24, max = 29)
  @Test
  fun disabledBecauseApiIsInValidRange() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(min = 27)
  @Test
  fun enabledBecauseMinApiLowEnough() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(max = 27)
  @Test
  fun disabledBecauseMaxApiHighEnough() {
  }

  @Disabled("Used by DisabledOnSdkVersionConditionTests only")
  @DisabledOnSdkVersion(min = 27, max = 29)
  @Test
  fun disabledBecauseApiIsInsideValidRange() {
  }
}
