package de.mannodermaus.gradle.plugins.android_junit5.kotlin

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import de.mannodermaus.gradle.plugins.android_junit5.AndroidJUnit5Test
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Copy

class AndroidJUnit5CopyKotlin extends Copy {

  static AndroidJUnit5CopyKotlin create(Project project, AndroidJUnit5Test testTask) {
    def configAction = new ConfigAction(project, testTask)
    return project.tasks.create(configAction.getName(), configAction.getType(), configAction)
  }

  static class ConfigAction implements TaskConfigAction<AndroidJUnit5CopyKotlin> {

    private static final String TASK_NAME_DEFAULT = "copyKotlinUnitTestClasses"
    private static final String TASK_GROUP = JavaBasePlugin.VERIFICATION_GROUP

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
    Class<AndroidJUnit5CopyKotlin> getType() {
      return AndroidJUnit5CopyKotlin.class
    }

    @Override
    void execute(AndroidJUnit5CopyKotlin copyTask) {
      copyTask.from "$project.buildDir/tmp/kotlin-classes/${variant.name}UnitTest"
      copyTask.into "$project.buildDir/intermediates/classes/test/$variant.name"
      copyTask.group = TASK_GROUP

      testTask.dependsOn copyTask
    }
  }
}
