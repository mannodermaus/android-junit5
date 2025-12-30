package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [EnabledOnManufacturerConditionTests]. The tests in here are intentionally
 * disabled; the partner class will drive them through reflection in order to assert the behavior of
 * the condition
 */
class EnabledOnManufacturerIntegrationTests {

    @Disabled("Used by EnabledOnManufacturerConditionTests only")
    @EnabledOnManufacturer([])
    @Test
    fun invalidBecauseArrayIsEmpty() {}

    @Disabled("Used by EnabledOnManufacturerConditionTests only")
    @EnabledOnManufacturer(["Samsung"])
    @Test
    fun enabledBecauseValueMatchesExactly() {}

    @Disabled("Used by EnabledOnManufacturerConditionTests only")
    @EnabledOnManufacturer(["Samsung", "Huawei"])
    @Test
    fun enabledBecauseValueIsAmongTheValues() {}

    @Disabled("Used by EnabledOnManufacturerConditionTests only")
    @EnabledOnManufacturer(["sAmSuNg"])
    @Test
    fun enabledBecauseValueMatchesWithOfIgnoreCase() {}

    @Disabled("Used by EnabledOnManufacturerConditionTests only")
    @EnabledOnManufacturer(["sAmSuNg"], ignoreCase = false)
    @Test
    fun disabledBecauseValueDoesntMatchDueToIgnoreCase() {}

    @Disabled("Used by EnabledOnManufacturerConditionTests only")
    @EnabledOnManufacturer(["Samsung", "Huawei"])
    @Test
    fun disabledBecauseValueDoesntMatchAnyValue() {}
}
