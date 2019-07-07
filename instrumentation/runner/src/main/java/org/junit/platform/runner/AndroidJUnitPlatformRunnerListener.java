package org.junit.platform.runner;

import org.junit.runner.notification.RunNotifier;

/**
 * Required, public extension to allow access to package-private RunnerListener class
 */
public final class AndroidJUnitPlatformRunnerListener extends JUnitPlatformRunnerListener {
  public AndroidJUnitPlatformRunnerListener(JUnitPlatformTestTree testTree, RunNotifier notifier) {
    super(testTree, notifier);
  }
}
