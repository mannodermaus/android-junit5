package de.mannodermaus.junit5

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KotlinInstrumentationTests {

  @JvmField
  @RegisterExtension
  val scenarioExtension = ActivityScenarioExtension.launch<TestActivity>()

  @Test
  fun testUsingGetScenario() {
    val scenario = scenarioExtension.scenario
    onView(withText("TestActivity")).check(matches(isDisplayed()))
    scenario.onActivity { it.changeText("New Text") }
    onView(withText("New Text")).check(matches(isDisplayed()))
  }

  @Test
  fun testUsingMethodParameter(scenario: ActivityScenario<TestActivity>) {
    onView(withText("TestActivity")).check(matches(isDisplayed()))
    scenario.onActivity { it.changeText("New Text") }
    onView(withText("New Text")).check(matches(isDisplayed()))
  }

  @ParameterizedTest
  @ValueSource(ints = [1, 4, 6, 7])
  fun kotlinTestWithParameters(value: Int) {

  }
}
