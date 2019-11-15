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
import org.junit.runner.manipulation.Filter;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.runner.AndroidJUnit5Utils.isTestTemplateInvocation;

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
  private final Class<?> testClass;

  public AndroidJUnitPlatformTestTree(TestPlan testPlan, Class<?> testClass) {
    this.testPlan = new ModifiedTestPlan(testPlan);
    this.testClass = testClass;
    this.nameExtractor = this::getTestName;
    this.suiteDescription = generateSuiteDescription(testPlan, testClass);
  }

  private String getTestName(TestIdentifier identifier) {
    String baseName = useTechnicalNames(testClass) ? getTechnicalName(identifier) : identifier.getDisplayName();
    if (isTestTemplateInvocation(identifier)) {
      // Fold surrounding test template name into a distinct format
      String reportName = identifier.getLegacyReportingName();

      int bracketIndex = baseName.indexOf("] ");
      if (bracketIndex > -1 && baseName.length() + 2 >= bracketIndex) {
        baseName = baseName.substring(bracketIndex + 2);
      }

      return reportName + ": " + baseName;
    }

    return baseName;
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

  Set<TestIdentifier> getFilteredLeaves(Filter filter) {
    Set<TestIdentifier> identifiers = applyFilterToDescriptions(filter);
    return removeNonLeafIdentifiers(identifiers);
  }

  private Set<TestIdentifier> removeNonLeafIdentifiers(Set<TestIdentifier> identifiers) {
    return identifiers.stream().filter(isALeaf(identifiers)).collect(toSet());
  }

  private Predicate<? super TestIdentifier> isALeaf(Set<TestIdentifier> identifiers) {
    return testIdentifier -> {
      Set<TestIdentifier> descendants = testPlan.getDescendants(testIdentifier);
      return identifiers.stream().noneMatch(descendants::contains);
    };
  }

  private Set<TestIdentifier> applyFilterToDescriptions(Filter filter) {
    // @formatter:off
    return descriptions.entrySet()
        .stream()
        .filter(entry -> filter.shouldRun(entry.getValue()))
        .map(Map.Entry::getKey)
        .collect(toSet());
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

    @Override
    public Optional<TestIdentifier> getParent(TestIdentifier child) {
      // Since parameterized tests are interpreted incorrectly by Android,
      // they access their grandparent identifier, instead of the parent like usual.
      // This causes each invocation to be grouped under the class, rather than next to it
      // using a butchered container name.
      if (isTestTemplateInvocation(child)) {
        return delegate.getParent(child).flatMap(delegate::getParent);
      }

      return delegate.getParent(child);
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
