package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.ConfigurationKind.ANDROID_TEST
import de.mannodermaus.gradle.plugins.junit5.ConfigurationScope.RUNTIME_ONLY
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.KotlinDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.PackagesExtension
import org.junit.platform.gradle.plugin.SelectorsExtension
import org.junit.platform.gradle.plugin.TagsExtension
import java.util.Properties

/**
 * Android JUnit Platform plugin for Gradle.
 * Configures JUnit 5 tasks on all unit-tested variants of an Android project.
 */
class AndroidJUnitPlatformPlugin : Plugin<Project> {

  private lateinit var projectConfig: ProjectConfig

  override fun apply(project: Project) {
    requireGradle(MIN_REQUIRED_GRADLE_VERSION) {
      "android-junit5 plugin requires Gradle $MIN_REQUIRED_GRADLE_VERSION or later"
    }

    requireAgp3 {
      "android-junit5 plugin requires Android Gradle Plugin 3.0.0 or later"
    }

    // Validates that the project's plugins are configured correctly
    this.projectConfig = ProjectConfig(project)

    project.configureExtensions()
    project.configureDependencies()
    project.afterEvaluate {
      it.configureTasks()
      it.applyConfigurationParameters()
    }
  }

  private fun Project.configureExtensions() {
    // Hook the JUnit Platform configuration into the Android testOptions
    android.testOptions
        .extend<AndroidJUnitPlatformExtension>(EXTENSION_NAME, arrayOf(this)) { ju5 ->
          ju5.extend<SelectorsExtension>(SELECTORS_EXTENSION_NAME)
          ju5.extend<FiltersExtension>(FILTERS_EXTENSION_NAME) { filters ->
            filters.extend<PackagesExtension>(PACKAGES_EXTENSION_NAME)
            filters.extend<TagsExtension>(TAGS_EXTENSION_NAME)
            filters.extend<EnginesExtension>(ENGINES_EXTENSION_NAME)
          }
        }

    // FIXME Deprecated --------------------------------------------------------------------------------
    // For backwards compatibility, still offer the "old" entry point "project.junitPlatform",
    // which should redirect to the testOptions-based DSL dynamically
    this.extend<ExtensionProxy>(EXTENSION_NAME, arrayOf(this, this.junit5))
    // END Deprecation  --------------------------------------------------------------------------------
  }

  private fun Project.configureDependencies() {
    // If no explicit dependency versions are given,
    // read the default values from a configuration file.
    val defaults = loadProperties(VERSIONS_RESOURCE_NAME)

    // Create a custom dependency configuration
    val configuration = project.configurations.maybeCreate(DEP_CONFIGURATION_NAME)
    configuration.defaultDependencies { config ->
      // By default, include both the Jupiter & Vintage TestEngines
      // as well as the Launcher-related dependencies on the runtime classpath
      withLoadedVersions(defaults) {
        config.addAll(listOf(
            it.platform.launcher,
            it.platform.console
        ))
      }
    }

    // Create the custom dependency endpoints for JUnit 5
    val dependencyHandler = JUnit5DependencyHandler(this, defaults)
    dependencyHandler.configure()
  }

  private fun Project.configureTasks() {
    // Add the test task to each of the project's unit test variants,
    // and connect a Code Coverage report to it if Jacoco is enabled.
    val testVariants = projectConfig.unitTestVariants
    val isJacocoApplied = projectConfig.jacocoPluginApplied

    testVariants.all { variant ->
      val directoryProviders = collectDirectoryProviders(variant)

      // Create JUnit 5 test task
      val testTask = AndroidJUnit5UnitTest.create(this, variant, directoryProviders)

      if (isJacocoApplied) {
        // Create a Jacoco friend task
        AndroidJUnit5JacocoReport.create(this, testTask, directoryProviders)
      }
    }
  }

  private fun Project.collectDirectoryProviders(
      variant: BaseVariant): Collection<DirectoryProvider> {
    val providers = mutableSetOf<DirectoryProvider>()

    // Default Java directories
    providers += JavaDirectoryProvider(variant)

    // Kotlin Integration
    if (projectConfig.kotlinPluginApplied) {
      providers += KotlinDirectoryProvider(this, variant)
    }

    return providers
  }

  private fun Project.applyConfigurationParameters() {
    // Consume Instrumentation Test options &
    // apply configuration if enabled
    if (junit5.instrumentationTests.enabled) {
      // Attach the JUnit 5 RunnerBuilder automatically
      // to the test instrumentation runner's parameters,
      // and attach the runner's artifact automatically to the runtime configuration
      val runnerArgs = android.defaultConfig.testInstrumentationRunnerArguments
      runnerArgs.append(RUNNER_BUILDER_ARG, JUNIT5_RUNNER_BUILDER_CLASS_NAME)

      val defaults = loadProperties(VERSIONS_RESOURCE_NAME)
      val rtOnly = configurations.findConfiguration(kind = ANDROID_TEST, scope = RUNTIME_ONLY)
      withLoadedVersions(defaults) {
        rtOnly.dependencies.add(it.others.instrumentationRunner)
      }
    }
  }

  /**
   * Executes the given block within the context of
   * the plugin's transitive dependencies.
   * This is used in our custom dependency handlers, and is required
   * to be used lazily instead of eagerly. This is motivated by the
   * user's capability to override the versions utilized by the plugin to work.
   * We need to wait until the configuration is evaluated by Gradle before
   * accessing our plugin Extension's parameters.
   */
  private fun Project.withLoadedVersions(defaults: Properties, config: (Versions) -> Any): Any {
    val versions = Versions(
        project = this,
        extension = project.junit5,
        defaults = defaults)
    return config(versions)
  }

}
