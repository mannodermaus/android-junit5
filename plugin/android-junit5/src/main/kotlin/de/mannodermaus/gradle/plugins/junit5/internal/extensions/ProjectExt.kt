package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.BaseExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import org.gradle.api.Project

/**
 * Access the Android extension applied by a respective plugin.
 * Equivalent to "Project#android" in Groovy.
 */
internal val Project.android: BaseExtension
    get() = extensions.getByName("android") as BaseExtension

internal fun Project.hasAndroidPlugin(): Boolean {
    return PluginConfig.supportedPlugins.any { plugins.findPlugin(it) != null }
}
