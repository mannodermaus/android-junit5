package de.mannodermaus.gradle.plugins.junit5.dsl

import org.gradle.api.provider.Property
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
    public abstract val enabled: Property<Boolean>

    /**
     * The version of the instrumentation libraries to autoconfigure.
     */
    @get:Input
    public abstract val version: Property<String>

    /**
     * Whether to include a dependency on the android-test-extensions library
     * on top of the main instrumentation artifacts.
     */
    @get:Input
    public abstract val includeExtensions: Property<Boolean>
}
