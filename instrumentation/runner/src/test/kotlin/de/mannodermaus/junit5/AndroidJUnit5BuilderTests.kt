package de.mannodermaus.junit5

import android.os.Build
import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.testutil.AndroidBuildUtils.withApiLevel
import de.mannodermaus.junit5.testutil.AndroidBuildUtils.withMockedInstrumentation
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class AndroidJUnit5BuilderTests {

    private val builder = AndroidJUnit5Builder()

    @TestFactory
    fun `no runner is created if class only contains top-level test methods`() = runTest(
        expectSuccess = false,
        // In Kotlin, a 'Kt'-suffixed class of top-level functions cannot be referenced
        // via the ::class syntax, so construct a reference to the class directly
        Class.forName(javaClass.packageName + ".TestClassesKt")
    )

    @TestFactory
    fun `runner is created correctly for classes with valid jupiter test methods`() = runTest(
        expectSuccess = true,
        HasTest::class.java,
        HasRepeatedTest::class.java,
        HasTestFactory::class.java,
        HasTestTemplate::class.java,
        HasParameterizedTest::class.java,
        HasInnerClassWithTest::class.java,
        HasTaggedTest::class.java,
        HasInheritedTestsFromClass::class.java,
        HasInheritedTestsFromInterface::class.java,
    )

    @TestFactory
    fun `no runner is created if class has no jupiter test methods`() = runTest(
        expectSuccess = false,
        DoesntHaveTestMethods::class.java,
        HasJUnit4Tests::class.java,
        kotlin.time.Duration::class.java,
    )

    /* Private */

    private fun runTest(expectSuccess: Boolean, vararg classes: Class<*>): List<DynamicNode> {
        // Generate a test container for each given class,
        // then create two sub-variants for testing both DummyJUnit5 and AndroidJUnit5
        return classes.map { cls ->
            dynamicContainer(
                /* displayName = */ cls.name,
                /* dynamicNodes = */ setOf(Build.VERSION_CODES.M, Build.VERSION_CODES.TIRAMISU).map { apiLevel ->
                    dynamicTest("API Level $apiLevel") {
                        withMockedInstrumentation {
                            withApiLevel(apiLevel) {
                                val runner = builder.runnerForClass(cls)
                                if (expectSuccess) {
                                    assertThat(runner).isNotNull()
                                } else {
                                    assertThat(runner).isNull()
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
