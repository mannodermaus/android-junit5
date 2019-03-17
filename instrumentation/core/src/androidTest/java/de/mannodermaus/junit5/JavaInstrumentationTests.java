package de.mannodermaus.junit5;

import androidx.test.core.app.ActivityScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

class JavaInstrumentationTests {

  @RegisterExtension
  final ActivityScenarioExtension<TestActivity> scenarioExtension = ActivityScenarioExtension.launch(TestActivity.class);

  @Test
  void testUsingGetScenario() {
    ActivityScenario<TestActivity> scenario = scenarioExtension.getScenario();

    scenario.onActivity(it -> onView(withText("TestActivity")).check(matches(isDisplayed())));
  }

  @Test
  void testUsingMethodParameter(ActivityScenario<TestActivity> scenario) {
    scenario.onActivity(it -> onView(withText("TestActivity")).check(matches(isDisplayed())));
  }
}
