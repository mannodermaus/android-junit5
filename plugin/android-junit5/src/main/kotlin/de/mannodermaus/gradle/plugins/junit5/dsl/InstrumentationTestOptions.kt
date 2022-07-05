package de.mannodermaus.gradle.plugins.junit5.dsl

import org.gradle.api.tasks.Input

/**
 * Options for controlling instrumentation test execution
 */
public abstract class InstrumentationTestOptions {

    /**
     * Whether to configure JUnit 5 instrumentation tests automatically
     * when junit-jupiter-api is added as an androidTestImplementation dependency.
     */
    @get:Input
    public var enabled: Boolean = true

    /**
     * Whether to configure JUnit 5 instrumentation tests automatically
     * when junit-jupiter-api is added as an androidTestImplementation dependency.
     */
    public fun enabled(state: Boolean) {
        this.enabled = state
    }

    /**
     * Whether to check if the instrumentation tests
     * are correctly set up. If this is disabled, the plugin
     * won't raise an error during evaluation if the instrumentation
     * libraries or the test runner are missing.
     */
    @get:Input
    @Deprecated(
        message = "Starting with android-junit5 1.9.0.0, instrumentation tests are automatically configured correctly " +
                "when the junit-jupiter-api dependency is added as an androidTestImplementation dependency; " +
                "this flag no longer does anything and can be safely removed. " +
                "If you don't want to automatically configure JUnit 5 instrumentation tests, use the `enabled` flag."
    )
    public var integrityCheckEnabled: Boolean = false

    /**
     * Whether to check if the instrumentation tests
     * are correctly set up. If this is disabled, the plugin
     * won't raise an error during evaluation if the instrumentation
     * libraries or the test runner are missing.
     */
    @Deprecated(
        message = "Starting with android-junit5 1.9.0.0, instrumentation tests are automatically configured correctly " +
                "when the junit-jupiter-api dependency is added as an androidTestImplementation dependency; " +
                "this flag no longer does anything and can be safely removed. " +
                "If you don't want to automatically configure JUnit 5 instrumentation tests, use the `enabled` flag."
    )
    public fun integrityCheckEnabled(state: Boolean) {}
}
