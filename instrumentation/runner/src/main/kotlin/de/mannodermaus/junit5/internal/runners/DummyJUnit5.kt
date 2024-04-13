package de.mannodermaus.junit5.internal.runners

import android.util.Log
import de.mannodermaus.junit5.internal.LOG_TAG
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.Method

/**
 * Fake Runner that marks all JUnit 5 methods as ignored,
 * used for old devices without Java 8 capabilities.
 */
internal class DummyJUnit5(
    private val testClass: Class<*>,
    private val testMethods: Set<Method>,
) : Runner() {

    override fun run(notifier: RunNotifier) {
        Log.w(
            LOG_TAG,
            "JUnit 5 is not supported on this device. All Jupiter tests will be disabled."
        )

        for (testMethod in testMethods) {
            val description = Description.createTestDescription(testClass, testMethod.name)
            notifier.fireTestIgnored(description)
        }
    }

    override fun getDescription(): Description = Description.createSuiteDescription(testClass).also {
        testMethods.forEach { method ->
            it.addChild(Description.createTestDescription(testClass, method.name))
        }
    }
}
