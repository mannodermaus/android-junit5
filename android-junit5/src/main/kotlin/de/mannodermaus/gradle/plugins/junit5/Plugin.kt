package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.unit.AndroidJUnit5Test
import de.mannodermaus.gradle.plugins.junit5.tasks.jacoco.AndroidJUnit5JacocoExtension
import de.mannodermaus.gradle.plugins.junit5.tasks.jacoco.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.kotlin.AndroidJUnit5CopyKotlin
import de.mannodermaus.gradle.plugins.junit5.Interop.createExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.PackagesExtension
import org.junit.platform.gradle.plugin.SelectorsExtension
import org.junit.platform.gradle.plugin.TagsExtension
import java.util.Properties

/* Configuration Constants */

private const val MIN_REQUIRED_GRADLE_VERSION = "2.5"
private const val VERSIONS_RESOURCE_NAME = "versions.properties"

private const val EXTENSION_NAME = "junitPlatform"
private const val DEP_CONFIGURATION_NAME = "junitPlatform"
private const val SELECTORS_EXTENSION_NAME = "selectors"
private const val FILTERS_EXTENSION_NAME = "filters"
private const val PACKAGES_EXTENSION_NAME = "packages"
private const val TAGS_EXTENSION_NAME = "tags"
private const val ENGINES_EXTENSION_NAME = "engines"
private const val JACOCO_EXTENSION_NAME = "jacoco"

// Mirrored from "versions.properties" resource file
private const val ANDROID_JUNIT5_VERSION_PROP = "androidJunit5Version"
private const val JUNIT_PLATFORM_VERSION_PROP = "junitPlatformVersion"
private const val JUNIT_JUPITER_VERSION_PROP = "junitJupiterVersion"
private const val JUNIT_VINTAGE_VERSION_PROP = "junitVintageVersion"
private const val JUNIT4_VERSION_PROP = "junit4Version"

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

  @Suppress("UNUSED_VARIABLE")
  private fun Project.configureExtensions() {
    /*
     * junitPlatform {
     *  selectors {
     *  }
     *  filters {
     *    packages {
     *    }
     *    tags {
     *    }
     *    engines {
     *    }
     *  }
     *  jacoco {
     *  }
     * }
     */
    // TODO DSL-ify?
    val junitPlatform = createExtension(this.extensions, EXTENSION_NAME,
        AndroidJUnitPlatformExtension::class.java)
    val selectors = createExtension(junitPlatform, SELECTORS_EXTENSION_NAME,
        SelectorsExtension::class.java)
    val filters = createExtension(junitPlatform, FILTERS_EXTENSION_NAME,
        FILTERS_EXTENSION_NAME::class.java)
    val packages = createExtension(filters, PACKAGES_EXTENSION_NAME, PackagesExtension::class.java)
    val tags = createExtension(filters, TAGS_EXTENSION_NAME, TagsExtension::class.java)
    val engines = createExtension(filters, ENGINES_EXTENSION_NAME, EnginesExtension::class.java)
    val jacoco = createExtension(junitPlatform, JACOCO_EXTENSION_NAME,
        AndroidJUnit5JacocoExtension::class.java)
  }

  private fun Project.configureDependencies() {
    // If no explicit dependency versions are given,
    // read the default values from a configuration file.
    val extension = this.extensions.getByName(EXTENSION_NAME) as AndroidJUnitPlatformExtension
    val deps = loadProperties(VERSIONS_RESOURCE_NAME).extractVersions(this, extension)

    // Create a custom dependency configuration
    val configuration = project.configurations.maybeCreate(DEP_CONFIGURATION_NAME)
    configuration.defaultDependencies {
      // By default, include both the Jupiter & Vintage TestEngines
      // as well as the Launcher-related dependencies on the runtime classpath
      it.addAll(arrayOf(
          deps.platform.launcher,
          deps.platform.console,
          deps.jupiter.engine,
          deps.vintage.engine
      ))

      // Create the dependency handlers for JUnit 5
      project.dependencies.ext["junit5"] = Callable {
        arrayOf(
            deps.junit4,
            deps.jupiter.api,
            deps.platform.engine,
            deps.jupiter.engine,
            deps.vintage.engine,

            // Only needed to run tests in an Android Studio that bundles an older version
            // (see also http://junit.org/junit5/docs/current/user-guide/#running-tests-ide-intellij-idea)
            deps.platform.launcher,
            deps.platform.console
        )
      }

      project.dependencies.ext["junit5Params"] = Callable {
        deps.jupiter.params
      }

      project.dependencies.ext["junit5EmbeddedRuntime"] = Callable {
        deps.android.embeddedRuntime
      }
    }
  }

  private fun Properties.extractVersions(project: Project,
      extension: AndroidJUnitPlatformExtension) = Versions(
      project = project,
      jupiterVersion = extension.jupiterVersion ?: getProperty(JUNIT_JUPITER_VERSION_PROP),
      platformVersion = extension.platformVersion ?: getProperty(JUNIT_PLATFORM_VERSION_PROP),
      vintageVersion = extension.vintageVersion ?: getProperty(JUNIT_VINTAGE_VERSION_PROP),
      junit4Version = getProperty(JUNIT4_VERSION_PROP),
      androidJunit5Version = getProperty(ANDROID_JUNIT5_VERSION_PROP))

  private fun Project.configureTasks() {
    // Add the test task to each of the project's unit test variants,
    // and connect a Code Coverage report to it if Jacoco is enabled.
    val testVariants = projectConfig.unitTestVariants
    val isJacocoApplied = projectConfig.jacocoPluginApplied
    val isKotlinApplied = projectConfig.kotlinPluginApplied

    testVariants.forEach { variant ->
      val testTask = AndroidJUnit5Test.create(this, variant)

      if (isJacocoApplied) {
        AndroidJUnit5JacocoReport.create(this, testTask)
      }

      if (isKotlinApplied) {
        AndroidJUnit5CopyKotlin.create(this, testTask)
      }
    }
  }
}
