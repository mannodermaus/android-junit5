package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [DisabledIfBuildConfigValueConditionTests].
 * The tests in here are intentionally disabled; the partner class will
 * drive them through reflection in order to assert the behavior of the condition
 */
class DisabledIfBuildConfigValueIntegrationTests {

  @Disabled("Used by DisabledIfBuildConfigValueConditionTests only")
  @DisabledIfBuildConfigValue(named = "", matches = ".*")
  @Test
  fun invalidBecauseNameIsEmpty() {
  }

  @Disabled("Used by DisabledIfBuildConfigValueConditionTests only")
  @DisabledIfBuildConfigValue(named = "DEBUG", matches = "")
  @Test
  fun invalidBecauseRegexIsEmpty() {
  }

  @Disabled("Used by DisabledIfBuildConfigValueConditionTests only")
  @DisabledIfBuildConfigValue(named = "DEBUG", matches = "\\w{4}")
  @Test
  fun disabledBecauseValueMatchesRegex() {
  }

  @Disabled("Used by DisabledIfBuildConfigValueConditionTests only")
  @DisabledIfBuildConfigValue(named = "VERSION_NAME", matches = "0.1.234")
  @Test
  fun enabledBecauseValueDoesNotMatchRegex() {
  }

  @Disabled("Used by DisabledIfBuildConfigValueConditionTests only")
  @DisabledIfBuildConfigValue(named = "NOT_EXISTENT_KEY", matches = "whatever")
  @Test
  fun enabledBecauseKeyDoesNotExist() {
  }
}
