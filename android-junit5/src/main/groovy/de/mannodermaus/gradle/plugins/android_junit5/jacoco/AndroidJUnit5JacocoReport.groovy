package de.mannodermaus.gradle.plugins.android_junit5.jacoco

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import de.mannodermaus.gradle.plugins.android_junit5.AndroidJUnit5Test
import de.mannodermaus.gradle.plugins.android_junit5.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.android_junit5.AndroidJUnitPlatformPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testing.jacoco.tasks.JacocoReport

class AndroidJUnit5JacocoReport extends JacocoReport {

  static AndroidJUnit5JacocoReport create(Project project, AndroidJUnit5Test testTask) {
    def configAction = new ConfigAction(project, testTask)
    return project.tasks.create(configAction.getName(), configAction.getType(), configAction)
  }

  private static String createDescription(BaseVariant variant) {
    return "Generates Jacoco coverage reports for the ${variant.name.capitalize()} variant."
  }

  static class ConfigAction implements TaskConfigAction<AndroidJUnit5JacocoReport> {

    private static final String TASK_NAME_DEFAULT = "jacocoTestReport"
    private static final String TASK_GROUP = "reporting"

    private final Project project
    private final AndroidJUnit5Test testTask
    private final BaseVariant variant
    private final VariantScope scope

    ConfigAction(Project project, AndroidJUnit5Test testTask) {
      this.project = project
      this.testTask = testTask
      this.variant = testTask.variant
      this.scope = variant.variantData.scope
    }

    @Override
    String getName() {
      return scope.getTaskName(TASK_NAME_DEFAULT)
    }

    @Override
    Class<AndroidJUnit5JacocoReport> getType() {
      return AndroidJUnit5JacocoReport.class
    }

    @Override
    void execute(AndroidJUnit5JacocoReport reportTask) {
      // Configure
      project.jacoco.applyTo testTask
      reportTask.dependsOn testTask
      reportTask.group = TASK_GROUP
      reportTask.description = createDescription(variant)

      reportTask.executionData = project.files(testTask.jacoco.destinationFile.path)
      reportTask.classDirectories = project.files(scope.javaOutputDir)
      reportTask.sourceDirectories = project.files(variant.sourceSets.java
          .srcDirs
          .collect { it.path }
          .flatten())

      // Hook in plugin configuration parameters
      def junitExtension = project.extensions.getByName(
          AndroidJUnitPlatformPlugin.EXTENSION_NAME) as AndroidJUnitPlatformExtension
      def jacocoExtension = junitExtension.extensions.getByName(
          AndroidJUnitPlatformPlugin.JACOCO_EXTENSION_NAME) as AndroidJUnit5JacocoExtension

      reportTask.reports {
        csv.enabled jacocoExtension.csvReport
        html.enabled jacocoExtension.htmlReport
        xml.enabled jacocoExtension.xmlReport
      }

      project.logger.info(
          "$AndroidJUnitPlatformPlugin.LOG_TAG: Assembled Jacoco Code Coverage for JUnit 5 Task '$testTask.name':")
      project.logger.info(
          "$AndroidJUnitPlatformPlugin.LOG_TAG: |__ Execution Data: ${reportTask.executionData.asPath}")
      project.logger.info(
          "$AndroidJUnitPlatformPlugin.LOG_TAG: |__ Source Dirs: ${reportTask.sourceDirectories.asPath}")
      project.logger.info(
          "$AndroidJUnitPlatformPlugin.LOG_TAG: |__ Class Dirs: ${reportTask.classDirectories.asPath}")

      // Hook into main Jacoco task
      def defaultJacocoTask = findOrCreateJacocoTask()
      defaultJacocoTask.dependsOn reportTask
    }

    /* Begin private */

    private Task findOrCreateJacocoTask() {
      def task = project.tasks.findByName(TASK_NAME_DEFAULT)
      if (!task) {
        task = project.tasks.create(TASK_NAME_DEFAULT)
        task.group = TASK_GROUP
      }
      return task
    }
  }
}
