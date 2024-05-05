package de.mannodermaus.junit5.internal.runners

import android.os.Build
import org.junit.runner.Runner

/**
 * Since we can't reference AndroidJUnit5 directly, use this factory for instantiation.
 *
 * On API 26 and above, delegate to the real implementation to drive JUnit 5 tests.
 * Below that however, they wouldn't work; for this case, delegate a dummy runner
 * which will highlight these tests as ignored.
 */
internal fun tryCreateJUnit5Runner(
    klass: Class<*>,
    paramsSupplier: () -> AndroidJUnit5RunnerParams
): Runner? {
    val runner = if (Build.VERSION.SDK_INT >= 26) {
        AndroidJUnit5(klass, paramsSupplier)
    } else {
        DummyJUnit5(klass)
    }

    // It's still possible for the runner to not be relevant to the test run,
    // which is related to how further filters are applied (e.g. via @Tag).
    // Only return the runner to the instrumentation if it has any tests to contribute,
    // otherwise there would be a mismatch between the number of test classes reported
    // to Android, and the number of test classes actually tested with JUnit 5 (ref #298)
    return runner.takeIf(Runner::hasExecutableTests)
}

private fun Runner.hasExecutableTests() =
    this.description.children.isNotEmpty()
