package de.mannodermaus.junit5

import android.util.Log
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.Runner
import org.junit.runners.model.RunnerBuilder

/* Constants */

private const val LOG_TAG = "AndroidJUnit5"

/* Types */

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this.
 *
 * Replacement For:
 * AndroidJUnit4
 */
class AndroidJUnit5(
    klass: Class<*>
) : JUnitPlatform(klass)

class AndroidJUnit5Builder : RunnerBuilder() {

  @Throws(Throwable::class)
  override fun runnerForClass(testClass: Class<*>): Runner? {
    try {
      if (!hasTestMethods(testClass)) {
        return null
      }

      return AndroidJUnit5(testClass)

    } catch (e: Throwable) {
      Log.e(LOG_TAG, "Error constructing runner", e)
      throw e
    }
  }

  private fun hasTestMethods(testClass: Class<*>): Boolean {
    return try {
      testClass.methods.firstOrNull {
        it.isAnnotationPresent(org.junit.jupiter.api.Test::class.java)
      } != null

    } catch (t: Throwable) {
      Log.w(LOG_TAG, "$t in hasTestMethods for ${testClass.name}")
      false
    }
  }
}
