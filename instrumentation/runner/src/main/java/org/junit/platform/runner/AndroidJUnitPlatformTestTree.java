package org.junit.platform.runner;

import org.junit.platform.launcher.TestPlan;
import org.junit.runner.Description;

/**
 * Required, public extension to allow access to package-private TestTree class
 */
public final class AndroidJUnitPlatformTestTree extends JUnitPlatformTestTree {
  public AndroidJUnitPlatformTestTree(TestPlan testPlan, Class<?> testClass) {
    super(testPlan, testClass);
  }

  @Override
  public Description getSuiteDescription() {
    return super.getSuiteDescription();
  }
}
