package de.mannodermaus.gradle.plugins.android_junit5

import org.gradle.api.Project
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

/**
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through JUnitPlatformExtension
 */
class AndroidJUnitPlatformExtension extends JUnitPlatformExtension {

    private static final String PLATFORM_VERSION = "1.0.0-RC3"

    AndroidJUnitPlatformExtension(Project project) {
        super(project)
        platformVersion = PLATFORM_VERSION
    }

    /**
     * The version of JUnit Jupiter to use.
     */
    String jupiterVersion = "5.0.0-RC3"

    /**
     * The version of JUnit Vintage Engine to use.
     */
    String vintageVersion = "4.12.0-RC3"
}
