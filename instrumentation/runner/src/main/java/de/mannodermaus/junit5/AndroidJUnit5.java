package de.mannodermaus.junit5;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.runner.AndroidJUnitPlatformRunnerListener;
import org.junit.platform.runner.AndroidJUnitPlatformTestTree;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.mannodermaus.junit5.discovery.GeneratedFilters;
import de.mannodermaus.junit5.discovery.ParsedSelectors;
import de.mannodermaus.junit5.discovery.PropertiesParser;

import static de.mannodermaus.junit5.ExtensionsKt.LOG_TAG;
import static org.junit.platform.runner.AndroidJUnit5Utils.libcore_os_setenv;

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this. This is in Java because we require access to package-private data,
 * and Kotlin is more strict about that: https://youtrack.jetbrains.com/issue/KT-15315
 * <p>
 * Replacement For:
 * AndroidJUnit4
 *
 * @see org.junit.platform.runner.JUnitPlatform
 */
@SuppressLint("NewApi")
public final class AndroidJUnit5 extends Runner {

  private static final String ARG_ENVIRONMENT_VARIABLES = "environmentVariables";
  private static final String ARG_SYSTEM_PROPERTIES = "systemProperties";

  private final Class<?> testClass;
  private final Launcher launcher = LauncherFactory.create();
  private final AndroidJUnitPlatformTestTree testTree;
  private final AndroidJUnit5RunnerParams runnerParams;

  public AndroidJUnit5(Class<?> testClass, AndroidJUnit5RunnerParams params) {
    this.testClass = testClass;
    this.runnerParams = params;
    this.testTree = generateTestTree(params.createDiscoveryRequest());
  }

  public AndroidJUnit5(Class<?> testClass) {
    this(testClass, createRunnerParams(testClass));
  }

  private static AndroidJUnit5RunnerParams createRunnerParams(Class<?> testClass) {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Bundle arguments = InstrumentationRegistry.getArguments();

    // Parse environment variables & pass them to the JVM
    Map<String, String> environmentVariables;
    String environmentVariablesArgument = arguments.getString(ARG_ENVIRONMENT_VARIABLES);
    if (environmentVariablesArgument != null) {
      environmentVariables = PropertiesParser.fromString(environmentVariablesArgument);
    } else {
      environmentVariables = Collections.emptyMap();
    }

    // Parse system properties & pass them to the JVM
    Map<String, String> systemProperties;
    String systemPropertiesArgument = arguments.getString(ARG_SYSTEM_PROPERTIES);
    if (systemPropertiesArgument != null) {
      systemProperties = PropertiesParser.fromString(systemPropertiesArgument);
    } else {
      systemProperties = Collections.emptyMap();
    }

    // Parse the selectors to use from what's handed to the runner.
    List<DiscoverySelector> selectors = ParsedSelectors.fromBundle(testClass, arguments);

    // The user may apply test filters to their instrumentation tests through the Gradle plugin's DSL,
    // which aren't subject to the filtering imposed through adb.
    // A special resource file may be looked up at runtime, containing
    // the filters to apply by the AndroidJUnit5 runner.
    List<Filter<?>> filters = GeneratedFilters.fromContext(instrumentation.getContext());

    return new AndroidJUnit5RunnerParams(selectors, filters, environmentVariables, systemProperties);
  }

  @Override
  public Description getDescription() {
    return testTree.getSuiteDescription();
  }

  @Override
  public void run(RunNotifier notifier) {
    // Apply all environment variables & system properties to the running process
    registerEnvironmentVariables();
    registerSystemProperties();

    // Finally, launch the test plan on the JUnit Platform
    launcher.execute(testTree.getTestPlan(), new AndroidJUnitPlatformRunnerListener(testTree, notifier));
  }

  /* Private */

  private void registerEnvironmentVariables() {
    runnerParams.getEnvironmentVariables().forEach((key, value) -> {
      try {
        libcore_os_setenv(key, value);
      } catch (Throwable t) {
        Log.w(LOG_TAG, "Error while setting up environment variables.", t);
      }
    });
  }

  private void registerSystemProperties() {
    runnerParams.getSystemProperties().forEach((key, value) -> {
      try {
        System.setProperty(key, value);
      } catch (Throwable t) {
        Log.w(LOG_TAG, "Error while setting up system properties.", t);
      }
    });
  }

  private AndroidJUnitPlatformTestTree generateTestTree(LauncherDiscoveryRequest discoveryRequest) {
    TestPlan testPlan = launcher.discover(discoveryRequest);
    return new AndroidJUnitPlatformTestTree(testPlan, testClass);
  }
}
