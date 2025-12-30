package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [EnabledIfBuildConfigValueConditionTests]. The tests in here are
 * intentionally disabled; the partner class will drive them through reflection in order to assert
 * the behavior of the condition
 */
class EnabledIfBuildConfigValueIntegrationTests {

    @Disabled("Used by EnabledIfBuildConfigValueConditionTests only")
    @EnabledIfBuildConfigValue(named = "", matches = ".*")
    @Test
    fun invalidBecauseNameIsEmpty() {}

    @Disabled("Used by EnabledIfBuildConfigValueConditionTests only")
    @EnabledIfBuildConfigValue(named = "DEBUG", matches = "")
    @Test
    fun invalidBecauseRegexIsEmpty() {}

    @Disabled("Used by EnabledIfBuildConfigValueConditionTests only")
    @EnabledIfBuildConfigValue(named = "DEBUG", matches = "\\w{4}")
    @Test
    fun enabledBecauseValueMatchesRegex() {}

    @Disabled("Used by EnabledIfBuildConfigValueConditionTests only")
    @EnabledIfBuildConfigValue(named = "VERSION_NAME", matches = "0.1.234")
    @Test
    fun disabledBecauseValueDoesNotMatchRegex() {}

    @Disabled("Used by EnabledIfBuildConfigValueConditionTests only")
    @EnabledIfBuildConfigValue(named = "NOT_EXISTENT_KEY", matches = "whatever")
    @Test
    fun disabledBecauseKeyDoesNotExist() {}
}
