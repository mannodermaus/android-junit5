package de.mannodermaus.junit5

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SomeTest {

//  @Rule
//  val rule = ActivityTestRule<MyActivity>(MyActivity::class.java)

  @Test
  fun doSomething() {
    Assertions.assertEquals(5, 2 + 2)
//    onView(withText("Hello World!")).check(matches(isDisplayed()))
  }
}
