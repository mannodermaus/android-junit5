package org.junit.platform.runner;

import android.annotation.SuppressLint;

import org.junit.jupiter.api.TestTemplate;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.platform.suite.api.UseTechnicalNames;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toCollection;
import static org.junit.platform.runner.AndroidJUnit5Utils.isDynamicTest;

/**
 * Required, public extension to allow access to package-private TestTree class.
 * Furthermore, manipulate the test tree in a way that will fold {@link TestTemplate} cases
 * into the test report, without having the Android instrumentation mess up their naming.
 */
@SuppressLint("NewApi")
public final class AndroidJUnitPlatformTestTree {

  private final Map<TestIdentifier, Description> descriptions = new HashMap<>();
  private final ModifiedTestPlan testPlan;
  private final Function<TestIdentifier, String> nameExtractor;
  private final Description suiteDescription;
  private final boolean isIsolatedMethodRun;

  public AndroidJUnitPlatformTestTree(TestPlan testPlan, Class<?> testClass, boolean isIsolatedMethodRun) {
    // Must be set first, as other methods in this constructor will access the field already
    this.isIsolatedMethodRun = isIsolatedMethodRun;

    this.testPlan = new ModifiedTestPlan(testPlan);
    this.nameExtractor = this::getTestName;
    this.suiteDescription = generateSuiteDescription(testPlan, testClass);
  }

  String getTestName(TestIdentifier identifier) {
    if (identifier.isContainer()) {
      return getTechnicalName(identifier);
    }

    if (isDynamicTest(identifier)) {
      // Collect all dynamic tests' IDs from this identifier,
      // all the way up to the first non-dynamic test.
      // Collect the name of all these into a list, then finally
      // compose the final name from this list. Note that, because we
      // move upwards the test plan, the elements must be reversed
      // before the final name can be composed.
      List<String> nameComponents = new ArrayList<>();
      TestIdentifier currentNode = identifier;
      do {
        nameComponents.add(formatTestName(currentNode));
        currentNode = testPlan.getRealParent(currentNode).orElse(null);
      } while (currentNode != null && isDynamicTest(currentNode));

      Collections.reverse(nameComponents);
      return String.join(": ", nameComponents);
    }

    return formatTestName(identifier);
  }

  private String formatTestName(TestIdentifier identifier) {
    // During isolated executions, construct a technical version of the test name
    // for backwards compatibility with the JUnit 4-based instrumentation of Android,
    // stripping the brackets and parameters completely.
    // If we didn't, then running them from the IDE doesn't work for @Test methods with parameters
    // (See AndroidX's TestRequestBuilder$MethodFilter for where this is cross-referenced).
    if (isIsolatedMethodRun) {
      String reportName = identifier.getLegacyReportingName();
      int bracketIndex = reportName.indexOf('(');
      if (bracketIndex > -1) {
        return reportName.substring(0, bracketIndex);
      }
    }

    return identifier.getDisplayName().replace("()", "");
  }

  public TestPlan getTestPlan() {
    // Do not expose our custom TestPlan, because JUnit Platform wouldn't like that very much.
    // Only internally, use the wrapped version
    return testPlan.delegate;
  }

  private static boolean useTechnicalNames(Class<?> testClass) {
    return testClass.isAnnotationPresent(UseTechnicalNames.class);
  }

  public Description getSuiteDescription() {
    return this.suiteDescription;
  }

  Description getDescription(TestIdentifier identifier) {
    return this.descriptions.get(identifier);
  }

  private Description generateSuiteDescription(TestPlan testPlan, Class<?> testClass) {
    String displayName = useTechnicalNames(testClass) ? testClass.getName() : getSuiteDisplayName(testClass);
    Description suiteDescription = Description.createSuiteDescription(displayName);
    buildDescriptionTree(suiteDescription, testPlan);
    return suiteDescription;
  }

  private String getSuiteDisplayName(Class<?> testClass) {
    // @formatter:off
    return AnnotationUtils.findAnnotation(testClass, SuiteDisplayName.class)
        .map(SuiteDisplayName::value)
        .filter(StringUtils::isNotBlank)
        .orElse(testClass.getName());
    // @formatter:on
  }

  private void buildDescriptionTree(Description suiteDescription, TestPlan testPlan) {
    testPlan.getRoots().forEach(testIdentifier -> buildDescription(testIdentifier, suiteDescription, testPlan));
  }

