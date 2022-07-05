package de.mannodermaus.gradle.plugins.junit5

import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension.Companion.createJUnit5Extension
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_AGP_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_GRADLE_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import de.mannodermaus.gradle.plugins.junit5.internal.configureJUnit5
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.whenAndroidPluginAdded
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireGradle
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Android JUnit Platform plugin for Gradle.
 * Configures JUnit 5 tasks on all unit-tested variants of an Android project.
 */
public class AndroidJUnitPlatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        requireGradle(MIN_REQUIRED_GRADLE_VERSION) {
            "android-junit5 plugin requires Gradle $MIN_REQUIRED_GRADLE_VERSION or later"
        }

        requireVersion(
            actual = ANDROID_GRADLE_PLUGIN_VERSION,
            required = MIN_REQUIRED_AGP_VERSION
        ) {
            "android-junit5 plugin requires Android Gradle Plugin $MIN_REQUIRED_AGP_VERSION or later"
        }

        project.whenAndroidPluginAdded { plugin ->
            val extension = project.createJUnit5Extension()
            val config = PluginConfig.find(project, plugin)
            if (config != null) {
                configureJUnit5(project, config, extension)
            }
        }
    }
}
