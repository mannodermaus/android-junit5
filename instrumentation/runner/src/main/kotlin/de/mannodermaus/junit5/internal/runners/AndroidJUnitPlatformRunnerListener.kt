package de.mannodermaus.junit5.internal.runners

import android.annotation.SuppressLint
import android.util.Log
import de.mannodermaus.junit5.internal.LOG_TAG
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

/**
 * Required, public extension to allow access to package-private RunnerListener class
 */
@SuppressLint("NewApi")
internal class AndroidJUnitPlatformRunnerListener(
    private val testTree: AndroidJUnitPlatformTestTree,
    private val notifier: RunNotifier
) : TestExecutionListener {

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        // No-op, but must be declared to avoid AbstractMethodError
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        notifier.fireTestSuiteStarted(testTree.suiteDescription)
    }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        val description = testTree.getDescription(testIdentifier)
        if (testIdentifier.isTest) {
            notifier.fireTestStarted(description)
        }
    }

    override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
        testTree.addDynamicDescription(testIdentifier, testIdentifier.parentId.get())
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String) {
        if (testIdentifier.isTest) {
            fireTestIgnored(testIdentifier, reason)
        } else {
            testTree.getTestsInSubtree(testIdentifier)
                .forEach { identifier ->
                    fireTestIgnored(identifier, reason)
                }
        }
    }

    private fun fireTestIgnored(testIdentifier: TestIdentifier, reason: String) {
        val description = testTree.getDescription(testIdentifier)
        notifier.fireTestIgnored(description)
        Log.w(LOG_TAG, testTree.getTestName(testIdentifier) + " is ignored. " + reason)
    }

    override fun executionFinished(
        testIdentifier: TestIdentifier,
        testExecutionResult: TestExecutionResult
    ) {
        val description = testTree.getDescription(testIdentifier)
        val status = testExecutionResult.status
        if (status == TestExecutionResult.Status.ABORTED) {
            notifier.fireTestAssumptionFailed(toFailure(testExecutionResult, description))
        } else if (status == TestExecutionResult.Status.FAILED) {
            notifier.fireTestFailure(toFailure(testExecutionResult, description))
        } else if (testIdentifier.isTest) {
            notifier.fireTestFinished(description)
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        notifier.fireTestSuiteFinished(testTree.suiteDescription)
    }

    private fun toFailure(
        testExecutionResult: TestExecutionResult,
        description: Description
    ): Failure = Failure(description, testExecutionResult.throwable.orElse(null))
}