  void addDynamicDescription(TestIdentifier newIdentifier, String parentId) {
    Description parent = getDescription(this.testPlan.getTestIdentifier(parentId));
    buildDescription(newIdentifier, parent, this.testPlan);
  }

  private void buildDescription(TestIdentifier identifier, Description parent, TestPlan testPlan) {
    Description newDescription = createJUnit4Description(identifier, testPlan);
    parent.addChild(newDescription);
    this.descriptions.put(identifier, newDescription);
    testPlan.getChildren(identifier).forEach(
        testIdentifier -> buildDescription(testIdentifier, newDescription, testPlan));
  }

  private Description createJUnit4Description(TestIdentifier identifier, TestPlan testPlan) {
    String name = nameExtractor.apply(identifier);
    if (identifier.isTest()) {
      String containerName = testPlan.getParent(identifier).map(nameExtractor).orElse("<unrooted>");
      return Description.createTestDescription(containerName, name, identifier.getUniqueId());
    }
    return Description.createSuiteDescription(name, identifier.getUniqueId());
  }

  private String getTechnicalName(TestIdentifier testIdentifier) {
    Optional<TestSource> optionalSource = testIdentifier.getSource();
    if (optionalSource.isPresent()) {
      TestSource source = optionalSource.get();
      if (source instanceof ClassSource) {
        return ((ClassSource) source).getJavaClass().getName();
      } else if (source instanceof MethodSource) {
        MethodSource methodSource = (MethodSource) source;
        String methodParameterTypes = methodSource.getMethodParameterTypes();
        if (StringUtils.isBlank(methodParameterTypes)) {
          return methodSource.getMethodName();
        }
        return String.format("%s(%s)", methodSource.getMethodName(), methodParameterTypes);
      }
    }

    // Else fall back to display name
    return testIdentifier.getDisplayName();
  }

  Set<TestIdentifier> getTestsInSubtree(TestIdentifier ancestor) {
    // @formatter:off
    return testPlan.getDescendants(ancestor).stream()
        .filter(TestIdentifier::isTest)
        .collect(toCollection(LinkedHashSet::new));
    // @formatter:on
  }

  /**
   * Custom drop-in TestPlan for Android purposes.
   */
  private static final class ModifiedTestPlan extends TestPlan {

    private final TestPlan delegate;

    ModifiedTestPlan(TestPlan delegate) {
      super(delegate.containsTests());
      this.delegate = delegate;
    }

    Optional<TestIdentifier> getRealParent(TestIdentifier child) {
      // Because the overridden "getParent()" from the superclass is modified,
      // expose this additional method to access the actual parent identifier of the given child.
      // This is needed when composing the display name of a dynamic test.
      return delegate.getParent(child);
    }

    @Override
    public Optional<TestIdentifier> getParent(TestIdentifier child) {
      // Since parameterized tests are interpreted incorrectly by Android,
      // they access their grandparent identifier, instead of the parent like usual.
      // This causes each invocation to be grouped under the class, rather than next to it
      // using a butchered container name.
      if (isDynamicTest(child)) {
        return findEligibleParentOfDynamicTest(child);
      }

      return getRealParent(child);
    }

    private Optional<TestIdentifier> findEligibleParentOfDynamicTest(TestIdentifier child) {
      Optional<TestIdentifier> node = delegate.getParent(child);
      while (node.isPresent() && isDynamicTest(node.get())) {
        node = node.flatMap(delegate::getParent);
      }

      return node;
    }

    /* Unchanged */

    @Override
    public void add(TestIdentifier testIdentifier) {
      delegate.add(testIdentifier);
    }

    @Override
    public Set<TestIdentifier> getRoots() {
      return delegate.getRoots();
    }

    @Override
    public Set<TestIdentifier> getChildren(TestIdentifier parent) {
      return delegate.getChildren(parent);
    }

    @Override
    public Set<TestIdentifier> getChildren(String parentId) {
      return delegate.getChildren(parentId);
    }

    @Override
    public TestIdentifier getTestIdentifier(String uniqueId) throws PreconditionViolationException {
      return delegate.getTestIdentifier(uniqueId);
    }

    @Override
    public long countTestIdentifiers(Predicate<? super TestIdentifier> predicate) {
      return delegate.countTestIdentifiers(predicate);
    }

    @Override
    public Set<TestIdentifier> getDescendants(TestIdentifier parent) {
      return delegate.getDescendants(parent);
    }

    @Override
    public boolean containsTests() {
      return delegate.containsTests();
    }
  }
}
