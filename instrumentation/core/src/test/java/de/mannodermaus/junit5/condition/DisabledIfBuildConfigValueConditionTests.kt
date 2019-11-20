package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.util.AndroidBuildUtils.withMockedInstrumentation
import de.mannodermaus.junit5.util.RESOURCE_LOCK_INSTRUMENTATION
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.platform.commons.PreconditionViolationException

/**
 * Unit tests for [DisabledIfBuildConfigValueCondition].
 *
 * This works together with [DisabledIfBuildConfigValueIntegrationTests]: The test methods
 * in both classes MUST be named identical.
 */
@ResourceLock(RESOURCE_LOCK_INSTRUMENTATION)
class DisabledIfBuildConfigValueConditionTests : AbstractExecutionConditionTests() {

  override fun getExecutionCondition(): ExecutionCondition = DisabledIfBuildConfigValueCondition()

  override fun getTestClass(): Class<*> = DisabledIfBuildConfigValueIntegrationTests::class.java

  /**
   * @see [DisabledIfBuildConfigValueIntegrationTests.invalidBecauseNameIsEmpty]
   */
  @Test
  fun invalidBecauseNameIsEmpty() {
    withMockedInstrumentation {
      val expected = assertThrows<PreconditionViolationException> {
        evaluateCondition()
      }

      assertThat(expected).hasMessageThat().contains("The 'named' attribute must not be blank in")
    }
  }

  /**
   * @see [DisabledIfBuildConfigValueIntegrationTests.invalidBecauseRegexIsEmpty]
   */
  @Test
  fun invalidBecauseRegexIsEmpty() {
    withMockedInstrumentation {
      val expected = assertThrows<PreconditionViolationException> {
        evaluateCondition()
      }

      assertThat(expected).hasMessageThat().contains("The 'matches' attribute must not be blank in")
    }
  }

  /**
   * @see [DisabledIfBuildConfigValueIntegrationTests.disabledBecauseValueMatchesRegex]
   */
  @Test
  fun disabledBecauseValueMatchesRegex() {
    withMockedInstrumentation {
      evaluateCondition()
      assertDisabled()
      assertReasonEquals("BuildConfig key [DEBUG] with value [true] matches regular expression [\\w{4}]")
    }
  }

  /**
   * @see [DisabledIfBuildConfigValueIntegrationTests.enabledBecauseValueDoesNotMatchRegex]
   */
  @Test
  fun enabledBecauseValueDoesNotMatchRegex() {
    withMockedInstrumentation {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("BuildConfig key [VERSION_NAME] with value [1.0] does not match regular expression [0.1.234]")
    }
  }

  /**
   * @see [DisabledIfBuildConfigValueIntegrationTests.enabledBecauseKeyDoesNotExist]
   */
  @Test
  fun enabledBecauseKeyDoesNotExist() {
    withMockedInstrumentation {
      evaluateCondition()
      assertEnabled()
      assertReasonEquals("BuildConfig key [NOT_EXISTENT_KEY] does not exist")
    }
  }
}
