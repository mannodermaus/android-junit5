package de.mannodermaus.junit5.internal.runners

import android.os.Build
import android.util.Log
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.dummy.JupiterTestMethodFinder
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.Method

/**
 * Fake Runner that marks all JUnit Framework methods as ignored,
 * used for old devices without Java 17 capabilities.
 */
internal class DummyJUnitFramework(private val testClass: Class<*>) : Runner() {

    private val testMethods: Set<Method> = JupiterTestMethodFinder.find(testClass)

    override fun run(notifier: RunNotifier) {
        Log.w(
            LOG_TAG,
            buildString {
                append("JUnit Framework is not supported on this device: ")
                append("API level ${Build.VERSION.SDK_INT} is less than 35, ")
                append("the minimum requirement. ")
                append("All Jupiter tests for ${testClass.name} will be disabled.")

                // Add a potential recourse for API levels >= 26, but <= 35
                if (Build.VERSION.SDK_INT >= 26) {
                    append(" You could downgrade to a previous version of the JUnit Framework ")
                    append("(1.14.0.0) in order to use instrumentation tests for this device, ")
                    append("as it meets the minimum API level requirement of that version.")
                }
            }
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
