package de.mannodermaus.gradle.plugins.junit5.tasks

import de.mannodermaus.gradle.plugins.junit5.Constants
import de.mannodermaus.gradle.plugins.junit5.extensionByName
import de.mannodermaus.gradle.plugins.junit5.logInfo
import de.mannodermaus.gradle.plugins.junit5.maybeCreate
import de.mannodermaus.gradle.plugins.junit5.tasks.unit.AndroidJUnit5Test
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

private const val TASK_NAME_DEFAULT = "jacocoTestReport"
private const val GROUP_REPORTING = "reporting"
private const val JACOCO_PLUGIN_EXT = "jacoco"
private const val JACOCO_TASK_EXT = "jacoco"

/**
 * Jacoco Test Reporting Task connected to a variant-aware JUnit 5 task.
 * Required to be "open" in order for Groovy's proxy magic to do its thing.
 */
open class AndroidJUnit5JacocoReport : JacocoReport() {

  companion object {
    fun create(project: Project, testTask: AndroidJUnit5Test): AndroidJUnit5JacocoReport {
      val configAction = ConfigAction(project, testTask)
      return project.tasks.create(configAction.name, configAction.type, configAction)
    }
  }

  /**
   * Configuration exposed to consumers
   */
  open class Extension {
    /** Generate a test coverage report in CSV */
    var csvReport = true
    /** Generate a test coverage report in XML */
    var xmlReport = true
    /** Generate a test coverage report in HTML */
    var htmlReport = true
  }

  /**
   * Configuration closure for an Android JUnit5 Jacoco Report task.
   */
  private class ConfigAction(
      project: Project,
      testTask: AndroidJUnit5Test
  ) : JUnit5TaskConfigAction<AndroidJUnit5JacocoReport>(project, testTask) {

    override fun getName(): String = scope.getTaskName(TASK_NAME_DEFAULT)

    override fun getType() = AndroidJUnit5JacocoReport::class.java

    override fun execute(reportTask: AndroidJUnit5JacocoReport) {
      // Project-level configuration
      val projectJacoco = project.extensionByName<JacocoPluginExtension>(JACOCO_PLUGIN_EXT)
      projectJacoco.applyTo(testTask)
      reportTask.dependsOn(testTask)
      reportTask.group = GROUP_REPORTING
      reportTask.description = "Generates Jacoco coverage reports for the ${variant.name.capitalize()} variant."

      // Task-level Configuration
      val taskJacoco = testTask.extensionByName<JacocoTaskExtension>(JACOCO_TASK_EXT)
      reportTask.executionData = project.files(taskJacoco.destinationFile.path)
      reportTask.classDirectories = project.files(scope.javaOutputDir)
      reportTask.sourceDirectories = project.files(variant.sourceSets
          .map { it.javaDirectories }
          .flatten()
          .map { it.path })

      // Apply JUnit 5 configuration parameters
      val junit5Jacoco = junit5
          .extensionByName<AndroidJUnit5JacocoReport.Extension>(Constants.JACOCO_EXTENSION_NAME)

      reportTask.reports.apply {
        csv.isEnabled = junit5Jacoco.csvReport
        html.isEnabled = junit5Jacoco.htmlReport
        xml.isEnabled = junit5Jacoco.xmlReport
      }

      project.logInfo("Assembled Jacoco Code Coverage for JUnit 5 Task '$testTask.name':")
      project.logInfo("|__ Execution Data: ${reportTask.executionData.asPath}")
      project.logInfo("|__ Source Dirs: ${reportTask.sourceDirectories.asPath}")
      project.logInfo("|__ Class Dirs: ${reportTask.classDirectories.asPath}")

      // Hook into the main Jacoco task
      val defaultJacocoTask = project.tasks.maybeCreate(
          name = TASK_NAME_DEFAULT,
          group = GROUP_REPORTING)
      defaultJacocoTask.dependsOn(reportTask)
    }
  }
}
