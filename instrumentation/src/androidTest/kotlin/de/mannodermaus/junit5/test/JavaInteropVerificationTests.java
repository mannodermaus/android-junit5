package de.mannodermaus.junit5.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import de.mannodermaus.junit5.ActivityTest;
import de.mannodermaus.junit5.Tested;
import de.mannodermaus.junit5.test.activities.FirstActivity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification that the library APIs
 * still kinda look good when invoked from Java.
 *
 * This class serves purely as syntactical validation,
 * and doesn't actually conduct meaningful tests itself -
 * check the {@link ActivityTestIntegrationTests} for that sort of thing.
 */
@ActivityTest(FirstActivity.class)
class JavaInteropVerificationTests {

  @Test
  @DisplayName("Launch without Parameters")
  void launchAutomaticallyNoParameters() {
  }

  @Test
  @DisplayName("Launch with Activity Parameter")
  void launchAutomaticallyActivityParameter(FirstActivity activity) {
  }

  @Test
  @ActivityTest(value = FirstActivity.class, launchActivity = false)
  @DisplayName("Tested<T> API Round Trip")
  void testedApiRoundTrip(Tested<FirstActivity> tested) {
    FirstActivity launched = tested.launchActivity(null);

    onView(withId(R.id.textView)).perform(click());

    Instrumentation.ActivityResult result = tested.getActivityResult();
    assertThat(result.getResultCode()).isEqualTo(Activity.RESULT_OK);
    assertThat(result.getResultData()).hasExtra("returnValue", 1337);

    FirstActivity retrieved = tested.getActivity();
    assertThat(launched).isEqualTo(retrieved);

    tested.finishActivity();
    FirstActivity absent = tested.getActivity();
    assertThat(absent).isNull();

    FirstActivity launchedAgain = tested.launchActivity(null);
    assertThat(launchedAgain).isNotEqualTo(launched);
  }

  @Test
  @ActivityTest(value = FirstActivity.class, targetPackage = "some.other.weird.package", launchFlags = Intent.FLAG_ACTIVITY_NO_HISTORY, initialTouchMode = false, launchActivity = false)
  @DisplayName("Configuration parameters in Annotation")
  void bloatedConfigAnnotation(Tested<FirstActivity> tested) {
  }
}
