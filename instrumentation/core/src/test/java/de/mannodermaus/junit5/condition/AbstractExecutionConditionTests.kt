package de.mannodermaus.junit5.condition

import com.google.common.truth.Truth.assertThat
import java.lang.reflect.AnnotatedElement
import java.util.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.ReflectionUtils
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

abstract class AbstractExecutionConditionTests {

    private val context = mock<ExtensionContext>()
    private var result: ConditionEvaluationResult? = null

    /* Lifecycle */

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        whenever(context.element).thenReturn(method(testInfo))
    }

    /* Abstract */

    abstract fun getTestClass(): Class<*>

    abstract fun getExecutionCondition(): ExecutionCondition

    /* Protected */

    protected fun evaluateCondition() {
        this.result = getExecutionCondition().evaluateExecutionCondition(context)
    }

    protected fun assertEnabled() {
        assertTrue(!result!!.isDisabled, "Should be enabled")
    }

    protected fun assertDisabled() {
        assertTrue(result!!.isDisabled, "Should be disabled")
    }

    protected fun assertReasonEquals(text: String) {
        assertThat(result!!.reason).hasValue(text)
    }

    /* Private */

    private fun method(testInfo: TestInfo) = method(getTestClass(), testInfo.testMethod.get().name)

    private fun method(clazz: Class<*>, methodName: String): Optional<AnnotatedElement> =
        Optional.of(ReflectionUtils.findMethod(clazz, methodName).get())
}
