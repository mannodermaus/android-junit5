package de.mannodermaus.junit5

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Runner
import org.junit.runners.model.RunnerBuilder

/**
 * Custom RunnerBuilder hooked into the main Test Instrumentation Runner
 * provided by the Android Test Support Library, which allows to run
 * the JUnit Platform for instrumented tests. With this,
 * the default JUnit 4-based Runner for Android instrumented tests is,
 * in a way, tricked into detecting JUnit Jupiter tests as well.
 *
 * The RunnerBuilder is added to the instrumentation runner
 * through a custom "testInstrumentationRunnerArgument" in the build.gradle script:
 *
 * <pre>
 *   android {
 *     defaultConfig {
 *       testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
 *       testInstrumentationRunnerArgument "runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder"
 *     }
 *   }
 * </pre>
 *
 * (Suppressing unused, since this is hooked into the
 * project configuration via a Test Instrumentation Runner Argument.)
 */
@Suppress("unused")
class AndroidJUnit5Builder : RunnerBuilder() {

  private val junit5Available by lazy {
    try {
      // The verification order of this block is quite important.
      // Do not change it without thorough testing of potential consequences!
      // After tampering with this, verify that integration
      // with applications using JUnit 5 for UI tests still works,
      // AND that integration with applications NOT using JUnit 5 for UI tests still works.
      //
      // First, verify the existence of junit-jupiter-api on the classpath.
      // Then, verify that the JUnitPlatform Runner is available on the classpath.
      // It is VERY important to perform this check BEFORE verifying the existence
      // of the AndroidJUnit5 Runner, which inherits from JUnitPlatform. If this is omitted,
      // an uncatchable verification error will be raised, rendering instrumentation testing
      // for applications without the desire to include JUnit 5 effectively useless.
      // The simple Class.forName() check however will catch this allowed inconsistency,
      // and gracefully abort.
      Class.forName("org.junit.jupiter.api.Test")
      Class.forName("org.junit.platform.runner.JUnitPlatform")
      Class.forName("de.mannodermaus.junit5.AndroidJUnit5")
      true
    } catch (e: Throwable) {
      false
    }
  }

  private val parsedFilters by lazy {
    // The user may apply test filters to their instrumentation tests through the Gradle plugin's DSL,
    // which aren't subject to the filtering imposed through adb.
    // A special resource file may be looked up at runtime, containing
    // the filters to apply by the AndroidJUnit5 runner.
    // This is initialized at most once, when the first eligible test class
    // is checked. See ParsedFilters.kt for more
    ParsedFilters.fromContext(InstrumentationRegistry.getInstrumentation().context)
  }

  @Throws(Throwable::class)
  override fun runnerForClass(testClass: Class<*>): Runner? {
    try {
      if (!junit5Available) {
        return null
      }

      if (testClass.jupiterTestMethods().isEmpty()) {
        return null
      }

      return createJUnit5Runner(testClass, parsedFilters)

    } catch (e: NoClassDefFoundError) {
      Log.e(LOG_TAG, "JUnitPlatform not found on runtime classpath")
      throw IllegalStateException(
          "junit-platform-runner not found on runtime classpath of instrumentation tests; " +
              "please review your androidTest dependencies or raise an issue.", e)

    } catch (e: Throwable) {
      Log.e(LOG_TAG, "Error constructing runner", e)
      throw e
    }
  }
}
