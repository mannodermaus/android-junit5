package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension.Companion.createJUnit5Extension
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_AGP_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_GRADLE_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import de.mannodermaus.gradle.plugins.junit5.internal.configureJUnit5
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.whenAndroidPluginAdded
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireGradle
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireAgp
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

/**
 * Android JUnit Platform plugin for Gradle.
 * Configures JUnit 5 tasks on all unit-tested variants of an Android project.
 */
public class AndroidJUnitPlatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        requireGradle(
            actual = GradleVersion.current(),
            required = MIN_REQUIRED_GRADLE_VERSION
        ) {
            "android-junit5 plugin requires Gradle $MIN_REQUIRED_GRADLE_VERSION or later"
        }

        project.whenAndroidPluginAdded { plugin ->
            PluginConfig.find(project, plugin)?.let { config ->
                requireAgp(
                    actual = config.currentAgpVersion,
                    required = MIN_REQUIRED_AGP_VERSION
                ) {
                    "android-junit5 plugin requires Android Gradle Plugin $MIN_REQUIRED_AGP_VERSION or later"
                }

                val extension = project.createJUnit5Extension()
                configureJUnit5(project, config, extension)
            }
        }
    }
}
