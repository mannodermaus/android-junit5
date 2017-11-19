package de.mannodermaus.gradle.plugins.junit5.util

class StringUtils {

  static String splitClasspath(List<File> dependencies) {
    return dependencies
        .collect { it.absolutePath.replace('\\', '\\\\') }
        .collect { "'$it'" }
        .join(", ")
  }
}
