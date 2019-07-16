package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [DisabledOnApiConditionTests].
 * The tests in here are intentionally disabled; the partner class will
 * drive them through reflection in order to assert the behavior of the condition
 */
class DisabledOnApiIntegrationTests {

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi
  @Test
  fun invalidBecauseNoValueGiven() {
  }

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi(min = 24)
  @Test
  fun disabledBecauseMinApiIsMatched() {
  }

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi(max = 26)
  @Test
  fun disabledBecauseMaxApiIsMatched() {
  }

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi(min = 24, max = 29)
  @Test
  fun disabledBecauseApiIsInValidRange() {
  }

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi(min = 27)
  @Test
  fun enabledBecauseMinApiLowEnough() {
  }

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi(max = 27)
  @Test
  fun disabledBecauseMaxApiHighEnough() {
  }

  @Disabled("Used by DisabledOnApiConditionTests only")
  @DisabledOnApi(min = 27, max = 29)
  @Test
  fun disabledBecauseApiIsInsideValidRange() {
  }
}
