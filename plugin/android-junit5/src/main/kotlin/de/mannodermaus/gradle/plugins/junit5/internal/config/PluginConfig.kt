@file:Suppress("UnstableApiUsage", "DEPRECATION")

package de.mannodermaus.gradle.plugins.junit5.internal.config

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.DynamicFeaturePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.internal.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.providers.KotlinDirectoryProvider
import org.gradle.api.Project

internal class PluginConfig
private constructor(
    private val project: Project,
    private val legacyPlugin: BasePlugin,
    private val componentsExtension: AndroidComponentsExtension<*, *, *>,
) {

    companion object {
        fun find(project: Project, plugin: BasePlugin): PluginConfig? {
            val componentsExtension =
                project.extensions.findByName("androidComponents")
                    as? AndroidComponentsExtension<*, *, *> ?: return null

            return PluginConfig(project, plugin, componentsExtension)
        }
    }

    val hasJacocoPlugin
        get() = project.plugins.hasPlugin("jacoco")

    private val hasKotlinPlugin
        get() = project.plugins.findPlugin("kotlin-android") != null

    val currentAgpVersion
        get() = componentsExtension.pluginVersion

    fun finalizeDsl(block: (CommonExtension<*, *, *, *, *>) -> Unit) {
        componentsExtension.finalizeDsl(block)
    }

    fun onVariants(block: (Variant) -> Unit) {
        componentsExtension.onVariants(callback = block)
    }

    fun directoryProvidersOf(variant: Variant): Set<DirectoryProvider> {
        // Locate the legacy variant for the given one, since the new API
        // does not give access to variant-specific source sets and class outputs
        val legacyExtension = project.extensions.findByName("android")

        val legacyVariants =
            try {
                when (legacyPlugin) {
                    is AppPlugin -> (legacyExtension as AppExtension).applicationVariants
                    is LibraryPlugin -> (legacyExtension as LibraryExtension).libraryVariants
                    is DynamicFeaturePlugin -> (legacyExtension as AppExtension).applicationVariants
                    else -> null
                }
            } catch (_: ClassCastException) {
                // AGP 9 removes access to the legacy API and thus, Jacoco integration
                // is deprecated henceforth. When the above block yields a ClassCastException,
                // we know that we're using exclusively against the new DSL and return an empty set
                // to the caller
                null
            }

        return legacyVariants
            ?.firstOrNull { it.name == variant.name }
            ?.let(::directoryProvidersOf)
            .orEmpty()
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
