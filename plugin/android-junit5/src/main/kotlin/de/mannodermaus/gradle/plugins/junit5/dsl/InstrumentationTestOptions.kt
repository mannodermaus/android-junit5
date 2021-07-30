package de.mannodermaus.gradle.plugins.junit5.dsl

/**
 * Options for controlling instrumentation test execution
 */
public abstract class InstrumentationTestOptions {

    public operator fun invoke(config: InstrumentationTestOptions.() -> Unit) {
        this.config()
    }

    /**
     * Whether or not to check if the instrumentation tests
     * are correctly set up. If this is disabled, the plugin
     * won't raise an error during evaluation if the instrumentation
     * libraries or the test runner are missing.
     */
    public var integrityCheckEnabled: Boolean = true

    /**
     * Whether or not to check if the instrumentation tests
     * are correctly set up. If this is disabled, the plugin
     * won't raise an error during evaluation if the instrumentation
     * libraries or the test runner are missing.
     */
    public fun integrityCheckEnabled(state: Boolean) {
        this.integrityCheckEnabled = state
    }
}
