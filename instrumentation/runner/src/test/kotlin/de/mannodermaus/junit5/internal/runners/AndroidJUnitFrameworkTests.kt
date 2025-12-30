package de.mannodermaus.junit5.internal.runners

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.testutil.AndroidBuildUtils.withMockedInstrumentation
import de.mannodermaus.junit5.testutil.CollectingRunListener
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.runner.RunWith
import org.junit.runner.notification.RunNotifier
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidJUnitFrameworkTests {

    @org.junit.Test
    fun `successful tests are reported correctly`() {
        val results = runTests()
        val successNames = results.runTestNames

        assertThat(successNames)
            .containsExactly(
                "normal test",
                "testFactory - container - test 1",
                "testFactory - container - test 2",
                "repeatedTest - repetition 1 of 3",
                "repeatedTest - repetition 2 of 3",
                "repeatedTest - repetition 3 of 3",
                "parameterizedTest(String) - [1] hello",
                "parameterizedTest(String) - [2] world",
            )
    }

    @org.junit.Test
    fun `sharding tests are reported correctly`() {
        // Divide the test suite into four shards. Execute a fifth iteration
        // to get a baseline of the non-sharded number of tests for comparisons.
        //
        // Compare to the JUnit 4 version:
        // https://android.googlesource.com/platform/frameworks/testing/+/android-support-test/runner/src/androidTest/java/android/support/test/internal/runner/TestRequestBuilderTest.java#668
        var totalTests = 0
        val allResults = mutableListOf<CollectingRunListener.Results>()

        for (i in 0..4) {
            val results =
                runTests(
                    shardingConfig =
                        if (i < 4) {
                            ShardingConfig(num = 4, index = i)
                        } else {
                            null
                        }
                )

            if (i == 4) {
                // Last execution should execute all tests together
                assertThat(results.runCount).isEqualTo(totalTests)
            } else {
                // Previous executions should only execute a subset of tests
                // (doesn't need to be equally divided, as long as the sum matches)
                totalTests += results.runCount
                allResults += results
            }
        }

        allResults.forEach { results -> assertThat(results.runCount).isLessThan(totalTests) }
    }

    /* Private */

    private data class ShardingConfig(val num: Int, val index: Int)

    private fun runTests(shardingConfig: ShardingConfig? = null): CollectingRunListener.Results {
        val resultRef = AtomicReference<CollectingRunListener.Results>()
        val args = buildArgs(shardingConfig)
        withMockedInstrumentation(args) {
            val runner = AndroidJUnitFramework(Sample_NormalTests::class.java)
            val listener = CollectingRunListener()
            val notifier = RunNotifier().also { it.addListener(listener) }
            runner.run(notifier)
            resultRef.set(listener.getResults())
        }

        return resultRef.get()
    }

    private fun buildArgs(shardingConfig: ShardingConfig?) =
        Bundle().apply {
            if (shardingConfig != null) {
                putString("numShards", shardingConfig.num.toString())
                putString("shardIndex", shardingConfig.index.toString())
            }
        }

    // JUnit Vintage Engine reports an empty event and must be excluded.
    // Because of this, only count tests with an attached method name
    private val CollectingRunListener.Results.runTestNames
        get() = this.successfulTests.mapNotNull { it.methodName }

    private val CollectingRunListener.Results.runCount
        get() = runTestNames.size
}

/* Data */

@Suppress("ClassName")
internal class Sample_NormalTests {
    @Test fun `normal test`() {}

    @TestFactory
    fun testFactory(): DynamicContainer =
        dynamicContainer("container", listOf(dynamicTest("test 1") {}, dynamicTest("test 2") {}))

    @RepeatedTest(3) fun repeatedTest() {}

    @ValueSource(strings = ["hello", "world"])
    @ParameterizedTest
    fun parameterizedTest(param: String) {}
}
