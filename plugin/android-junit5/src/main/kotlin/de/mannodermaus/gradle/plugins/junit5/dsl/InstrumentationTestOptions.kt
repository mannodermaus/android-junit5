package de.mannodermaus.gradle.plugins.junit5.dsl

import org.gradle.api.tasks.Input

/**
 * Options for controlling instrumentation test execution
 */
public abstract class InstrumentationTestOptions {

    /**
     * Whether to configure JUnit 5 instrumentation tests automatically.
     */
    @get:Input
    public var enabled: Boolean = true

    /**
     * Whether to configure JUnit 5 instrumentation tests automatically.
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
    public var integrityCheckEnabled: Boolean = true

    /**
     * Whether to check if the instrumentation tests
     * are correctly set up. If this is disabled, the plugin
     * won't raise an error during evaluation if the instrumentation
     * libraries or the test runner are missing.
     */
    public fun integrityCheckEnabled(state: Boolean) {
        this.integrityCheckEnabled = state
    }
}
