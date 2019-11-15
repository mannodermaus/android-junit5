package org.junit.platform.runner;

import org.junit.platform.launcher.TestIdentifier;

class AndroidJUnit5Utils {

  private AndroidJUnit5Utils() {
  }

  /**
   * Format a short ID out of the JUnit Jupiter unique identifier.
   * This is used to detect a parameterized/dynamic test
   *
   * @param identifier Identifier to check
   * @return The shortened ID of the identifier
   */
  private static String getShortId(TestIdentifier identifier) {
    String id = identifier.getUniqueId();
    int lastSlashIndex = id.lastIndexOf('/');
    if (lastSlashIndex > -1 && id.length() >= lastSlashIndex) {
      id = id.substring(lastSlashIndex + 1);
    }
    return id;
  }

  /**
   * Check if the given TestIdentifier describes a "test template invocation",
   * i.e. a dynamic test generated at runtime.
   *
   * @param identifier Identifier to check
   * @return True if the TestIdentifier is a test template invocation, false otherwise
   */
  static boolean isTestTemplateInvocation(TestIdentifier identifier) {
    return getShortId(identifier).startsWith("[test-template-invocation");
  }
}
