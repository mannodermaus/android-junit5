package de.mannodermaus.gradle.plugins.junit5

import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.dsl.attachGlobalDsl
import de.mannodermaus.gradle.plugins.junit5.dsl.attachSpecificDsl
import de.mannodermaus.gradle.plugins.junit5.dsl.evaluateExtensions
import de.mannodermaus.gradle.plugins.junit5.internal.config.ANDROID_JUNIT5_RUNNER_BUILDER_CLASS
import de.mannodermaus.gradle.plugins.junit5.internal.config.INSTRUMENTATION_RUNNER_LIBRARY_ARTIFACT
import de.mannodermaus.gradle.plugins.junit5.internal.config.INSTRUMENTATION_RUNNER_LIBRARY_GROUP
import de.mannodermaus.gradle.plugins.junit5.internal.config.JUnit5TaskConfig
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_AGP_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_GRADLE_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.config.PluginConfig
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.getAsList
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.hasAndroidPlugin
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.instrumentationTestVariant
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junit5Warn
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.testTaskOf
import de.mannodermaus.gradle.plugins.junit5.internal.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.providers.KotlinDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.internal.utils.excludedPackagingOptions
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireGradle
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireVersion
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5WriteFilters
import org.gradle.api.GradleException
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
                required = MIN_REQUIRED_AGP_VERSION) {
            "android-junit5 plugin requires Android Gradle Plugin $MIN_REQUIRED_AGP_VERSION or later"
        }

        // Non-specific global extension (i.e. global filters, nothing for variants)
        project.attachGlobalDsl()

        // Await an Android plugin and configure it upon detection
        project.plugins.all { plugin ->
            PluginConfig.find(project, plugin)?.let { config ->
                project.attachSpecificDsl(config)

                // Add default ignore rules for JUnit 5 metadata files to the packaging options of the plugin,
                // so that consumers don't need to do this explicitly
                excludedPackagingOptions().forEach(project.android.packagingOptions::exclude)

                project.afterEvaluate {
                    it.evaluateExtensions()
                    it.configureUnitTests(config)
                    it.configureInstrumentationTests(config)
                    it.configureJacocoTasks(config)
                }
            }
        }

        project.afterEvaluate {
            // If no Android plugin was applied at this point, fail
            if (!it.hasAndroidPlugin()) {
                throw IllegalStateException("An Android plugin must be applied in order for android-junit5 to work correctly!")
            }
        }
    }

    /* Private */

    private fun Project.configureUnitTests(projectConfig: PluginConfig) {
        // Configure JUnit 5 for each variant-specific unit test task,
        // unless that variant has its tests disabled
        projectConfig.variants.all { variant ->
            val testTask = tasks.testTaskOf(variant) ?: return@all
            val configuration = JUnit5TaskConfig(variant, this)

            testTask.configure { task ->
                task.useJUnitPlatform { options ->
                    options.includeTags(*configuration.combinedIncludeTags)
                    options.excludeTags(*configuration.combinedExcludeTags)
                    options.includeEngines(*configuration.combinedIncludeEngines)
                    options.excludeEngines(*configuration.combinedExcludeEngines)
                }

                task.include(*configuration.combinedIncludePatterns)
                task.exclude(*configuration.combinedExcludePatterns)

                // From the User Guide:
                // "The standard Gradle test task currently does not provide a dedicated DSL
                // to set JUnit Platform configuration parameters to influence test discovery and execution.
                // However, you can provide configuration parameters within the build script via system properties"
                task.systemProperties(junitPlatform.configurationParameters)
            }
        }
    }

    private fun Project.configureInstrumentationTests(projectConfig: PluginConfig) {
        // Validate configuration of instrumentation tests, unless this
        // step is deactivated through the DSL.
        //
        // Normally, both of the following statements must be fulfilled for this to work:
        // 1) A special test instrumentation runner argument is applied
        // 2) The test runner library is added
        val hasRunnerBuilder = android.defaultConfig
                .testInstrumentationRunnerArguments
                .getAsList("runnerBuilder")
                .contains(ANDROID_JUNIT5_RUNNER_BUILDER_CLASS)

        val hasDependency = configurations
                .getByName("androidTestRuntimeOnly")
                .dependencies
                .any { it.group == INSTRUMENTATION_RUNNER_LIBRARY_GROUP && it.name == INSTRUMENTATION_RUNNER_LIBRARY_ARTIFACT }

        val extension = project.junitPlatform
        val checkEnabled = extension.instrumentationTests.integrityCheckEnabled

        if (checkEnabled && hasRunnerBuilder xor hasDependency) {
            val missingStep = if (hasRunnerBuilder) {
                "Add the android-test-runner library to the androidTestRuntimeOnly configuration's dependencies"
            } else {
                "Add the JUnit 5 RunnerBuilder to the application's defaultConfig"
            }
            throw GradleException("""Incomplete configuration for JUnit 5 instrumentation tests: $missingStep.
        |Find more information at: https://bit.ly/junit5-instrumentation-tests""".trimMargin())
        }

        if (hasRunnerBuilder) {
            // For each instrumentation test variant,
            // write out an asset file containing the filters applied through the DSL
            projectConfig.variants.all { variant ->
                val instrumentationTestVariant = variant.instrumentationTestVariant
                if (instrumentationTestVariant != null) {
                    // Register a resource generator for the androidTest variant
                    AndroidJUnit5WriteFilters.register(this, instrumentationTestVariant)
                }
            }
        }
    }

    private fun Project.configureJacocoTasks(projectConfig: PluginConfig) {
        // Connect a Code Coverage report to it if Jacoco is enabled.
        val isJacocoApplied = projectConfig.jacocoPluginApplied
        val jacocoOptions = this.junitPlatform.jacocoOptions

        if (isJacocoApplied && jacocoOptions.taskGenerationEnabled) {
            projectConfig.variants.all { variant ->
                val directoryProviders = collectDirectoryProviders(projectConfig, variant)
                val testTask = tasks.testTaskOf(variant) ?: return@all

                // Create a Jacoco friend task
                val enabledVariants = jacocoOptions.onlyGenerateTasksForVariants
                if (enabledVariants.isEmpty() || enabledVariants.contains(variant.name)) {
                    val registered = AndroidJUnit5JacocoReport.register(this, variant, testTask, directoryProviders)
                    if (!registered) {
                        project.logger.junit5Warn("Jacoco task for variant '${variant.name}' already exists. Disabling customization for JUnit 5...")
                    }
                }
            }
        }
    }

    private fun Project.collectDirectoryProviders(
            projectConfig: PluginConfig,
            variant: BaseVariant
    ): Collection<DirectoryProvider> {
        val providers = mutableSetOf<DirectoryProvider>()

        // Default Java directories
        providers += JavaDirectoryProvider(variant)

        // Kotlin Integration
        if (projectConfig.kotlinPluginApplied) {
            providers += KotlinDirectoryProvider(this, variant)
        }

        return providers
    }
}
