package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Companion class for [DisabledOnManufacturerConditionTests]. The tests in here are intentionally
 * disabled; the partner class will drive them through reflection in order to assert the behavior of
 * the condition
 */
class DisabledOnManufacturerIntegrationTests {

    @Disabled("Used by DisabledOnManufacturerConditionTests only")
    @DisabledOnManufacturer([])
    @Test
    fun invalidBecauseArrayIsEmpty() {}

    @Disabled("Used by DisabledOnManufacturerConditionTests only")
    @DisabledOnManufacturer(["Samsung"])
    @Test
    fun disabledBecauseValueMatchesExactly() {}

    @Disabled("Used by DisabledOnManufacturerConditionTests only")
    @DisabledOnManufacturer(["Samsung", "Huawei"])
    @Test
    fun disabledBecauseValueIsAmongTheValues() {}

    @Disabled("Used by DisabledOnManufacturerConditionTests only")
    @DisabledOnManufacturer(["sAmSuNg"])
    @Test
    fun disabledBecauseValueMatchesWithOfIgnoreCase() {}

    @Disabled("Used by DisabledOnManufacturerConditionTests only")
    @DisabledOnManufacturer(["sAmSuNg"], ignoreCase = false)
    @Test
    fun enabledBecauseValueDoesntMatchDueToIgnoreCase() {}

    @Disabled("Used by DisabledOnManufacturerConditionTests only")
    @DisabledOnManufacturer(["Samsung", "Huawei"])
    @Test
    fun enabledBecauseValueDoesntMatchAnyValue() {}
}
