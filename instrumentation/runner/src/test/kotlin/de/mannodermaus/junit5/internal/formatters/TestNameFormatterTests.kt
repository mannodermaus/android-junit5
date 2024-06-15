package de.mannodermaus.junit5.internal.formatters

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.HasParameterizedTest
import de.mannodermaus.junit5.HasRepeatedTest
import de.mannodermaus.junit5.HasTest
import de.mannodermaus.junit5.HasTestFactory
import de.mannodermaus.junit5.HasTestTemplate
import de.mannodermaus.junit5.discoverTests
import de.mannodermaus.junit5.internal.extensions.format
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.reflect.KClass

class TestNameFormatterTests {

    @Test
    fun `normal junit5 test`() = runTestWith(HasTest::class) { identifier ->
        assertThat(identifier.format(false)).isEqualTo("method")
        assertThat(identifier.format(true)).isEqualTo("method")
    }

    @Test
    fun `repeated test`() = runTestWith(HasRepeatedTest::class) { identifier ->
        assertThat(identifier.format(false)).isEqualTo("method(RepetitionInfo)")
        assertThat(identifier.format(true)).isEqualTo("method")

        // Inspect individual executions, too
        assertChildren(identifier, expectedCount = 5) { index, child ->
            val number = index + 1
            assertThat(child.format(false)).isEqualTo("repetition $number of 5")
            assertThat(child.format(true)).isEqualTo("method[$number]")
        }
    }

    @Test
    fun `test factory`() = runTestWith(HasTestFactory::class) { identifier ->
        assertThat(identifier.format(false)).isEqualTo("method")
        assertThat(identifier.format(true)).isEqualTo("method")

        // Inspect individual executions, too
        assertChildren(identifier, expectedCount = 2) { index, child ->
            val number = index + 1
            assertThat(child.format(false)).isEqualTo(if (index == 0) "a" else "b")
            assertThat(child.format(true)).isEqualTo("method[$number]")
        }
    }

    @Test
    fun `test template`() = runTestWith(HasTestTemplate::class) { identifier ->
        assertThat(identifier.format(false)).isEqualTo("method(String)")
        assertThat(identifier.format(true)).isEqualTo("method")

        // Inspect individual executions, too
        assertChildren(identifier, expectedCount = 2) { index, child ->
            val number = index + 1
            assertThat(child.format(false)).isEqualTo("param$number")
            assertThat(child.format(true)).isEqualTo("method[$number]")
        }
    }

    @Test
    fun `parameterized test`() = runTestWith(HasParameterizedTest::class) { identifier ->
        assertThat(identifier.format(false)).isEqualTo("method(String)")
        assertThat(identifier.format(true)).isEqualTo("method")

        // Inspect individual executions, too
        assertChildren(identifier, expectedCount = 2) { index, child ->
            val number = index + 1
            assertThat(child.format(false)).isEqualTo("[$number] " + if (index == 0) "a" else "b")
            assertThat(child.format(true)).isEqualTo("method[$number]")
        }
    }

    /* Private */

    private fun runTestWith(cls: KClass<*>, block: TestPlan.(TestIdentifier) -> Unit) {
        // Discover and execute the test plan of the given class
        // (execution is important to resolve any dynamic tests
        // that aren't generated until the test plan actually runs)
        val plan = discoverTests(cls, executeAsWell = true)

        // Validate common behavior of formatter against class names
        val root = plan.roots.first()
        val classIdentifier = plan.getChildren(root).first()
        assertThat(classIdentifier.format(false)).isEqualTo(cls.simpleName)
        assertThat(classIdentifier.format(true)).isEqualTo(cls.simpleName)

        // Delegate to the provided block for the test method of the class
        val methodIdentifier = plan.getChildren(classIdentifier).first()
        plan.block(methodIdentifier)
    }

    private fun TestPlan.assertChildren(
        of: TestIdentifier,
        expectedCount: Int,
        block: (Int, TestIdentifier) -> Unit
    ) {
        with(getChildren(of)) {
            assertThat(size).isEqualTo(expectedCount)
            forEachIndexed(block)
        }
    }
}
