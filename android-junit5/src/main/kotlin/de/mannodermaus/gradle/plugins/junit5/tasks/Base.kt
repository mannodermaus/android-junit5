package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.EXTENSION_NAME
import de.mannodermaus.gradle.plugins.junit5.extensionByName
import de.mannodermaus.gradle.plugins.junit5.variantData
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Base for ConfigActions related to custom plugin tasks
 * that connect to an [AndroidJUnit5UnitTest] in some shape or form.
 */
abstract class JUnit5TaskConfigAction<T : Task>(
    protected val project: Project,
    protected val testTask: AndroidJUnit5UnitTest
) : TaskConfigAction<T> {

  protected val variant: BaseVariant = testTask.variant
  protected val scope: VariantScope = variant.variantData.scope
  protected val junit5 = project.extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)
}
