package de.mannodermaus.junit5.internal.runners

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.VisibleForTesting
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.LibcoreAccess
import de.mannodermaus.junit5.internal.runners.notification.ParallelRunNotifier
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this.
 *
 * @see org.junit.platform.runner.JUnitPlatform
 */
@SuppressLint("NewApi")
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
internal class AndroidJUnit5(
    private val testClass: Class<*>,
    private val runnerParams: AndroidJUnit5RunnerParams = createRunnerParams(testClass),
) : Runner() {

    private val launcher = LauncherFactory.create()
    private val testTree by lazy { generateTestTree(runnerParams) }

    override fun getDescription() =
        testTree.suiteDescription

    override fun run(notifier: RunNotifier) {
        // Apply all environment variables & system properties to the running process
        registerEnvironmentVariables()
        registerSystemProperties()

        // Finally, launch the test plan on the JUnit Platform
        launcher.execute(
            testTree.testPlan,
            AndroidJUnitPlatformRunnerListener(testTree, createNotifier(notifier)),
        )
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

    private fun generateTestTree(params: AndroidJUnit5RunnerParams) =
        AndroidJUnitPlatformTestTree(
            testPlan = launcher.discover(params.createDiscoveryRequest()),
            testClass = testClass,
            isIsolatedMethodRun = params.isIsolatedMethodRun,
            isParallelExecutionEnabled = params.isParallelExecutionEnabled,
        )

    private fun createNotifier(nextNotifier: RunNotifier) =
        if (testTree.isParallelExecutionEnabled) {
            // Wrap the default notifier with a special handler for parallel test execution
            ParallelRunNotifier(nextNotifier)
        } else {
            nextNotifier
        }
}
