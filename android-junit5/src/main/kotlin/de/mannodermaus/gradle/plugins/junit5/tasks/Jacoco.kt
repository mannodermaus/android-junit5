package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.*
import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import de.mannodermaus.gradle.plugins.junit5.internal.junit5Info
import de.mannodermaus.gradle.plugins.junit5.internal.maybeCreate
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.mainClassDirectories
import de.mannodermaus.gradle.plugins.junit5.providers.mainSourceDirectories
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

private const val TASK_NAME_DEFAULT = "jacocoTestReport"
private const val GROUP_REPORTING = "reporting"

/**
 * Jacoco Test Reporting Task connected to a variant-aware JUnit 5 task.
 * Required to be "open" in order for Groovy's proxy magic to do its thing.
 */
@Suppress("MemberVisibilityCanPrivate")
open class AndroidJUnit5JacocoReport : JacocoReport() {

  companion object {
    fun create(project: Project,
               variant: BaseVariant,
               testTask: Test,
               directoryProviders: Collection<DirectoryProvider>): AndroidJUnit5JacocoReport {
      val configAction = ConfigAction(project, variant, testTask, directoryProviders)
      return project.tasks.create(configAction.name, configAction.type) {
        configAction.execute(it)
      }
    }
  }

  /*
   * Gradle 5.0 changed the return type of these methods from FileCollection to ConfigurableFileCollection.
   * By explicitly re-declaring them here with the old return type, the binary incompatibility to Gradle 4.x is bridged.
   */

  @Suppress("RedundantOverride", "USELESS_CAST")
  override fun getExecutionData(): ConfigurableFileCollection? {
    return super.getExecutionData() as? ConfigurableFileCollection
  }

  @Suppress("RedundantOverride", "USELESS_CAST")
  override fun getClassDirectories(): ConfigurableFileCollection? {
    return super.getClassDirectories() as? ConfigurableFileCollection
  }

  @Suppress("RedundantOverride", "USELESS_CAST")
  override fun getSourceDirectories(): ConfigurableFileCollection? {
    return super.getSourceDirectories() as? ConfigurableFileCollection
  }

  /**
   * Configuration closure for an Android JUnit5 Jacoco Report task.
   */
  private class ConfigAction(
      val project: Project,
      val variant: BaseVariant,
      val testTask: Test,
      private val directoryProviders: Collection<DirectoryProvider>
  ) {

    private val scope = variant.variantData.scope

    val name: String = scope.getTaskName(TASK_NAME_DEFAULT)

    val type = AndroidJUnit5JacocoReport::class.java

    fun execute(reportTask: AndroidJUnit5JacocoReport) {
      // Project-level configuration
      reportTask.dependsOn(testTask)
      reportTask.group = GROUP_REPORTING
      reportTask.description = "Generates Jacoco coverage reports " +
          "for the ${variant.name.capitalize()} variant."

      // Apply JUnit 5 configuration parameters
      val junit5Jacoco = project.android.testOptions.junitPlatform.jacocoOptions
      val allReports = listOf(
          junit5Jacoco.csv to reportTask.reports.csv,
          junit5Jacoco.xml to reportTask.reports.xml,
          junit5Jacoco.html to reportTask.reports.html)

      allReports.forEach { (from, to) ->
        to.isEnabled = from.enabled
        from.destination?.let { to.destination = it }
      }

      // Task-level Configuration
      val taskJacoco = testTask.extensionByName<JacocoTaskExtension>("jacoco")
      taskJacoco.destinationFile?.let {
        reportTask.safeExecutionDataSetFrom(project, it.path)
      }

      // Apply exclusion rules to both class & source directories for Jacoco,
      // using the sum of all DirectoryProviders' outputs as a foundation:
      reportTask.safeClassDirectoriesSetFrom(project,
          directoryProviders.mainClassDirectories().toFileCollectionExcluding(junit5Jacoco.excludedClasses))
      reportTask.safeSourceDirectoriesSetFrom(project,
          directoryProviders.mainSourceDirectories())

      project.logger.junit5Info(
          "Assembled Jacoco Code Coverage for JUnit 5 Task '${testTask.name}':")
      project.logger.junit5Info("|__ Execution Data: ${reportTask.executionData?.asPath}")
      project.logger.junit5Info("|__ Source Dirs: ${reportTask.sourceDirectories?.asPath}")
      project.logger.junit5Info("|__ Class Dirs: ${reportTask.classDirectories?.asPath}")

      // Hook into the main Jacoco task
      val defaultJacocoTask = project.tasks.maybeCreate(
          name = TASK_NAME_DEFAULT,
          group = GROUP_REPORTING)
      defaultJacocoTask.dependsOn(reportTask)
    }

    /* Extension Functions */

    /**
     * Joins the given collection of Files together, while
     * ignoring the provided patterns in the resulting FileCollection.
     */
    private fun Iterable<File>.toFileCollectionExcluding(
        patterns: Iterable<String>): FileCollection = this
        // Convert each directory to a Gradle FileTree, excluding the specified patterns
        .map { project.fileTree(it).exclude(patterns) }
        // Convert the resulting list of FileTree objects into a single FileCollection
        .run { project.files(this) }
  }
}
