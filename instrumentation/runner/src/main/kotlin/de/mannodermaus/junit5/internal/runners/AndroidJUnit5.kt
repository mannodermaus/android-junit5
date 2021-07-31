package de.mannodermaus.junit5.internal.runners

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.VisibleForTesting
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.LibcoreAccess
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this. This is in Java because we require access to package-private data,
 * and Kotlin is more strict about that: https://youtrack.jetbrains.com/issue/KT-15315
 *
 *
 * Replacement For:
 * AndroidJUnit4
 *
 * @see org.junit.platform.runner.JUnitPlatform
 */
@SuppressLint("NewApi")
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
internal class AndroidJUnit5
@JvmOverloads constructor(
    private val testClass: Class<*>,
    private val runnerParams: AndroidJUnit5RunnerParams = createRunnerParams(testClass)
) : Runner() {

    private val launcher = LauncherFactory.create()
    private val testTree = generateTestTree(runnerParams)

    override fun getDescription() =
        testTree.suiteDescription

    override fun run(notifier: RunNotifier) {
        // Apply all environment variables & system properties to the running process
        registerEnvironmentVariables()
        registerSystemProperties()

        // Finally, launch the test plan on the JUnit Platform
        launcher.execute(testTree.testPlan, AndroidJUnitPlatformRunnerListener(testTree, notifier))
    }

    /* Private */

    private fun registerEnvironmentVariables() {
        runnerParams.environmentVariables.forEach { (key, value) ->
            try {
                LibcoreAccess.setenv(key, value)
            } catch (t: Throwable) {
                Log.w(LOG_TAG, "Error while setting up environment variables.", t)
            }
        }
    }

    private fun registerSystemProperties() {
        runnerParams.systemProperties.forEach { (key, value) ->
            try {
                System.setProperty(key, value)
            } catch (t: Throwable) {
                Log.w(LOG_TAG, "Error while setting up system properties.", t)
            }
        }
    }

    private fun generateTestTree(params: AndroidJUnit5RunnerParams): AndroidJUnitPlatformTestTree {
        val discoveryRequest = params.createDiscoveryRequest()
        val testPlan = launcher.discover(discoveryRequest)
        return AndroidJUnitPlatformTestTree(testPlan, testClass, params.isIsolatedMethodRun())
    }
}
