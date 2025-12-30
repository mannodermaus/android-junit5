package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.internal.EnabledIfBuildConfigValueCondition
import de.mannodermaus.junit5.testutil.AndroidBuildUtils.withMockedInstrumentation
import de.mannodermaus.junit5.util.RESOURCE_LOCK_INSTRUMENTATION
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [EnabledIfBuildConfigValueCondition].
 *
 * This works together with [EnabledIfBuildConfigValueIntegrationTests]: The test methods in both
 * classes MUST be named identical.
 */
@ResourceLock(RESOURCE_LOCK_INSTRUMENTATION)
class EnabledIfBuildConfigValueConditionTests : AbstractExecutionConditionTests() {

    override fun getExecutionCondition(): ExecutionCondition = EnabledIfBuildConfigValueCondition()

    override fun getTestClass(): Class<*> = EnabledIfBuildConfigValueIntegrationTests::class.java

    /** @see [EnabledIfBuildConfigValueIntegrationTests.invalidBecauseNameIsEmpty] */
    @Test
    fun invalidBecauseNameIsEmpty() {
        withMockedInstrumentation {
            val expected = assertThrows<PreconditionViolationException> { evaluateCondition() }

            assertThat(expected)
                .hasMessageThat()
                .contains("The 'named' attribute must not be blank in")
        }
    }

    /** @see [EnabledIfBuildConfigValueIntegrationTests.invalidBecauseRegexIsEmpty] */
    @Test
    fun invalidBecauseRegexIsEmpty() {
        withMockedInstrumentation {
            val expected = assertThrows<PreconditionViolationException> { evaluateCondition() }

            assertThat(expected)
                .hasMessageThat()
                .contains("The 'matches' attribute must not be blank in")
        }
    }

    /** @see [EnabledIfBuildConfigValueIntegrationTests.enabledBecauseValueMatchesRegex] */
    @Test
    fun enabledBecauseValueMatchesRegex() {
        withMockedInstrumentation {
            evaluateCondition()
            assertEnabled()
            assertReasonEquals(
                "BuildConfig key [DEBUG] with value [true] matches regular expression [\\w{4}]"
            )
        }
    }

    /** @see [EnabledIfBuildConfigValueIntegrationTests.disabledBecauseValueDoesNotMatchRegex] */
    @Test
    fun disabledBecauseValueDoesNotMatchRegex() {
        withMockedInstrumentation {
            evaluateCondition()
            assertDisabled()
            assertReasonEquals(
                "BuildConfig key [VERSION_NAME] with value [1.0] does not match regular expression [0.1.234]"
            )
        }
    }

    /** @see [EnabledIfBuildConfigValueIntegrationTests.disabledBecauseKeyDoesNotExist] */
    @Test
    fun disabledBecauseKeyDoesNotExist() {
        withMockedInstrumentation {
            evaluateCondition()
            assertDisabled()
            assertReasonEquals("BuildConfig key [NOT_EXISTENT_KEY] does not exist")
        }
    }
}
