package de.mannodermaus.sample

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import de.mannodermaus.junit5.ActivityScenarioExtension
import de.mannodermaus.junit5.condition.EnabledIfBuildConfigValue
import de.mannodermaus.junit5.sample.ActivityOne
import de.mannodermaus.junit5.sample.R
import java.util.function.Supplier
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.ValueSource

class ActivityOneTest {
    companion object {
        val someLettersOfTheAlphabet = Supplier { Stream.of("A", "B", "C") }
    }

    @JvmField
    @RegisterExtension
    val scenarioExtension = ActivityScenarioExtension.launch<ActivityOne>()

    @DisplayName("test with pretty display name")
    @Test
    fun testDisplayName() {
        onView(withId(R.id.textView)).check(matches(withText("0")))
    }

    @EnabledIfBuildConfigValue(named = "MY_VALUE", matches = "true")
    @Test
    fun testExample(scenario: ActivityScenario<ActivityOne>) {
        onView(withId(R.id.textView)).check(matches(withText("0")))
    }

    // This test is disabled by default, because the sample module
    // defines an excludeTags() clause for this tag in its build.gradle file
    @Tag("slow")
    @Test
    fun disabledTest(scenario: ActivityScenario<ActivityOne>) {
        onView(withId(R.id.textView)).check(matches(withText("0")))
    }

    @ValueSource(strings = ["value1", "value2"])
    @ParameterizedTest
    fun parameterizedTestExample(value: String, scenario: ActivityScenario<ActivityOne>) {
        scenario.onActivity {
            assertEquals(0, it.getClickCount())
            it.setButtonLabel(value)
        }

        onView(withId(R.id.button)).check(matches(withText(value)))
        onView(withId(R.id.button)).perform(click())

        scenario.onActivity { assertEquals(1, it.getClickCount()) }
    }

    @FieldSource("someLettersOfTheAlphabet")
    @ParameterizedTest
    fun parameterizedTestWithFieldSource(letter: String) {
        scenarioExtension.scenario.onActivity { it.setButtonLabel(letter) }

        onView(withText(letter)).perform(click())

        scenarioExtension.scenario.onActivity { assertEquals(1, it.getClickCount()) }
    }

    @RepeatedTest(3)
    fun repeatedTestExample(
        repetitionInfo: RepetitionInfo,
        scenario: ActivityScenario<ActivityOne>,
    ) {
        val count = repetitionInfo.currentRepetition

        for (i in 0 until count) {
            onView(withId(R.id.button)).perform(click())
        }

        scenario.onActivity { assertEquals(count, it.getClickCount()) }
    }
}
