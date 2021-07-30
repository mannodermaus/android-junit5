package de.mannodermaus.gradle.plugins.junit5.dsl

import de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.EXTENSION_NAME
import de.mannodermaus.gradle.plugins.junit5.FiltersExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extend
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionByName
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionExists
import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import org.gradle.api.Project

internal fun Project.attachGlobalDsl() {
    // Hook the default JUnit Platform configuration into the project
    project.extend<AndroidJUnitPlatformExtension>(EXTENSION_NAME) { junitPlatform ->
        // General-purpose filters
        junitPlatform.attachFiltersDsl(qualifier = null)
    }
}

internal fun Project.attachSpecificDsl(projectConfig: PluginConfig) {
    // Variant-specific filters:
    // This will add filters for build types (e.g. "debug" or "release")
    // as well as composed variants  (e.g. "freeDebug" or "paidRelease")
    // and product flavors (e.g. "free" or "paid")
    android.buildTypes.all { buildType ->
        // "debugFilters"
        // "releaseFilters"
        junitPlatform.attachFiltersDsl(qualifier = buildType.name)
    }

    // Attach DSL objects for all permutations of variants available.
    // As an example, assume the incoming `variant` to be:
    // Name:                    "brandADevelopmentDebug"
    // Dimension "brand":       "brandA"
    // Dimension "environment": "development"
    // Build Type Name:         "debug"
    //
    // The following DSL objects have to be generated from this:
    // 1) brandADevelopmentDebugFilters
    // 2) brandAFilters
    // 3) developmentFilters
    projectConfig.variants.all { variant ->
        // 1) Fully-specialized name ("brandADevelopmentDebugFilters")
        junitPlatform.attachFiltersDsl(qualifier = variant.name)

        variant.productFlavors.forEach { flavor ->
            // 2) & 3) Single flavors ("brandAFilters" & "developmentFilters")
            junitPlatform.attachFiltersDsl(qualifier = flavor.name)
        }
    }
}

private fun AndroidJUnitPlatformExtension.attachFiltersDsl(qualifier: String? = null) {
    val extensionName = filtersExtensionName(qualifier)

    if (this.extensionExists(extensionName)) {
        return
    }

    this.extend<FiltersExtension>(extensionName)
}

internal fun Project.evaluateExtensions() {
    junitPlatform._filters.forEach { (qualifier, actions) ->
        val extensionName = junitPlatform.filtersExtensionName(qualifier)
        val extension = junitPlatform.extensionByName<FiltersExtension>(extensionName)

        actions.forEach { action ->
            extension.action()
        }
    }
}
