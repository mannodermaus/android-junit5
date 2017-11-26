package de.mannodermaus.junit5.sample

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import de.mannodermaus.junit5.ActivityTest
import de.mannodermaus.junit5.Tested
import org.hamcrest.Matchers.allOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@ActivityTest(MyActivity::class)
class SomeTest {

  @Test
  fun doSomething(activity: MyActivity) {
    onView(
        withId(R.id.textView))
        .check(matches(
            allOf(
                isDisplayed(),
                withText("Hello World!"))))
  }

  @Test
  fun doSomethingWrappedParam(test: Tested<MyActivity>) {
    Assertions.assertNotNull(test)
  }

  @Test
  fun doSomethingWithoutParam() {
    Assertions.assertEquals(4, 2 + 2)
  }
}

class MethodBasedTest {

  @Test
  @ActivityTest(MyActivity::class)
  fun methodTest() {
    onView(
        withId(R.id.textView))
        .check(matches(
            isDisplayed()))
  }
}
