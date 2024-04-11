package de.mannodermaus.junit5

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AndroidJUnit5BuilderTests {

    private val builder = AndroidJUnit5Builder()

    @Test
    fun `no runner is created if class only contains top-level test methods`() {
        // In Kotlin, a 'Kt'-suffixed class of top-level functions cannot be referenced
        // via the ::class syntax, so construct a reference to the class directly
        val cls = Class.forName(javaClass.packageName + ".TestClassesKt")

        // Top-level tests should be discarded, so no runner must be created for this class
        runTest(cls, expectSuccess = false)
    }

    @ValueSource(
        classes = [
            HasTest::class,
            HasRepeatedTest::class,
            HasTestFactory::class,
            HasTestTemplate::class,
            HasParameterizedTest::class,
            HasInnerClassWithTest::class,
            HasTaggedTest::class,
            HasInheritedTestsFromClass::class,
            HasInheritedTestsFromInterface::class,
        ]
    )
    @ParameterizedTest
    fun `runner is created correctly for classes with valid jupiter test methods`(cls: Class<*>) =
        runTest(cls, expectSuccess = true)

    @ValueSource(
        classes = [
            DoesntHaveTestMethods::class,
            HasJUnit4Tests::class,
            kotlin.time.Duration::class,
        ]
    )
    @ParameterizedTest
    fun `no runner is created if class has no jupiter test methods`(cls: Class<*>) =
        runTest(cls, expectSuccess = false)

    /* Private */

    private fun runTest(cls: Class<*>, expectSuccess: Boolean) {
        val runner = builder.runnerForClass(cls)
        if (expectSuccess) {
            assertThat(runner).isNotNull()
        } else {
            assertThat(runner).isNull()
        }
    }
}
