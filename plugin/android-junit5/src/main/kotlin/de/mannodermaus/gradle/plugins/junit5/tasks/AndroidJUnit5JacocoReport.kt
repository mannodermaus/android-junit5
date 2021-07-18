package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import de.mannodermaus.gradle.plugins.junit5.*
import de.mannodermaus.gradle.plugins.junit5.internal.*
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.mainClassDirectories
import de.mannodermaus.gradle.plugins.junit5.providers.mainSourceDirectories
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

const val JACOCO_TASK_NAME = "jacocoTestReport"
private const val GROUP_REPORTING = "reporting"

/**
 * Jacoco Test Reporting Task connected to a variant-aware JUnit 5 task.
 * Required to be "open" in order for Groovy's proxy magic to do its thing.
 */
@Suppress("MemberVisibilityCanPrivate")
@CacheableTask
open class AndroidJUnit5JacocoReport : JacocoReport() {

  companion object {
    fun register(
      project: Project,
      variant: BaseVariant,
      testTask: Test,
      directoryProviders: Collection<DirectoryProvider>
    ): Boolean {
      val configAction = ConfigAction(project, variant, testTask, directoryProviders)
      if (project.tasks.namedOrNull<Task>(configAction.name) != null) {
        // Already exists; abort
        return false
      }

      val provider = project.tasks.register(
        configAction.name,
        configAction.type,
        configAction::execute
      )

      // Hook the task into the build chain
      provider.dependsOn(testTask.name)
      findOrRegisterDefaultJacocoTask(project).dependsOn(provider)

      return true
    }

    private fun findOrRegisterDefaultJacocoTask(project: Project): TaskProvider<Task> =
      project.tasks.namedOrNull(JACOCO_TASK_NAME)
        ?: project.tasks.register(JACOCO_TASK_NAME) { task ->
          task.group = GROUP_REPORTING
        }
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

    val name: String = variant.getTaskName(prefix = JACOCO_TASK_NAME)

    val type = AndroidJUnit5JacocoReport::class.java

    fun execute(reportTask: AndroidJUnit5JacocoReport) {
      // Project-level configuration
      reportTask.dependsOn(testTask)
      reportTask.group = GROUP_REPORTING
      reportTask.description = "Generates Jacoco coverage reports " +
          "for the ${variant.name.capitalize()} variant."

      // Apply JUnit 5 configuration parameters
      val junit5Jacoco = project.junitPlatform.jacocoOptions
      val allReports = listOf(
        junit5Jacoco.csv to reportTask.reports.csv,
        junit5Jacoco.xml to reportTask.reports.xml,
        junit5Jacoco.html to reportTask.reports.html
      )

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
      reportTask.safeClassDirectoriesSetFrom(
        project,
        directoryProviders.mainClassDirectories().toFileCollectionExcluding(junit5Jacoco.excludedClasses)
      )
      reportTask.safeSourceDirectoriesSetFrom(
        project,
        directoryProviders.mainSourceDirectories()
      )

      project.logger.junit5Info(
        "Assembled Jacoco Code Coverage for JUnit 5 Task '${testTask.name}':"
      )
      project.logger.junit5Info("|__ Execution Data: ${reportTask.safeGetExecutionData?.asPath}")
      project.logger.junit5Info("|__ Source Dirs: ${reportTask.safeGetSourceDirectories?.asPath}")
      project.logger.junit5Info("|__ Class Dirs: ${reportTask.safeGetClassDirectories?.asPath}")
    }

    /* Extension Functions */

    /**
     * Joins the given collection of Files together, while
     * ignoring the provided patterns in the resulting FileCollection.
     */
    private fun Iterable<File>.toFileCollectionExcluding(
      patterns: Iterable<String>
    ): FileCollection = this
      // Convert each directory to a Gradle FileTree, excluding the specified patterns
      .map { project.fileTree(it).exclude(patterns) }
      // Convert the resulting list of FileTree objects into a single FileCollection
      .run { project.files(this) }
  }
}
