package org.junit.platform.runner;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.Nullable;

import org.junit.platform.launcher.TestIdentifier;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static de.mannodermaus.junit5.internal.ExtensionsKt.LOG_TAG;

@SuppressLint("NewApi")
public final class AndroidJUnit5Utils {

  private static final List<String> DYNAMIC_TEST_PREFIXES = Arrays.asList(
      "[test-template-invocation",
      "[dynamic-test",
      "[dynamic-container",
      "[test-factory",
      "[test-template"
  );

  @Nullable
  private static final Object LIBCORE_OS_OBJECT;
  @Nullable
  private static final Method LIBCORE_OS_SETENV_METHOD;

  static {
    // Reflectively look up "libcore.io.Libcore.os.setenv(String, String, boolean)",
    // because this API is the entry point to provide custom environment variables
    // to the Android runtime. If this doesn't work, the runner will deactivate
    // custom environment variables.
    Class libcoreClass = null;
    try {
      libcoreClass = Class.forName("libcore.io.Libcore");
    } catch (Throwable t) {
      Log.e(LOG_TAG, "FATAL: Cannot initialize custom environment variables", t);
    }

    Object osObject = null;
    if (libcoreClass != null) {
      try {
        osObject = libcoreClass.getField("os").get(null);
      } catch (Throwable t) {
        Log.e(LOG_TAG, "FATAL: Cannot initialize custom environment variables", t);
      }
    }

    Method setenvMethod = null;
    if (osObject != null) {
      try {
        setenvMethod = osObject.getClass().getMethod("setenv", String.class, String.class, boolean.class);
      } catch (Throwable t) {
        Log.e(LOG_TAG, "FATAL: Cannot initialize custom environment variables", t);
      }
    }

    // Store for later lookup through the convenience method
    LIBCORE_OS_OBJECT = osObject;
    LIBCORE_OS_SETENV_METHOD = setenvMethod;
  }

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

  /**
   * Invokes the method "libcore.io.Libcore.os.setenv(String, String)" with the provided key/value pair.
   * This effectively adds a custom environment variable to the running process,
   * allowing instrumentation tests to honor JUnit 5's @EnabledIfEnvironmentVariable and @DisabledIfEnvironmentVariable annotations.
   *
   * @param key   Key of the variable
   * @param value Value of the variable
   * @throws Exception If anything happens during this call
   */
  public static void libcore_os_setenv(String key, String value) throws Exception {
    if (LIBCORE_OS_OBJECT != null && LIBCORE_OS_SETENV_METHOD != null) {
      LIBCORE_OS_SETENV_METHOD.invoke(LIBCORE_OS_OBJECT, key, value, true);
    } else {
      throw new IllegalAccessException("Cannot access Libcore.os (" + LIBCORE_OS_OBJECT + ") or its setenv() method (" + LIBCORE_OS_SETENV_METHOD + ")");
    }
  }
}
