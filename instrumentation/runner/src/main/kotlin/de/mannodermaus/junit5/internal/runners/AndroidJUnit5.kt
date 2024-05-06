package de.mannodermaus.junit5.internal.runners

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.VisibleForTesting
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.LibcoreAccess
import de.mannodermaus.junit5.internal.runners.notification.ParallelRunNotifier
import org.junit.platform.engine.discovery.MethodSelector
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
    paramsSupplier: () -> AndroidJUnit5RunnerParams = AndroidJUnit5RunnerParams.Companion::create,
) : Runner() {

    private val launcher = LauncherFactory.create()
    private val testTree by lazy { generateTestTree(paramsSupplier()) }

    override fun getDescription() =
        testTree.suiteDescription

    override fun run(notifier: RunNotifier) {
        // Finally, launch the test plan on the JUnit Platform
        launcher.execute(
            testTree.testPlan,
            AndroidJUnitPlatformRunnerListener(testTree, createNotifier(notifier)),
        )
    }

    /* Private */

    private fun generateTestTree(params: AndroidJUnit5RunnerParams): AndroidJUnitPlatformTestTree {
        val selectors = params.createSelectors(testClass)
        val isIsolatedMethodRun = selectors.size == 1 && selectors.first() is MethodSelector
        val request = params.createDiscoveryRequest(selectors)

        return AndroidJUnitPlatformTestTree(
            testPlan = launcher.discover(request),
            testClass = testClass,
            isIsolatedMethodRun = isIsolatedMethodRun,
            isParallelExecutionEnabled = params.isParallelExecutionEnabled,
        )
    }

    private fun createNotifier(nextNotifier: RunNotifier) =
        if (testTree.isParallelExecutionEnabled) {
            // Wrap the default notifier with a special handler for parallel test execution
            ParallelRunNotifier(nextNotifier)
        } else {
            nextNotifier
        }
}
