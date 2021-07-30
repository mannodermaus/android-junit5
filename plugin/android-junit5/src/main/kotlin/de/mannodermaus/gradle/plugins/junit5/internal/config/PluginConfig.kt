package de.mannodermaus.gradle.plugins.junit5.internal.config

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.plugins.AppPlugin
import com.android.build.gradle.internal.plugins.DynamicFeaturePlugin
import com.android.build.gradle.internal.plugins.LibraryPlugin
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class PluginConfig
private constructor(private val project: Project, val variants: DomainObjectSet<out BaseVariant>) {

    companion object {
        internal val supportedPlugins: List<String> =
                listOf("com.android.application", "com.android.library", "com.android.dynamic-feature")

        fun find(project: Project, plugin: Plugin<*>): PluginConfig? {
            // Grab the list of variants for all supported Android plugins
            val variants = when (plugin) {
                is AppPlugin -> (plugin.extension as AppExtension).applicationVariants
                is LibraryPlugin -> (plugin.extension as LibraryExtension).libraryVariants
                is DynamicFeaturePlugin -> (plugin.extension as AppExtension).applicationVariants
                else -> null
            }

            return variants?.let { PluginConfig(project, it) }
        }
    }

    val jacocoPluginApplied get() = project.plugins.findPlugin("jacoco") != null
    val kotlinPluginApplied get() = project.plugins.findPlugin("kotlin-android") != null
}
