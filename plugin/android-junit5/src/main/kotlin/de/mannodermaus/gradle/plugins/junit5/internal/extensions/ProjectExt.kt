package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.BaseExtension
import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.EXTENSION_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import org.gradle.api.Project

internal val Project.junitPlatform
    get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

internal val Project.android
    get() = extensionByName<BaseExtension>("android")

internal fun Project.hasAndroidPlugin(): Boolean {
    return PluginConfig.supportedPlugins.any { plugins.findPlugin(it) != null }
}
