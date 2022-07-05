@file:Suppress("UnstableApiUsage", "DEPRECATION")

package de.mannodermaus.gradle.plugins.junit5.internal.config

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.DynamicFeaturePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.instrumentationTestVariant
import de.mannodermaus.gradle.plugins.junit5.internal.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.providers.KotlinDirectoryProvider
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project

internal class PluginConfig
private constructor(
    private val project: Project,
    private val legacyVariants: DomainObjectSet<out BaseVariant>,
    private val componentsExtension: AndroidComponentsExtension<*, *, *>
) {

    companion object {
        fun find(project: Project, plugin: BasePlugin): PluginConfig? {
            val componentsExtension = project.extensions
                .findByName("androidComponents") as? AndroidComponentsExtension<*, *, *>
                ?: return null

            val legacyExtension = project.extensions
                .findByName("android") as? BaseExtension
                ?: return null

            val legacyVariants = when (plugin) {
                is AppPlugin -> (legacyExtension as AppExtension).applicationVariants
                is LibraryPlugin -> (legacyExtension as LibraryExtension).libraryVariants
                is DynamicFeaturePlugin -> (legacyExtension as AppExtension).applicationVariants
                else -> return null
            }

            return PluginConfig(project, legacyVariants, componentsExtension)
        }
    }

    val hasJacocoPlugin get() = project.plugins.hasPlugin("jacoco")
    private val hasKotlinPlugin get() = project.plugins.findPlugin("kotlin-android") != null

    fun finalizeDsl(block: (CommonExtension<*, *, *, *>) -> Unit) {
        componentsExtension.finalizeDsl(block)
    }

    fun onVariants(block: (Variant) -> Unit) {
        componentsExtension.onVariants(callback = block)
    }

    fun directoryProvidersOf(variant: Variant): Set<DirectoryProvider> {
        // Locate the legacy variant for the given one, since the new API
        // does not give access to variant-specific source sets and class outputs
        return legacyVariants.firstOrNull { it.name == variant.name }
            ?.run { directoryProvidersOf(this) }
            ?: emptySet()
    }

    fun instrumentationTestVariantOf(variant: Variant): TestVariant? {
        return legacyVariants.firstOrNull { it.name == variant.name }
            ?.run { this.instrumentationTestVariant }
    }

    /* Private */

    private fun directoryProvidersOf(legacyVariant: BaseVariant): Set<DirectoryProvider> {
        return buildSet {
            add(JavaDirectoryProvider(legacyVariant))

            // Kotlin integration
            if (hasKotlinPlugin) {
                add(KotlinDirectoryProvider(project, legacyVariant))
            }
        }
    }
}
