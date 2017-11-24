package de.mannodermaus.junit5

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@ActivityTest(MyActivity::class)
class SomeTest {

  @Test
  fun doSomething(activity: MyActivity) {
    onView(withText("Hello World!")).check(matches(isDisplayed()))
  }

  @Test
  fun doSomethingWithoutParam() {
    Assertions.assertEquals(4, 2 + 2)
  }
}
