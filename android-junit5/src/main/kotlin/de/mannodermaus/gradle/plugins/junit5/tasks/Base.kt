package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import de.mannodermaus.gradle.plugins.junit5.variantData
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.process.JavaForkOptions
import org.gradle.process.ProcessForkOptions

/**
 * Base for test tasks. Unlike "JUnit5UnitTest",
 * this is only overridden by actual variant-aware tasks,
 * not the "Run All" task.
 */
interface JUnit5Task : Task {
  fun hasPackageInclude(name: String): Boolean
  fun hasPackageExclude(name: String): Boolean
  fun hasTagInclude(tag: String): Boolean
  fun hasTagExclude(tag: String): Boolean
  fun hasEngineInclude(name: String): Boolean
  fun hasEngineExclude(name: String): Boolean
}

/**
 * Base for tasks added by plugin, shared between
 * the "Run All" task and its individual variant-aware ones.
 */
interface JUnit5UnitTest : Task {
  /**
   * Whether or not this is the "Run All" JUnit 5 task. It doesn't over the entire JavaForkOptions API,
   * and instead will only be a facade.
   * @return True if "Run All" task, false if individual test task
   */
  val isRunAllTask: Boolean

  /**
   * Access the underlying JavaForkOptions, if present on this task.
   * @return The task's JavaForkOptions, if any
   */
  val javaForkOptions: JavaForkOptions?

  /*
   * Subset of JavaForkOptions API, shared between Unit Tests and "Run All" task,
   * so they can be configured together using unitTests.all {}.
   *
   * If a user needs to find out whether or not the "Run All" task is being processed or not,
   * use the "isRunAllTask" API.
  */

  /**
   * Adds the provided arguments to the Java process executing the tests.
   * Does nothing if specified on the "Run All" task.
   * @return Self
   */
  fun jvmArgs(vararg args: Any): JavaForkOptions

  /**
   * Adds the provided system property to the Java process executing the tests.
   * Does nothing if specified on the "Run All" task.
   * @return Self
   */
  fun systemProperty(key: String, value: Any?): JavaForkOptions

  /**
   * Adds the provided environment variable to the Java process executing the tests.
   * Does nothing if specified on the "Run All" task.
   * @return Self
   */
  fun environment(key: String, value: Any?): ProcessForkOptions
}

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
}
