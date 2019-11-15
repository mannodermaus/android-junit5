package org.junit.platform.runner;

import android.annotation.SuppressLint;

import org.junit.platform.launcher.TestIdentifier;

import java.util.Arrays;
import java.util.List;

@SuppressLint("NewApi")
class AndroidJUnit5Utils {

  private static final List<String> DYNAMIC_TEST_PREFIXES = Arrays.asList(
      "[test-template-invocation",
      "[dynamic-test",
      "[dynamic-container",
      "[test-factory",
      "[test-template"
  );

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
  static boolean isDynamicTest(TestIdentifier identifier) {
    String shortId = getShortId(identifier);
    return DYNAMIC_TEST_PREFIXES.stream().anyMatch(shortId::startsWith);
  }
}
