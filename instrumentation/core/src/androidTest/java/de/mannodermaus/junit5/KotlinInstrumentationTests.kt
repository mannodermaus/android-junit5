package de.mannodermaus.junit5

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import de.mannodermaus.junit5.condition.EnabledOnManufacturer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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

  @DisplayName("cool display name")
  @Test
  fun testWithDisplayName() {
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

  @RepeatedTest(3)
  fun kotlinRepeatedTest(info: RepetitionInfo) {

  }

  @TestFactory
  fun kotlinTestFactory() = listOf(
    dynamicTest("Dynamic 1") {},
    dynamicTest("Dynamic 2") {}
  )

  @EnabledOnManufacturer(["Samsung"])
  @Test
  fun onlyOnSamsung() {

  }
}
