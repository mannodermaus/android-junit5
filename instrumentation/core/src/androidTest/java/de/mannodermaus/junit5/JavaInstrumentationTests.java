package de.mannodermaus.junit5;

import androidx.test.core.app.ActivityScenario;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import de.mannodermaus.junit5.condition.DisabledOnManufacturer;
import de.mannodermaus.junit5.condition.DisabledOnSdkVersion;
import de.mannodermaus.junit5.condition.EnabledOnManufacturer;
import de.mannodermaus.junit5.condition.EnabledOnSdkVersion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unused")
class JavaInstrumentationTests {

  @RegisterExtension
  final ActivityScenarioExtension<TestActivity> scenarioExtension = ActivityScenarioExtension.launch(TestActivity.class);

  @Test
  void testUsingGetScenario() {
    ActivityScenario<TestActivity> scenario = scenarioExtension.getScenario();
    onView(withText("TestActivity")).check(matches(isDisplayed()));
    scenario.onActivity(it -> it.changeText("New Text"));
    onView(withText("New Text")).check(matches(isDisplayed()));
  }

  @Test
  void testUsingGetScenario(ActivityScenario<TestActivity> scenario) {
    // Intentionally overloaded
  }

  @Test
  void testUsingMethodParameter(ActivityScenario<TestActivity> scenario) {
    onView(withText("TestActivity")).check(matches(isDisplayed()));
    scenario.onActivity(it -> it.changeText("New Text"));
    onView(withText("New Text")).check(matches(isDisplayed()));
  }

  @Disabled
  @Test
  void testDisabled() {
  }

  @TestFactory
  List<DynamicNode> testFactory() {
    return Arrays.asList(
        DynamicContainer.dynamicContainer("Container 1",
            Arrays.asList(
                DynamicTest.dynamicTest("C1 Test1", () -> {

                })
            )
        ),
        DynamicTest.dynamicTest("Test 2", () -> {

        })
    );
  }

  @RepeatedTest(2)
  void repeatedTest() {

  }

  @ParameterizedTest
  @ValueSource(strings = {"ABC", "", "lol"})
  void testParameterized(String parameter) {
    assertNotNull(parameter);
  }

  private final List<String> fruits = Arrays.asList("apple", "banana");

  @TestTemplate
  @ExtendWith(CustomTestProvider.class)
  void testTemplate(String fruit) {
    if (!fruits.contains(fruit)) {
      fail();
    }
  }


  @EnabledOnSdkVersion(from = 28)
  @Test
  void testOnPieAndNewer() {
  }

  @DisabledOnSdkVersion(until = 28)
  @Test
  void testNotOnPieOrOlder() {
  }

  @EnabledOnManufacturer("imaginary")
  @Test
  void testForImaginaryDevicesOnly() {
  }

  @DisabledOnManufacturer("google")
  @Test
  void testNotForGoogleDevices() {
  }

  public static class CustomTestProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
      return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
      return Stream.of(invocationContext("apple"), invocationContext("banana"));
    }

    private TestTemplateInvocationContext invocationContext(String parameter) {
      return new TestTemplateInvocationContext() {
        @Override
        public String getDisplayName(int invocationIndex) {
          return "number " + invocationIndex + ": " + parameter;
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
          return Collections.singletonList(new ParameterResolver() {
            @Override
            public boolean supportsParameter(ParameterContext parameterContext,
                                             ExtensionContext extensionContext) {
              return parameterContext.getParameter().getType().equals(String.class);
            }

            @Override
            public Object resolveParameter(ParameterContext parameterContext,
                                           ExtensionContext extensionContext) {
              return parameter;
            }
          });
        }
      };
    }
  }

  @Nested
  class InnerTests {

    @Test
    void innerTest() {

    }
  }
}
