package de.mannodermaus.junit5

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinInstrumentationTests {

  @JvmField
  @RegisterExtension
  val scenarioExtension = ActivityScenarioExtension.launch<TestActivity>()

  @Test
  fun testUsingGetScenario() {
    val scenario = scenarioExtension.scenario

    scenario.onActivity { onView(withText("TestActivity")).check(matches(isDisplayed())) }
  }

  @Test
  fun testUsingMethodParameter(scenario: ActivityScenario<TestActivity>) {
    scenario.onActivity { onView(withText("TestActivity")).check(matches(isDisplayed())) }
  }
}
