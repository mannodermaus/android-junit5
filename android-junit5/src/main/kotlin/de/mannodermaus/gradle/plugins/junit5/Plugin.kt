package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5CopyKotlin
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.PackagesExtension
import org.junit.platform.gradle.plugin.SelectorsExtension
import org.junit.platform.gradle.plugin.TagsExtension

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

    // Validates that the project's plugins are configured correctly
    this.projectConfig = ProjectConfig(project)

    project.configureExtensions()
    project.configureDependencies()
    project.afterEvaluate { it.configureTasks() }
  }

  private fun Project.configureExtensions() {
    createExtension<AndroidJUnitPlatformExtension>(EXTENSION_NAME, arrayOf(this)) {
      createExtension<SelectorsExtension>(SELECTORS_EXTENSION_NAME)
      createExtension<FiltersExtension>(FILTERS_EXTENSION_NAME) {
        createExtension<PackagesExtension>(PACKAGES_EXTENSION_NAME)
        createExtension<TagsExtension>(TAGS_EXTENSION_NAME)
        createExtension<EnginesExtension>(ENGINES_EXTENSION_NAME)
      }
      createExtension<AndroidJUnit5JacocoReport.Extension>(JACOCO_EXTENSION_NAME)
    }
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
      withDependencies(defaults) {
        config.addAll(listOf(
            it.platform.launcher,
            it.platform.console
        ))
      }
    }

    // Create the dependency handlers for JUnit 5
    project.dependencies.ext["junit5"] = Callable {
      withDependencies(defaults) {
        listOf(
            it.others.junit4,
            it.jupiter.api,
            it.platform.engine,
            it.jupiter.engine,
            it.vintage.engine,

            // Only needed to run tests in an Android Studio that bundles an older version
            // (see also http://junit.org/junit5/docs/current/user-guide/#running-tests-ide-intellij-idea)
            it.platform.launcher,
            it.platform.console
        )
      }
    }

    project.dependencies.ext["junit5Params"] = Callable {
      withDependencies(defaults) { it.jupiter.params }
    }

    project.dependencies.ext["junit5EmbeddedRuntime"] = Callable {
      withDependencies(defaults) { it.others.embeddedRuntime }
    }
  }

  private fun Project.configureTasks() {
    // Add the test task to each of the project's unit test variants,
    // and connect a Code Coverage report to it if Jacoco is enabled.
    val testVariants = projectConfig.unitTestVariants
    val isJacocoApplied = projectConfig.jacocoPluginApplied
    val isKotlinApplied = projectConfig.kotlinPluginApplied

    testVariants.forEach { variant ->
      val testTask = AndroidJUnit5UnitTest.create(this, variant)

      if (isJacocoApplied) {
        AndroidJUnit5JacocoReport.create(this, testTask)
      }

      if (isKotlinApplied) {
        AndroidJUnit5CopyKotlin.create(this, testTask)
      }
    }
  }
}
