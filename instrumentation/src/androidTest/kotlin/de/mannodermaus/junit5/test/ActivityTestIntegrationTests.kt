package de.mannodermaus.junit5.test

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import de.mannodermaus.junit5.ActivityAlreadyLaunchedException
import de.mannodermaus.junit5.ActivityNotLaunchedException
import de.mannodermaus.junit5.ActivityTest
import de.mannodermaus.junit5.Tested
import de.mannodermaus.junit5.test.activities.FirstActivity
import de.mannodermaus.junit5.test.activities.OtherActivity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ActivityTest(FirstActivity::class)
class ActivityTestIntegrationTests {

  @Test
  @DisplayName("Launch Automatically: No Parameters")
  fun launchAutomaticallyNoParameters() {
    onView(withId(R.id.textView)).check(matches(withText("Hello World!")))
  }

  @Test
  @DisplayName("Launch Automatically: Activity Parameter")
  fun launchAutomaticallyActivityParameter(activity: FirstActivity) {
    Assertions.assertEquals(activity.textView.text, "Hello World!")
  }

  @Test
  @DisplayName("Launch Automatically: Tested Parameter")
  fun launchAutomaticallyTestedParameter(tested: Tested<FirstActivity>) {
    Assertions.assertEquals(tested.activity!!.textView.text, "Hello World!")
  }

  @Test
  @Disabled("No idea how to assert this using an integration test")
  @DisplayName("Tested Parameter with invalid Activity type throws")
  fun testedParameterWithInvalidActivityTypeThrows(tested: Tested<OtherActivity>) {
  }

  @Test
  @ActivityTest(OtherActivity::class)
  @DisplayName("Method Parameter overrides class-level declaration")
  fun methodLevelParameterOverridesClassLevelDeclaration(activity: OtherActivity) {
  }

  @Test
  @ActivityTest(FirstActivity::class, launchActivity = false)
  @DisplayName("Launching twice causes Exception")
  fun launchingTwiceCausesException(tested: Tested<FirstActivity>) {
    tested.launchActivity()
    Assertions.assertThrows(ActivityAlreadyLaunchedException::class.java) {
      tested.launchActivity()
    }
  }

  @Test
  @DisplayName("Finishing twice causes Exception")
  fun finishingTwiceCausesException(tested: Tested<FirstActivity>) {
    tested.finishActivity()
    Assertions.assertThrows(ActivityNotLaunchedException::class.java) {
      tested.finishActivity()
    }
  }
}
