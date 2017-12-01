package de.mannodermaus.gradle.plugins.junit5.util

import org.gradle.api.tasks.JavaExec

import javax.annotation.Nullable

class TaskUtils {
  private TaskUtils() {}

  @Nullable
  static String argument(JavaExec task, String argumentName) {
    def index = task.args.indexOf(argumentName)
    return index == -1 || task.args.size() == index + 1 ? null : task.args[index + 1]
  }
}
