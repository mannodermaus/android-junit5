package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.internal.EnabledOnManufacturerCondition
import de.mannodermaus.junit5.testutil.AndroidBuildUtils.withManufacturer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [EnabledOnManufacturerCondition].
 *
 * This works together with [EnabledOnManufacturerIntegrationTests]: The test methods in both
 * classes MUST be named identical.
 */
class EnabledOnManufacturerConditionTests : AbstractExecutionConditionTests() {

    override fun getExecutionCondition(): ExecutionCondition = EnabledOnManufacturerCondition()

    override fun getTestClass(): Class<*> = EnabledOnManufacturerIntegrationTests::class.java

    /** @see [EnabledOnManufacturerIntegrationTests.invalidBecauseArrayIsEmpty] */
    @Test
    fun invalidBecauseArrayIsEmpty() {
        val expected = assertThrows<PreconditionViolationException> { evaluateCondition() }

        assertThat(expected)
            .hasMessageThat()
            .contains("You must declare at least one Manufacturer in @EnabledOnManufacturer")
    }

    /** @see [EnabledOnManufacturerIntegrationTests.enabledBecauseValueMatchesExactly] */
    @Test
    fun enabledBecauseValueMatchesExactly() {
        withManufacturer("Samsung") {
            evaluateCondition()
            assertEnabled()
            assertReasonEquals("Enabled on Manufacturer: Samsung")
        }
    }

    /** @see [EnabledOnManufacturerIntegrationTests.enabledBecauseValueIsAmongTheValues] */
    @Test
    fun enabledBecauseValueIsAmongTheValues() {
        withManufacturer("Huawei") {
            evaluateCondition()
            assertEnabled()
            assertReasonEquals("Enabled on Manufacturer: Huawei")
        }
    }

    /** @see [EnabledOnManufacturerIntegrationTests.enabledBecauseValueMatchesWithOfIgnoreCase] */
    @Test
    fun enabledBecauseValueMatchesWithOfIgnoreCase() {
        withManufacturer("Samsung") {
            evaluateCondition()
            assertEnabled()
            assertReasonEquals("Enabled on Manufacturer: Samsung")
        }
    }

    /**
     * @see [EnabledOnManufacturerIntegrationTests.disabledBecauseValueDoesntMatchDueToIgnoreCase]
     */
    @Test
    fun disabledBecauseValueDoesntMatchDueToIgnoreCase() {
        withManufacturer("Samsung") {
            evaluateCondition()
            assertDisabled()
            assertReasonEquals("Disabled on Manufacturer: Samsung")
        }
    }

    /** @see [EnabledOnManufacturerIntegrationTests.disabledBecauseValueDoesntMatchAnyValue] */
    @Test
    fun disabledBecauseValueDoesntMatchAnyValue() {
        withManufacturer("Google") {
            evaluateCondition()
            assertDisabled()
            assertReasonEquals("Disabled on Manufacturer: Google")
        }
    }
}
