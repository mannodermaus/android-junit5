package de.mannodermaus.junit5.test

import android.content.Intent
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
    assertEquals(activity.textView.text, "Hello World!")
  }

  @Test
  @DisplayName("Launch Automatically: Tested Parameter")
  fun launchAutomaticallyTestedParameter(tested: Tested<FirstActivity>) {
    assertEquals(tested.activity!!.textView.text, "Hello World!")
  }

  @Test
  @Disabled("No idea how to assert this using an integration test")
  @DisplayName("Tested Parameter with invalid Activity type throws")
  fun testedParameterWithInvalidActivityTypeThrows(tested: Tested<OtherActivity>) {
  }

  @Test
  @Disabled("No idea how to assert this using an integration test")
  @ActivityTest(FirstActivity::class, launchActivity = false)
  @DisplayName("Manual Launch without Tested Parameter is useless")
  fun manualLaunchWithoutTestedParameterIsUseless() {
  }

  @Test
  @Disabled("No idea how to assert this using an integration test")
  @ActivityTest(FirstActivity::class, launchActivity = false)
  @DisplayName("Manual Launch with Activity Parameter is useless")
  fun manualLaunchWithActivityParameterIsUseless(activity: FirstActivity) {
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
    assertThrows(ActivityAlreadyLaunchedException::class.java) {
      tested.launchActivity()
    }
  }

  @Test
  @DisplayName("Finishing twice causes Exception")
  fun finishingTwiceCausesException(tested: Tested<FirstActivity>) {
    tested.finishActivity()
    assertThrows(ActivityNotLaunchedException::class.java) {
      tested.finishActivity()
    }
  }

  @Test
  @ActivityTest(
      FirstActivity::class,
      launchFlags = Intent.FLAG_ACTIVITY_NO_HISTORY)
  @DisplayName("Launch Flags are properly applied")
  fun launchFlagsAreProperlyApplied(activity: FirstActivity) {
    assertEquals(
        Intent.FLAG_ACTIVITY_NO_HISTORY,
        activity.intent.flags and Intent.FLAG_ACTIVITY_NO_HISTORY,
        "Expected Intent flag 'NO_HISTORY' wasn't provided to Activity")
  }

  @Test
  @ActivityTest(
      FirstActivity::class,
      targetPackage = "some.weird.other.package",
      launchActivity = false)
  @DisplayName("Target Package is properly applied")
  fun targetPackageIsProperlyProperlyApplied(tested: Tested<FirstActivity>) {
    val error = assertThrows(RuntimeException::class.java) {
      tested.launchActivity()
    }

    assertThat(error.message)
        .contains("Could not launch activity")

    assertThat(error.cause?.message)
        .contains("cmp=some.weird.other.package/")
        .contains("FirstActivity")
  }
}
