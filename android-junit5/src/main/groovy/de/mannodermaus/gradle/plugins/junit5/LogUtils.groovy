package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.logging.Logger

/**
 * Note: This is in Groovy until no other code in this language
 * is using it anymore, or no more groovy files remain.*/
class LogUtils {

  static def agpStyleLog(Logger logger, Level level, String message) {
    def fullMessage = "AGPBI: {\"kind\":\"$level.tag\",\"text\":\"$message\"}"

    if (level == Level.WARNING) {
      logger.warn(fullMessage)
    } else {
      logger.info(fullMessage)
    }
  }

  enum Level {
    INFO("info"),
    WARNING("warning");

    private final String tag

    Level(String tag) {
      this.tag = tag
    }
  }
}
