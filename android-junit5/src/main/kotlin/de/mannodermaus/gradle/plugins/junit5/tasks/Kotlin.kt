package de.mannodermaus.gradle.plugins.junit5.tasks

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Copy

private const val TASK_NAME_DEFAULT = "copyKotlinUnitTestClasses"
private const val GROUP_VERIFICATION = JavaBasePlugin.VERIFICATION_GROUP

open class AndroidJUnit5CopyKotlin : Copy() {

  companion object {
    fun create(project: Project, testTask: AndroidJUnit5UnitTest): AndroidJUnit5CopyKotlin {
      val configAction = ConfigAction(project, testTask)
      return project.tasks.create(configAction.name, configAction.type, configAction)
    }
  }

  private class ConfigAction(
      project: Project,
      testTask: AndroidJUnit5UnitTest
  ) : JUnit5TaskConfigAction<AndroidJUnit5CopyKotlin>(project, testTask) {

    override fun getName(): String = scope.getTaskName(TASK_NAME_DEFAULT)

    override fun getType() = AndroidJUnit5CopyKotlin::class.java

    override fun execute(copyTask: AndroidJUnit5CopyKotlin) {
      copyTask.from("${project.buildDir}/tmp/kotlin-classes/${variant.name}UnitTest")
      copyTask.into(
          "${project.buildDir}/intermediates/classes/test/${variant.flavorName}/${variant.buildType.name}")
      copyTask.group = GROUP_VERIFICATION
      copyTask.description = "Copies over Kotlin test classes " +
          "for the ${variant.name.capitalize()} variant " +
          "to a location where the IDE can pick them up properly."

      testTask.dependsOn(copyTask)
    }
  }
}
