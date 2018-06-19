package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationKind.ANDROID_TEST
import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationScope.RUNTIME_ONLY
import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.append
import de.mannodermaus.gradle.plugins.junit5.internal.find
import de.mannodermaus.gradle.plugins.junit5.internal.loadProperties
import de.mannodermaus.gradle.plugins.junit5.internal.requireGradle
import de.mannodermaus.gradle.plugins.junit5.internal.requireVersion
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.KotlinDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import org.gradle.api.Plugin
import org.gradle.api.Project
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

    requireVersion(
        actual = ANDROID_GRADLE_PLUGIN_VERSION,
        required = MIN_REQUIRED_AGP_VERSION) {
      "android-junit5 plugin requires Android Gradle Plugin $MIN_REQUIRED_AGP_VERSION or later"
    }

    // Validates that the project's plugins are configured correctly
    this.projectConfig = ProjectConfig(project)

    project.configureExtensions()
    project.configureDependencies()

    project.afterEvaluate {
      it.configureTestTasks()
      it.configureJacocoTasks()
      it.applyEvaluatedConfiguration()
    }
  }

  private fun Project.configureExtensions() {
    // Hook the JUnit Platform configuration into the Android testOptions
    attachDsl(this)
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

    // For instrumentation tests, attach the JUnit 5 RunnerBuilder automatically
    // to the test instrumentation runner's parameters
    // (runtime dependency is being added after evaluation, though)
    val runnerArgs = this.android.defaultConfig.testInstrumentationRunnerArguments
    runnerArgs.append(RUNNER_BUILDER_ARG, JUNIT5_RUNNER_BUILDER_CLASS_NAME)
  }

  private fun Project.configureTestTasks() {
    // Add the test task to each of the project's unit test variants
    projectConfig.unitTestVariants.all { variant ->
      val directoryProviders = collectDirectoryProviders(variant)
      AndroidJUnit5UnitTest.create(this, variant, directoryProviders)
    }
  }

  /* After evaluate */

  private fun Project.configureJacocoTasks() {
    // Connect a Code Coverage report to it if Jacoco is enabled.
    val isJacocoApplied = projectConfig.jacocoPluginApplied
    val jacocoOptions = this.android.testOptions.junitPlatform.jacocoOptions

    if (isJacocoApplied && jacocoOptions.taskGenerationEnabled) {
      projectConfig.unitTestVariants.all { variant ->
        val directoryProviders = collectDirectoryProviders(variant)
        val testTask = AndroidJUnit5UnitTest.find(project, variant)

        // Create a Jacoco friend task
        val enabledVariants = jacocoOptions.onlyGenerateTasksForVariants
        if (enabledVariants.isEmpty() || enabledVariants.contains(variant.name)) {
          AndroidJUnit5JacocoReport.create(this, testTask, directoryProviders)
        }
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

  private fun Project.applyEvaluatedConfiguration() {
    // Verify that the JUnit 5 RunnerBuilder wasn't overwritten by user code,
    // and if so, throw an exception
    val actualRunnerBuilder = android.defaultConfig.testInstrumentationRunnerArguments[RUNNER_BUILDER_ARG]!!
    if (!actualRunnerBuilder.contains(JUNIT5_RUNNER_BUILDER_CLASS_NAME)) {
      throw IllegalArgumentException(
          "Custom runnerBuilder is overwriting JUnit 5 integration! " +
              "Change your declaration to '$actualRunnerBuilder,$JUNIT5_RUNNER_BUILDER_CLASS_NAME'.")
    }

    // Attach runtime-only dependency on JUnit 5 instrumentation test facade, unless disabled
    val defaults = loadProperties(VERSIONS_RESOURCE_NAME)
    val rtOnly = configurations.find(kind = ANDROID_TEST, scope = RUNTIME_ONLY)
    withLoadedVersions(defaults) {
      rtOnly.dependencies.add(it.others.instrumentationRunner)
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
        extension = project.android.testOptions.junitPlatform,
        defaults = defaults)
    return config(versions)
  }

}
