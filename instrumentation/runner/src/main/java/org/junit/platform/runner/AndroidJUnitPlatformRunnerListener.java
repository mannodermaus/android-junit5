package org.junit.platform.runner;

import android.annotation.SuppressLint;
import android.util.Log;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static de.mannodermaus.junit5.internal.ExtensionsKt.LOG_TAG;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;

/**
 * Required, public extension to allow access to package-private RunnerListener class
 */
@SuppressLint("NewApi")
public final class AndroidJUnitPlatformRunnerListener implements TestExecutionListener {

  private final AndroidJUnitPlatformTestTree testTree;
  private final RunNotifier notifier;

  public AndroidJUnitPlatformRunnerListener(AndroidJUnitPlatformTestTree testTree, RunNotifier notifier) {
    this.testTree = testTree;
    this.notifier = notifier;
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    Description description = testTree.getDescription(testIdentifier);

    if (testIdentifier.isTest()) {
      notifier.fireTestStarted(description);
    }
  }

  @Override
  public void dynamicTestRegistered(TestIdentifier testIdentifier) {
    testTree.addDynamicDescription(testIdentifier, testIdentifier.getParentId().get());
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    if (testIdentifier.isTest()) {
      fireTestIgnored(testIdentifier, reason);
    } else {
      testTree.getTestsInSubtree(testIdentifier).forEach(identifier -> fireTestIgnored(identifier, reason));
    }
  }

  private void fireTestIgnored(TestIdentifier testIdentifier, String reason) {
    Description description = testTree.getDescription(testIdentifier);
    this.notifier.fireTestIgnored(description);
    Log.w(LOG_TAG, testTree.getTestName(testIdentifier) + " is ignored. " + reason);
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    Description description = testTree.getDescription(testIdentifier);
    TestExecutionResult.Status status = testExecutionResult.getStatus();
    if (status == ABORTED) {
      this.notifier.fireTestAssumptionFailed(toFailure(testExecutionResult, description));
    } else if (status == FAILED) {
      this.notifier.fireTestFailure(toFailure(testExecutionResult, description));
    }
    if (description.isTest()) {
      this.notifier.fireTestFinished(description);
    }
  }

  private Failure toFailure(TestExecutionResult testExecutionResult, Description description) {
    return new Failure(description, testExecutionResult.getThrowable().orElse(null));
  }
}
