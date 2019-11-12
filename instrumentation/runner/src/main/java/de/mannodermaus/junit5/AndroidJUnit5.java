package de.mannodermaus.junit5;

import android.app.Instrumentation;
import android.os.Bundle;

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

import java.util.List;

import de.mannodermaus.junit5.discovery.GeneratedFilters;
import de.mannodermaus.junit5.discovery.ParsedSelectors;

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
public final class AndroidJUnit5 extends Runner {

  private final Class<?> testClass;
  private final Launcher launcher = LauncherFactory.create();
  private final AndroidJUnitPlatformTestTree testTree;

  public AndroidJUnit5(Class<?> testClass, AndroidJUnit5RunnerParams params) {
    this.testClass = testClass;
    this.testTree = generateTestTree(params.createDiscoveryRequest());
  }

  public AndroidJUnit5(Class<?> testClass) {
    this(testClass, createRunnerParams(testClass));
  }

  private static AndroidJUnit5RunnerParams createRunnerParams(Class<?> testClass) {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Bundle arguments = InstrumentationRegistry.getArguments();

    // Parse the selectors to use from what's handed to the runner.
    List<DiscoverySelector> selectors = ParsedSelectors.fromBundle(testClass, arguments);

    // The user may apply test filters to their instrumentation tests through the Gradle plugin's DSL,
    // which aren't subject to the filtering imposed through adb.
    // A special resource file may be looked up at runtime, containing
    // the filters to apply by the AndroidJUnit5 runner.
    List<Filter<?>> filters = GeneratedFilters.fromContext(instrumentation.getContext());

    return new AndroidJUnit5RunnerParams(selectors, filters);
  }

  @Override
  public Description getDescription() {
    return testTree.getSuiteDescription();
  }

  @Override
  public void run(RunNotifier notifier) {
    launcher.execute(testTree.getTestPlan(), new AndroidJUnitPlatformRunnerListener(testTree, notifier));
  }

  /* Private */

  private AndroidJUnitPlatformTestTree generateTestTree(LauncherDiscoveryRequest discoveryRequest) {
    TestPlan testPlan = launcher.discover(discoveryRequest);
    return new AndroidJUnitPlatformTestTree(testPlan, testClass);
  }
}
