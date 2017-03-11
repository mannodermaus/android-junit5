package de.mannodermaus.gradle.anj5

import org.gradle.api.Project
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

/**
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through JUnitPlatformExtension
 */
class AndroidJUnit5PlatformExtension extends JUnitPlatformExtension {

    AndroidJUnit5PlatformExtension(Project project) {
        super(project)
    }

    /**
     * The version of JUnit Jupiter to use.
     *
     * <p>Defaults to {@code '5.+'}.
     */
    String jupiterVersion = '5.+'
}
