package de.mannodermaus.junit5.internal.runners

import android.os.Build
import de.mannodermaus.junit5.internal.JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION
import org.junit.runner.Runner

/**
 * Since we can't reference [AndroidJUnitFramework] directly, use this factory for instantiation.
 *
 * On devices with sufficient API levels, delegate to the real implementation to drive
 * the execution of JUnit Framework tests. Below this threshold, they wouldn't work, however;
 * for this case, delegate to a dummy runner which will highlight these tests as ignored.
 */
internal fun tryCreateJUnitFrameworkRunner(
    klass: Class<*>,
    paramsSupplier: () -> JUnitFrameworkRunnerParams
): Runner? {
    val runner = if (Build.VERSION.SDK_INT >= JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION) {
        AndroidJUnitFramework(klass, paramsSupplier)
    } else {
        DummyJUnitFramework(klass)
    }

    // It's still possible for the runner to not be relevant to the test run,
    // which is related to how further filters are applied (e.g. via @Tag).
    // Only return the runner to the instrumentation if it has any tests to contribute,
    // otherwise there would be a mismatch between the number of test classes reported
    // to Android, and the number of test classes actually tested with JUnit (ref #298)
    return runner.takeIf(Runner::hasExecutableTests)
}

private fun Runner.hasExecutableTests() =
    this.description.children.isNotEmpty()
