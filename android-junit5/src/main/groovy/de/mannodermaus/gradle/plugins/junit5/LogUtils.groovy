package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project

/**
 * Note: This is in Groovy until no other code in this language
 * is using it anymore, or no more groovy files remain.
 */
class LogUtils {

  static def warning(Project project, String message) {
    project.logger.warn("AGPBI: {\"kind\":\"warning\",\"text\":\"$message\"}")
  }
}
