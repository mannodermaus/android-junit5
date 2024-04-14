@file:Suppress("UnstableApiUsage", "unused")

package de.mannodermaus.gradle.plugins.junit5.internal

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.Variant
import com.android.builder.core.ComponentType.Companion.UNIT_TEST_PREFIX
import com.android.builder.core.ComponentType.Companion.UNIT_TEST_SUFFIX
import de.mannodermaus.Libraries
import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.ANDROID_JUNIT5_RUNNER_BUILDER_CLASS
import de.mannodermaus.gradle.plugins.junit5.internal.config.JUnit5TaskConfig
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.getAsList
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.getTaskName
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.hasDependency
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junit5Warn
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.namedOrNull
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.usesComposeIn
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.usesJUnitJupiterIn
import de.mannodermaus.gradle.plugins.junit5.internal.utils.excludedPackagingOptions
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5WriteFilters
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.testing.Test

internal fun configureJUnit5(
    project: Project,
    config: PluginConfig,
    extension: AndroidJUnitPlatformExtension
) {
    with(extension) {
        // General-purpose filters
        filters(qualifier = null)

        config.finalizeDsl { android ->
            prepareBuildTypeDsl(android)
            prepareUnitTests(project, android)
            prepareInstrumentationTests(project, android)
        }

        val variants = mutableSetOf<Variant>()
        config.onVariants { variant ->
            prepareVariantDsl(variant)
            variants.add(variant)
        }

        project.afterEvaluate {
            variants.forEach { variant ->
                configureUnitTests(it, variant)
                configureJacoco(it, config, variant)
                configureInstrumentationTests(it, config, variant)
            }
        }
    }
}

/* Private */

private typealias AndroidExtension = CommonExtension<*, *, *, *>

private fun AndroidJUnitPlatformExtension.prepareBuildTypeDsl(android: AndroidExtension) {
    // This will add filters for build types (e.g. "debug" or "release")
    android.buildTypes.all { buildType ->
        // "debugFilters"
        // "releaseFilters"
        filters(qualifier = buildType.name)
    }
}

private fun AndroidJUnitPlatformExtension.prepareVariantDsl(variant: Variant) {
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
    // ----------------

    // 1) Fully-specialized variant ("brandADevelopmentDebugFilters")
    filters(qualifier = variant.name)

    variant.productFlavors.forEach { flavor ->
        // 2) & 3) Single flavors ("brandAFilters" & "developmentFilters")
        filters(qualifier = flavor.second)
    }
}

private fun prepareUnitTests(project: Project, android: AndroidExtension) {
    // Add default ignore rules for JUnit 5 metadata files to the packaging options of the plugin,
    // so that consumers don't need to do this explicitly
    val options = excludedPackagingOptions()

    try {
        android.packaging.resources.excludes.addAll(options)
    } catch (e: NoSuchMethodError) {
        // TODO Because of https://issuetracker.google.com/issues/263387063,
        //  there is a breaking API change in AGP 8.x that causes a NoSuchMethodError
        //  (renaming PackagingOptions to Packaging without any fallback).
        //  Fall back to the old DSL when this happens
        options.forEach(project.android.packagingOptions::exclude)
    }
}

private fun AndroidJUnitPlatformExtension.prepareInstrumentationTests(project: Project, android: AndroidExtension) {
    if (!instrumentationTests.enabled.get()) return

    // Automatically configure instrumentation tests when JUnit 5 is detected in that configuration
    if (!project.usesJUnitJupiterIn("androidTestImplementation")) return

    // Attach the JUnit 5 RunnerBuilder to the list, unless it's already added
    val runnerBuilders = android.defaultConfig.testInstrumentationRunnerArguments.getAsList("runnerBuilder")
    if (ANDROID_JUNIT5_RUNNER_BUILDER_CLASS !in runnerBuilders) {
        android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"] = runnerBuilders
            .toMutableList()
            .also { it.add(ANDROID_JUNIT5_RUNNER_BUILDER_CLASS) }
            .joinToString(",")
    }

    val version = instrumentationTests.version.get()

    project.dependencies.add(
        "androidTestImplementation",
        "${Libraries.instrumentationCore}:$version"
    )
    project.dependencies.add(
        "androidTestRuntimeOnly",
        "${Libraries.instrumentationRunner}:$version"
    )

    if (instrumentationTests.includeExtensions.get()) {
        project.dependencies.add(
            "androidTestImplementation",
            "${Libraries.instrumentationExtensions}:$version"
        )
    }

    if (project.usesComposeIn("androidTestImplementation")) {
        project.dependencies.add(
            "androidTestImplementation",
            "${Libraries.instrumentationCompose}:$version"
        )
    }
}

private fun AndroidJUnitPlatformExtension.configureUnitTests(project: Project, variant: Variant) {
    val taskName = variant.getTaskName(prefix = UNIT_TEST_PREFIX, suffix = UNIT_TEST_SUFFIX)
    project.tasks.namedOrNull<Test>(taskName)?.configure { testTask ->
        val taskConfig = JUnit5TaskConfig(variant, this)

        testTask.useJUnitPlatform { options ->
            options.includeTags(*taskConfig.combinedIncludeTags)
            options.excludeTags(*taskConfig.combinedExcludeTags)
            options.includeEngines(*taskConfig.combinedIncludeEngines)
            options.excludeEngines(*taskConfig.combinedExcludeEngines)
        }

        testTask.include(*taskConfig.combinedIncludePatterns)
        testTask.exclude(*taskConfig.combinedExcludePatterns)

        // From the User Guide:
        // "The standard Gradle test task currently does not provide a dedicated DSL
        // to set JUnit Platform configuration parameters to influence test discovery and execution.
        // However, you can provide configuration parameters within the build script via system properties"
        testTask.systemProperties(configurationParameters.get())
    }
}

private fun AndroidJUnitPlatformExtension.configureJacoco(
    project: Project,
    config: PluginConfig,
    variant: Variant
) {
    // Connect a Code Coverage report to it if Jacoco is enabled
    if (jacocoOptions.taskGenerationEnabled.get() && config.hasJacocoPlugin) {
        val taskName = variant.getTaskName(prefix = UNIT_TEST_PREFIX, suffix = UNIT_TEST_SUFFIX)
        project.tasks.namedOrNull<Test>(taskName)?.get()?.let { testTask ->
            // Create a Jacoco friend task
            val enabledVariants = jacocoOptions.onlyGenerateTasksForVariants.get()
            if (enabledVariants.isEmpty() || variant.name in enabledVariants) {
                val directoryProviders = config.directoryProvidersOf(variant)
                val registered = AndroidJUnit5JacocoReport.register(project, variant, testTask, directoryProviders)
                if (!registered) {
                    project.logger.junit5Warn("Jacoco task for variant '${variant.name}' already exists. Disabling customization for JUnit 5...")
                }
            }
        }
    }
}

private fun AndroidJUnitPlatformExtension.configureInstrumentationTests(
    project: Project,
    config: PluginConfig,
    variant: Variant,
) {
    if (!instrumentationTests.enabled.get()) return

    config.instrumentationTestVariantOf(variant)?.let { instrumentationTestVariant ->
        AndroidJUnit5WriteFilters.register(project, variant, instrumentationTestVariant)
    }
}
