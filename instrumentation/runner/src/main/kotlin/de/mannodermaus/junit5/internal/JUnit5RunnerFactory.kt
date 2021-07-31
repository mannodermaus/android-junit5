package de.mannodermaus.junit5.internal

import android.os.Build
import org.junit.runner.Runner

internal object JUnit5RunnerFactory {
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
}
