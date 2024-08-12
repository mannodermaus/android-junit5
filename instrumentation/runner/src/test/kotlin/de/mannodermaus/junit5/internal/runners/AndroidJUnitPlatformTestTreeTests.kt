package de.mannodermaus.junit5.internal.runners

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.HasParameterizedTest
import de.mannodermaus.junit5.HasRepeatedTest
import de.mannodermaus.junit5.HasTest
import de.mannodermaus.junit5.HasTestFactory
import de.mannodermaus.junit5.HasTestTemplate
import de.mannodermaus.junit5.discoverTests
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.reflect.KClass

class AndroidJUnitPlatformTestTreeTests {
    @CsvSource(
        "false, method",
        "true, method",
    )
    @ParameterizedTest
    fun test(isolated: Boolean, expected: String) =
        runTestWith(HasTest::class, isolated) { identifier ->
            val description = getDescription(identifier)
            assertThat(description.methodName).isEqualTo(expected)
        }

    @CsvSource(
        "false, method[RepetitionInfo] - repetition %d of 5",
        "true, method[%d]",
    )
    @ParameterizedTest
    fun `repeated test`(isolated: Boolean, expected: String) =
        runTestWith(HasRepeatedTest::class, isolated) { identifier ->
            assertChildren(identifier, expectedCount = 5) { index, child ->
                val num = index + 1
                val childDescription = getDescription(child)
                assertThat(childDescription.methodName).isEqualTo(expected.format(num))
            }
        }

    @CsvSource(
        "false, method - %s",
        "true, method[%d]",
    )
    @ParameterizedTest
    fun `test factory`(isolated: Boolean, expected: String) =
        runTestWith(HasTestFactory::class, isolated) { identifier ->
            val childMethodNames = listOf("a", "b")

            assertChildren(identifier, expectedCount = 2) { index, child ->
                val num = index + 1
                val childDescription = getDescription(child)

                if (isolated) {
                    assertThat(childDescription.methodName).isEqualTo(expected.format(num))
                } else {
                    assertThat(childDescription.methodName).isEqualTo(expected.format(childMethodNames[index]))
                }
            }
        }

    @CsvSource(
        "false, method[String] - %s",
        "true, method[%d]",
    )
    @ParameterizedTest
    fun `test template`(isolated: Boolean, expected: String) =
        runTestWith(HasTestTemplate::class, isolated) { identifier ->
            val childMethodNames = listOf("param1", "param2")

            assertChildren(identifier, expectedCount = 2) { index, child ->
                val num = index + 1
                val childDescription = getDescription(child)

                if (isolated) {
                    assertThat(childDescription.methodName).isEqualTo(expected.format(num))
                } else {
                    assertThat(childDescription.methodName).isEqualTo(expected.format(childMethodNames[index]))
                }
            }
        }

    @CsvSource(
        "false, method[String] - [%d] %s",
        "true, method[%d]",
    )
    @ParameterizedTest
    fun `parameterized test`(isolated: Boolean, expected: String) =
        runTestWith(HasParameterizedTest::class, isolated) { identifier ->
            val childMethodNames = listOf("a", "b")

            assertChildren(identifier, expectedCount = 2) { index, child ->
                val num = index + 1
                val childDescription = getDescription(child)

                if (isolated) {
                    assertThat(childDescription.methodName).isEqualTo(expected.format(num))
                } else {
                    assertThat(childDescription.methodName).isEqualTo(expected.format(num, childMethodNames[index]))
                }
            }
        }

    /* Private */

    private fun runTestWith(
        cls: KClass<*>,
        isIsolatedMethodRun: Boolean = false,
        block: AndroidJUnitPlatformTestTree.(TestIdentifier) -> Unit,
    ) {
        // Prepare a test plan to launch
        val launcher = LauncherFactory.create()
        val plan = discoverTests(cls, launcher, executeAsWell = false)
        val tree = AndroidJUnitPlatformTestTree(
            testPlan = plan,
            testClass = cls.java,
            needLegacyFormat = isIsolatedMethodRun,
            isParallelExecutionEnabled = false,
        )

        // Execute the test plan, adding dynamic tests with the tree
        // as they are registered during execution
        launcher.execute(plan, object : TestExecutionListener {
            override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
                tree.addDynamicDescription(testIdentifier, testIdentifier.parentId.get())
            }
        })

        // For concrete assertions, delegate to the given block
        val root = plan.roots.first()
        val classIdentifier = plan.getChildren(root).first()
        val methodIdentifier = plan.getChildren(classIdentifier).first()
        tree.block(methodIdentifier)
    }

    private fun AndroidJUnitPlatformTestTree.assertChildren(
        identifier: TestIdentifier,
        expectedCount: Int,
        block: (Int, TestIdentifier) -> Unit
    ) {
        with(getChildren(identifier)) {
            assertThat(size).isEqualTo(expectedCount)
            forEachIndexed(block)
        }
    }
}
