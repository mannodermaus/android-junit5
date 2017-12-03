package de.mannodermaus.gradle.plugins.junit5.tasks

import de.mannodermaus.gradle.plugins.junit5.jacoco
import de.mannodermaus.gradle.plugins.junit5.junit5
import de.mannodermaus.gradle.plugins.junit5.junit5Info
import de.mannodermaus.gradle.plugins.junit5.maybeCreate
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.mainClassDirectories
import de.mannodermaus.gradle.plugins.junit5.providers.mainSourceDirectories
import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport

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

      // Task-level Configuration
      val taskJacoco = testTask.jacoco
      reportTask.executionData = project.files(taskJacoco.destinationFile.path)
      reportTask.classDirectories = project.files(directoryProviders.mainClassDirectories())
      reportTask.sourceDirectories = project.files(directoryProviders.mainSourceDirectories())

      // Apply JUnit 5 configuration parameters
      val junit5Jacoco = project.junit5.jacoco
      val allReports = listOf(
          junit5Jacoco.csv to reportTask.reports.csv,
          junit5Jacoco.xml to reportTask.reports.xml,
          junit5Jacoco.html to reportTask.reports.html)

      allReports.forEach { (from, to) ->
        to.isEnabled = from.isEnabled
        from.destination?.let { to.destination = it }
      }

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
  }
}
