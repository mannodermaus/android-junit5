package de.mannodermaus.junit5.internal.runners

import android.os.Build
import android.util.Log
import de.mannodermaus.junit5.internal.JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.dummy.JupiterTestMethodFinder
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.Method

/**
 * Fake Runner that marks all JUnit Framework methods as ignored,
 * used for old devices without the required Java capabilities.
 */
internal class DummyJUnitFramework(private val testClass: Class<*>) : Runner() {

    private val testMethods: Set<Method> = JupiterTestMethodFinder.find(testClass)

    override fun run(notifier: RunNotifier) {
        Log.w(
            LOG_TAG,
            "JUnit Framework is not supported on this device: " +
                    "API level ${Build.VERSION.SDK_INT} is less than " +
                    "${JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION}, the minimum requirement. " +
                    "All Jupiter tests for ${testClass.name} will be disabled."
        )

        for (testMethod in testMethods) {
            val description = Description.createTestDescription(testClass, testMethod.name)
            notifier.fireTestIgnored(description)
        }
    }

    override fun getDescription(): Description =
        Description.createSuiteDescription(testClass).also {
            testMethods.forEach { method ->
                it.addChild(Description.createTestDescription(testClass, method.name))
            }
        }
}
