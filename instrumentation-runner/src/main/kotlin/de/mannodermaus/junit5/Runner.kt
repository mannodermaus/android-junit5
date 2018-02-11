package de.mannodermaus.junit5

import android.util.Log
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.Runner
import org.junit.runners.model.RunnerBuilder

/* Constants */

private const val LOG_TAG = "AndroidJUnit5"
private val jupiterTestAnnotations = listOf(
    org.junit.jupiter.api.Test::class.java,
    org.junit.jupiter.api.TestFactory::class.java,
    org.junit.jupiter.params.ParameterizedTest::class.java)

/* Types */

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this.
 *
 * Replacement For:
 * AndroidJUnit4
 */
internal class AndroidJUnit5(
    klass: Class<*>
) : JUnitPlatform(klass)

/**
 * Custom RunnerBuilder hooked into the main Test Instrumentation Runner
 * provided by the Android Test Support Library, which allows to run
 * the JUnit Platform for instrumented tests. With this,
 * the default JUnit 4-based Runner for Android instrumented tests is,
 * in a way, tricked into detecting JUnit Jupiter tests as well.
 *
 * Applying the android-junit5 Gradle Plugin will hook this RunnerBuilder
 * into the instrumentation runner's lifecycle through a custom
 * "testInstrumentationRunnerArgument".
 *
 * (Suppressing unused, since this is hooked into the
 * project configuration via a Test Instrumentation Runner Argument.)
 */
@Suppress("unused")
class AndroidJUnit5Builder : RunnerBuilder() {

  @Throws(Throwable::class)
  override fun runnerForClass(testClass: Class<*>): Runner? {
    try {
      if (!testClass.hasJupiterTestMethods()) {
        return null
      }

      return AndroidJUnit5(testClass)

    } catch (e: Throwable) {
      Log.e(LOG_TAG, "Error constructing runner", e)
      throw e
    }
  }

  /* Extension Functions */

  private fun Class<*>.hasJupiterTestMethods(): Boolean {
    try {
      // Check each method in the Class for the presence
      // of the well-known list of JUnit Jupiter annotations
      val testMethod = declaredMethods.firstOrNull { method ->
        jupiterTestAnnotations.firstOrNull { annotation ->
          method.isAnnotationPresent(annotation)
        } != null
      }

      if (testMethod != null) {
        return true
      }

      // Recursively check inner classes as well
      declaredClasses.forEach { inner ->
        if (inner.hasJupiterTestMethods()) {
          return true
        }
      }

    } catch (t: Throwable) {
      Log.w(LOG_TAG, "$t in hasTestMethods for $name")
    }

    return false
  }
}
