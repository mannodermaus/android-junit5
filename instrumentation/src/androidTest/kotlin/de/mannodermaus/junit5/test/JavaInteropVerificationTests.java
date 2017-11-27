package de.mannodermaus.junit5.test;

import android.content.Intent;
import de.mannodermaus.junit5.ActivityTest;
import de.mannodermaus.junit5.Tested;
import de.mannodermaus.junit5.test.activities.FirstActivity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Verification that the library APIs
 * still kinda look good when invoked from Java.
 *
 * This class serves purely as syntactical validation,
 * and doesn't actually contains meaningful tests itself.
 * Check the {@link ActivityTestIntegrationTests} for that sort of thing.
 */
@Disabled @ActivityTest(FirstActivity.class) class JavaInteropVerificationTests {

  @Test void launchAutomaticallyNoParameters() {
  }

  @Test void launchAutomaticallyActivityParameter(FirstActivity activity) {
  }

  @Test void launchAutomaticallyTestedParameter(Tested<FirstActivity> activity) {
  }

  @Test
  @ActivityTest(value = FirstActivity.class, targetPackage = "some.other.weird.package", launchFlags = Intent.FLAG_ACTIVITY_NO_HISTORY, initialTouchMode = false, launchActivity = false)
  void bloatedConfigAnnotation(Tested<FirstActivity> activity) {
  }
}
