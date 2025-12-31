package de.mannodermaus.gradle.plugins.junit5.dsl

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/** Options for controlling instrumentation test execution */
public interface InstrumentationTestOptions {

    /**
     * Whether to configure JUnit 5 instrumentation tests automatically when junit-jupiter-api is
     * added as an androidTestImplementation dependency.
     */
    @get:Input public val enabled: Property<Boolean>

    /** The version of the instrumentation libraries to autoconfigure. */
    @get:Input public val version: Property<String>

    /**
     * How to behave when executing instrumentation tests on an unsupported device (i.e. too old).
     */
    @get:Input public val behaviorForUnsupportedDevices: Property<UnsupportedDeviceBehavior>

    /**
     * Whether to include a dependency on the android-test-extensions library on top of the main
     * instrumentation artifacts.
     */
    @get:Input public val includeExtensions: Property<Boolean>

    /**
     * Whether to use configuration parameters configured via the plugin DSL for instrumentation
     * tests, too.
     */
    @get:Input public val useConfigurationParameters: Property<Boolean>
}
