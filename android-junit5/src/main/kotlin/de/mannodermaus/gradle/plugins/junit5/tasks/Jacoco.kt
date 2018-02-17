package de.mannodermaus.gradle.plugins.junit5.tasks

import de.mannodermaus.gradle.plugins.junit5.jacoco
import de.mannodermaus.gradle.plugins.junit5.junit5
import de.mannodermaus.gradle.plugins.junit5.junit5Info
import de.mannodermaus.gradle.plugins.junit5.maybeCreate
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.mainClassDirectories
import de.mannodermaus.gradle.plugins.junit5.providers.mainSourceDirectories
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
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
        testTask: AndroidJUnit5UnitTest,
        directoryProviders: Collection<DirectoryProvider>): AndroidJUnit5JacocoReport {
      val configAction = ConfigAction(project, testTask, directoryProviders)
      return project.tasks.create(configAction.name, configAction.type, configAction)
    }
  }

  /**
   * Configuration closure for an Android JUnit5 Jacoco Report task.
   */
  private class ConfigAction(
      project: Project,
      testTask: AndroidJUnit5UnitTest,
      private val directoryProviders: Collection<DirectoryProvider>
  ) : JUnit5TaskConfigAction<AndroidJUnit5JacocoReport>(project, testTask) {

    override fun getName(): String = scope.getTaskName(TASK_NAME_DEFAULT)

    override fun getType() = AndroidJUnit5JacocoReport::class.java

    override fun execute(reportTask: AndroidJUnit5JacocoReport) {
      // Project-level configuration
      val projectJacoco = project.jacoco
      projectJacoco.applyTo(testTask)
      reportTask.dependsOn(testTask)
      reportTask.group = GROUP_REPORTING
      reportTask.description = "Generates Jacoco coverage reports " +
          "for the ${variant.name.capitalize()} variant."

      // Apply JUnit 5 configuration parameters
      val junit5Jacoco = project.junit5.jacocoOptions
      val allReports = listOf(
          junit5Jacoco.csv to reportTask.reports.csv,
          junit5Jacoco.xml to reportTask.reports.xml,
          junit5Jacoco.html to reportTask.reports.html)

      allReports.forEach { (from, to) ->
        to.isEnabled = from.isEnabled
        from.destination?.let { to.destination = it }
      }

      // Task-level Configuration
      val taskJacoco = testTask.jacoco
      reportTask.executionData = project.files(taskJacoco.destinationFile.path)

      // Apply exclusion rules to both class & source directories for Jacoco,
      // using the sum of all DirectoryProviders' outputs as a foundation:
      reportTask.classDirectories = directoryProviders.mainClassDirectories()
          .toFileCollectionExcluding(junit5Jacoco.excludedClasses)
      reportTask.sourceDirectories = directoryProviders.mainSourceDirectories()
          .toFileCollectionExcluding(junit5Jacoco.excludedSources)

      project.logger.junit5Info(
          "Assembled Jacoco Code Coverage for JUnit 5 Task '${testTask.name}':")
      project.logger.junit5Info("|__ Execution Data: ${reportTask.executionData.asPath}")
      project.logger.junit5Info("|__ Source Dirs: ${reportTask.sourceDirectories.asPath}")
      project.logger.junit5Info("|__ Class Dirs: ${reportTask.classDirectories.asPath}")

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
