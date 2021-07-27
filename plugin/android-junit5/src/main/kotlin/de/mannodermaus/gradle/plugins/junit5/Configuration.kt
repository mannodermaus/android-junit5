package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.plugins.AppPlugin
import com.android.build.gradle.internal.plugins.DynamicFeaturePlugin
import com.android.build.gradle.internal.plugins.LibraryPlugin
import de.mannodermaus.gradle.plugins.junit5.internal.hasPlugin
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

/**
 * Utility class, used for controlled access
 * to a Project's configuration.
 *
 * This class provides a safe interface to access the
 * properties specific to the Android Gradle Plugin
 * in a backwards-compatible manner. It will raise a
 * [ProjectConfigurationException] early, whenever the plugin
 * is not applied in an Android environment.
 */
internal class PluginConfig
private constructor(
        private val project: Project,
        val variants: DomainObjectSet<out BaseVariant>,
) {
    companion object {
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

    val jacocoPluginApplied get() = project.hasPlugin("jacoco")
    val kotlinPluginApplied get() = project.hasPlugin("kotlin-android")
}

internal fun Project.hasAndroidPlugin(): Boolean {
    return supportedPlugins.any { hasPlugin(it) }
}

private val supportedPlugins: List<String> =
        listOf("com.android.application", "com.android.library", "com.android.dynamic-feature")

internal class JUnit5TaskConfig(
        private val variant: BaseVariant,
        project: Project) {

    private val extension = project.junitPlatform

    // There is a distinct application order, which determines how values are merged and overwritten.
    // From top to bottom, this list goes as follows (values on the bottom will override conflicting
    // entries specified above them):
    // 1) Default ("filters")
    // 2) Build-type-specific (e.g. "debug")
    // 3) Flavor-specific (e.g. "free")
    // 4) Variant-specific (e.g. "freeDebug")
    private fun collect(
            func: FiltersExtension.() -> IncludeExcludeContainer): IncludeExcludeContainer {
        // 1)
        val layer1 = filtersOf(null, func)
        // 2)
        val layer2 = layer1 + filtersOf(variant.buildType.name, func)
        // 3)
        val layer3 = variant.productFlavors
                .map { filtersOf(it.name, func) }
                .fold(layer2) { a, b -> a + b }
        // 4)
        return layer3 + filtersOf(variant.name, func)
    }

    private inline fun filtersOf(
            qualifier: String?,
            func: FiltersExtension.() -> IncludeExcludeContainer) =
            if (qualifier == null) {
                extension.filters.func()
            } else {
                extension.findFilters(qualifier).func()
            }

    val combinedIncludePatterns = this.collect { patterns }.include.toTypedArray()
    val combinedExcludePatterns = this.collect { patterns }.exclude.toTypedArray()
    val combinedIncludeTags = this.collect { tags }.include.toTypedArray()
    val combinedExcludeTags = this.collect { tags }.exclude.toTypedArray()
    val combinedIncludeEngines = this.collect { engines }.include.toTypedArray()
    val combinedExcludeEngines = this.collect { engines }.exclude.toTypedArray()
}
