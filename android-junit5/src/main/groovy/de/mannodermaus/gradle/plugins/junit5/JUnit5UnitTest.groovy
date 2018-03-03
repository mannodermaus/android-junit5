package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaForkOptions
import org.gradle.process.ProcessForkOptions

/*
 * Marker interface to allow the Kotlin-based tasks
 * to be referenced from Groovy indirectly.
 */

interface JUnit5UnitTest extends Task {

  /**
   * Whether or not this is the "Run All" JUnit 5 task. It doesn't over the entire JavaForkOptions API,
   * and instead will only be a facade.
   * @return True if "Run All" task, false if individual test task
   */
  boolean isRunAllTask()

  /**
   * Access the underlying JavaForkOptions, or return an empty Optional if no such options are
   * available to this task (e.g., for the "Run All" task.
   * @return An Optional of the task's JavaForkOptions, if any
   */
  java.util.Optional<JavaForkOptions> javaForkOptions()

  /*
   * Subset of JavaForkOptions API, shared between Unit Tests and "Run All" task,
   * so they can be configured together using unitTests.all {}.
   *
   * If a user needs to find out whether or not the "Run All" task is being processed or not,
   * use the "isRunAllTask()" API.
   */

  @Optional
  @Input
  List<String> getJvmArgs()

  void setJvmArgs(List<String> var1)

  void setJvmArgs(Iterable<?> var1)

  JavaForkOptions jvmArgs(Iterable<?> var1)

  JavaForkOptions jvmArgs(Object... var1)

  @Input
  Map<String, Object> getSystemProperties()

  void setSystemProperties(Map<String, ?> var1)

  JavaForkOptions systemProperties(Map<String, ?> var1)

  JavaForkOptions systemProperty(String var1, Object var2)

  Map<String, Object> getEnvironment()

  void setEnvironment(Map<String, ?> var1)

  ProcessForkOptions environment(Map<String, ?> var1)

  ProcessForkOptions environment(String var1, Object var2)
}
