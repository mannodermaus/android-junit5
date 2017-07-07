package de.mannodermaus.gradle.plugins.android_junit5

import org.gradle.api.Project
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

/**
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through JUnitPlatformExtension
 */
class AndroidJUnitPlatformExtension extends JUnitPlatformExtension {

    private static final String PLATFORM_VERSION = "1.0.0-M5"

    AndroidJUnitPlatformExtension(Project project) {
        super(project)
        platformVersion = PLATFORM_VERSION
    }

    /**
     * The version of JUnit Jupiter to use.
     *
     * Defaults to {@code '5.0.0-M4'}.
     */
    String jupiterVersion = '5.0.0-M5'

    /**
     * The version of JUnit Vintage Engine to use.
     *
     * Defaults to {@code '4.12.0-M4'}.
     */
    String vintageVersion = '4.12.0-M5'
}
