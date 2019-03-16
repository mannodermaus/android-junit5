package de.mannodermaus.junit5

import android.os.Build
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.Runner

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this.
 *
 * Suppressing unused, since this class is instantiated reflectively.
 *
 * Replacement For:
 * AndroidJUnit4
 */
@Suppress("unused")
internal class AndroidJUnit5(klass: Class<*>) : JUnitPlatform(klass)

/**
 * Since we can't reference AndroidJUnit5 directly, use this factory for instantiation.
 *
 * On API 26 and above, delegate to the real implementation to drive JUnit 5 tests.
 * Below that however, they wouldn't work; for this case, delegate a dummy runner
 * which will highlight these tests as ignored.
 */
internal fun createJUnit5Runner(klass: Class<*>): Runner =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      AndroidJUnit5(klass)
    } else {
      DummyJUnit5(klass)
    }
