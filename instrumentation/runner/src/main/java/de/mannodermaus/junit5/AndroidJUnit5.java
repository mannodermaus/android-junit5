package de.mannodermaus.junit5;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.runner.AndroidJUnitPlatformRunnerListener;
import org.junit.platform.runner.AndroidJUnitPlatformTestTree;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

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
  private final ParsedFilters filters;

  private final Launcher launcher = LauncherFactory.create();
  private final AndroidJUnitPlatformTestTree testTree;

  public AndroidJUnit5(Class<?> testClass, ParsedFilters filters) {
    this.testClass = testClass;
    this.filters = filters;
    this.testTree = generateTestTree(createDiscoveryRequest());
  }

  @Override
  public Description getDescription() {
    return this.testTree.getSuiteDescription();
  }

  @Override
  public void run(RunNotifier notifier) {
    this.launcher.execute(this.testTree.getTestPlan(), new AndroidJUnitPlatformRunnerListener(this.testTree, notifier));
  }

  /* Private */

  private AndroidJUnitPlatformTestTree generateTestTree(LauncherDiscoveryRequest discoveryRequest) {
    TestPlan testPlan = this.launcher.discover(discoveryRequest);
    return new AndroidJUnitPlatformTestTree(testPlan, testClass);
  }

  private LauncherDiscoveryRequest createDiscoveryRequest() {
    // Select the entire class, then apply the configured filters
    return LauncherDiscoveryRequestBuilder.request()
        .selectors(DiscoverySelectors.selectClass(testClass))
        .filters(this.filters.all())
        .build();
  }
}
