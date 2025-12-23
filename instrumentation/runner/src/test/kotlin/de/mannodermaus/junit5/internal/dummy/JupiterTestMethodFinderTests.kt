@file:Suppress("unused")

package de.mannodermaus.junit5.internal.dummy

import androidx.annotation.CheckResult
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import de.mannodermaus.junit5.DoesntHaveTestMethods
import de.mannodermaus.junit5.HasInheritedTestsFromClass
import de.mannodermaus.junit5.HasInheritedTestsFromInterface
import de.mannodermaus.junit5.HasInnerClassWithTest
import de.mannodermaus.junit5.HasMultipleInheritancesAndOverrides
import de.mannodermaus.junit5.HasParameterizedTest
import de.mannodermaus.junit5.HasRepeatedTest
import de.mannodermaus.junit5.HasTaggedTest
import de.mannodermaus.junit5.HasTest
import de.mannodermaus.junit5.HasTestFactory
import de.mannodermaus.junit5.HasTestTemplate
import de.mannodermaus.junit5.internal.runners.AndroidJUnitFramework
import de.mannodermaus.junit5.internal.runners.JUnitFrameworkRunnerParams
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.platform.engine.Filter
import org.junit.platform.launcher.TagFilter
import org.junit.runner.Description
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

class JupiterTestMethodFinderTests {
    // Each element is a Pair of 'test class' and 'number of expected tests'
    private val classes = listOf(
        DoesntHaveTestMethods::class.java to 0,
        HasTaggedTest::class.java to 1,
        HasTest::class.java to 1,
        HasRepeatedTest::class.java to 5,
        HasTestFactory::class.java to 2,
        HasTestTemplate::class.java to 2,
        HasParameterizedTest::class.java to 2,
        HasInnerClassWithTest::class.java to 1,
        HasInheritedTestsFromClass::class.java to 2,
        HasInheritedTestsFromInterface::class.java to 1,
        HasMultipleInheritancesAndOverrides::class.java to 3,
    )

    private val allFinders = setOf(JupiterTestMethodFinder)

    @TestFactory
    fun `check number of jupiter test methods`() = testForEachFinder { finder ->
        classes.map { (cls, testCount) ->
            dynamicTest("expect $testCount tests for ${cls.simpleName}") {
                val methods = finder.find(cls)

                if (testCount == 0) {
                    assertThat(methods).isEmpty()
                } else {
                    assertThat(methods).isNotEmpty()

                    val result = runJUnit5(cls)
                    assertWithMessage("Executed ${result.count()} instead of $testCount tests on class '${cls.simpleName}': '${result.methodNames()}'")
                        .that(result.count())
                        .isEqualTo(testCount)
                }
            }
        }
    }

    @Test
    fun `check that tag filter works`() {
        val result = runJUnit5(
            cls = HasTaggedTest::class.java,
            filter = TagFilter.excludeTags("slow"),
        )

        assertWithMessage("Executed ${result.count()} instead of 0 tests: '${result.methodNames()}'")
            .that(result.count())
            .isEqualTo(0)
    }

    /* Private */

    private fun testForEachFinder(block: (JupiterTestMethodFinder) -> List<DynamicNode>) =
        allFinders.map { finder ->
            dynamicContainer(
                "using ${finder.javaClass.simpleName}",
                block(finder),
            )
        }

    @CheckResult
    private fun runJUnit5(
        cls: Class<*>,
        filter: Filter<*>? = null,
    ): CountingRunListener {
        // Verify number of executed test cases as well
        val notifier = RunNotifier()
        val listener = CountingRunListener()
        notifier.addListener(listener)

        val params = JUnitFrameworkRunnerParams(filters = listOfNotNull(filter))
        AndroidJUnitFramework(cls) { params }.run(notifier)

        return listener
    }

    private class CountingRunListener : RunListener() {
        private val methodNames = mutableListOf<String>()

        override fun testFinished(description: Description) {
            // Only count actual method executions
            // (this method is also called for the class itself)
            description.methodName?.let { methodNames += it }
        }

        fun count() = this.methodNames.size

        fun methodNames() = methodNames.toList()
    }
}
