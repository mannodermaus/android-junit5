package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.*
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.KotlinDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import org.gradle.api.Plugin
import org.gradle.api.Project

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

    project.afterEvaluate {
      it.evaluateDsl()
      it.configureTestTasks()
      it.configureJacocoTasks()
    }
  }

  private fun Project.configureExtensions() {
    // Hook the JUnit Platform configuration into the Android testOptions
    attachDsl(this, projectConfig)
  }

  private fun Project.evaluateDsl() {
    evaluateDsl(this)
  }

  private fun Project.configureTestTasks() {
    // Configure JUnit 5 for each variant-specific test task
    projectConfig.unitTestVariants.all { variant ->
      val testTask = tasks.testTaskOf(variant)
      val configuration = createJUnit5ConfigurationFor(variant)

      testTask.useJUnitPlatform { options ->
        options.includeTags(*configuration.combinedIncludeTags)
        options.excludeTags(*configuration.combinedExcludeTags)
        options.includeEngines(*configuration.combinedIncludeEngines)
        options.excludeEngines(*configuration.combinedExcludeEngines)
      }

      testTask.include(*configuration.combinedIncludePatterns)
      testTask.exclude(*configuration.combinedExcludePatterns)

      // From the User Guide:
      // "The standard Gradle test task currently does not provide a dedicated DSL
      // to set JUnit Platform configuration parameters to influence test discovery and execution.
      // However, you can provide configuration parameters within the build script via system properties"
      val junit5 = android.testOptions.junitPlatform
      testTask.systemProperties(junit5.configurationParameters)
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
        val testTask = tasks.testTaskOf(variant)

        // Create a Jacoco friend task
        val enabledVariants = jacocoOptions.onlyGenerateTasksForVariants
        if (enabledVariants.isEmpty() || enabledVariants.contains(variant.name)) {
          val jacocoTask = AndroidJUnit5JacocoReport.create(this, variant, testTask, directoryProviders)
          if (jacocoTask == null) {
            project.logger.junit5Warn("Jacoco task for variant '${variant.name}' already exists. Disabling customization for JUnit 5...")
          }
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
}
