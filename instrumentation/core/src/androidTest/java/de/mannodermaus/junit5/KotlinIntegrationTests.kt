package de.mannodermaus.junit5

import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinIntegrationTests {

  @JvmField
  @RegisterExtension
  val scenarioExtension = ActivityScenarioExtension.launch<TestActivity>()

  @Test
  fun testUsingGetScenario() {
    val scenario = scenarioExtension.scenario

    scenario.onActivity { assertThat(it).isNotNull() }
  }

  @Test
  fun testUsingMethodParameter(scenario: ActivityScenario<TestActivity>) {
    scenario.onActivity { assertThat(it).isNotNull() }
  }
}
