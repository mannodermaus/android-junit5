package de.mannodermaus.junit5

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit5::class)
class SomeTest {

  @Rule
  val rule = ActivityTestRule<MyActivity>(MyActivity::class.java)

  @Test
  fun doSomething() {
//    Assertions.assertEquals(5, 2 + 2)
    onView(withText("Hello World!")).check(matches(isDisplayed()))
  }
}
