package de.mannodermaus.junit5.internal.runners

import android.os.Build
import android.util.Log
import de.mannodermaus.junit5.internal.ConfigurationParameters
import de.mannodermaus.junit5.internal.JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.dummy.JupiterTestMethodFinder
import org.junit.platform.commons.JUnitException
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

/**
 * Fake Runner that marks all JUnit Framework methods as ignored, used for old devices without the
 * required Java capabilities.
 */
internal class DummyJUnitFramework(
    private val testClass: Class<*>,
    params: JUnitFrameworkRunnerParams,
) : Runner() {

    private val testMethods = JupiterTestMethodFinder.find(testClass)
    private val behaviorForUnsupportedDevices = params.behaviorForUnsupportedDevices

    override fun run(notifier: RunNotifier) {
        when (behaviorForUnsupportedDevices) {
            "skip" -> skipTests(notifier)
            "fail" -> failExecution(unsupportedDeviceMessage)
            else -> {
                Log.w(
                    LOG_TAG,
                    "Unknown value found for configuration parameter " +
                        "'${ConfigurationParameters.BEHAVIOR_FOR_UNSUPPORTED_DEVICES}': " +
                        "$behaviorForUnsupportedDevices. Apply default behavior " +
                        "and skip tests for this class.",
                )
                skipTests(notifier)
            }
        }
    }

    override fun getDescription(): Description =
        Description.createSuiteDescription(testClass).also {
            testMethods.forEach { method ->
                it.addChild(Description.createTestDescription(testClass, method.name))
            }
        }

    private val unsupportedDeviceMessage by lazy {
        "JUnit Framework is not supported on this device: " +
            "API level ${Build.VERSION.SDK_INT} is less than " +
            "${JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION}, the minimum requirement. " +
            "All Jupiter tests for ${testClass.name} will be disabled."
    }

    private fun skipTests(notifier: RunNotifier) {
        Log.w(LOG_TAG, unsupportedDeviceMessage)

        for (testMethod in testMethods) {
            val description = Description.createTestDescription(testClass, testMethod.name)
            notifier.fireTestIgnored(description)
        }
    }

    private fun failExecution(message: String): Nothing {
        throw JUnitException(message)
    }
}
