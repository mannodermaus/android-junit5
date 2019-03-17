package de.mannodermaus.junit5;

import androidx.test.core.app.ActivityScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.google.common.truth.Truth.assertThat;

class JavaIntegrationTests {

  @RegisterExtension
  final ActivityScenarioExtension<TestActivity> scenarioExtension = ActivityScenarioExtension.launch(TestActivity.class);

  @Test
  void testUsingGetScenario() {
    ActivityScenario<TestActivity> scenario = scenarioExtension.getScenario();

    scenario.onActivity(it -> assertThat(it).isNotNull());
  }

  @Test
  void testUsingMethodParameter(ActivityScenario<TestActivity> scenario) {
    scenario.onActivity(it -> assertThat(it).isNotNull());
  }
}
