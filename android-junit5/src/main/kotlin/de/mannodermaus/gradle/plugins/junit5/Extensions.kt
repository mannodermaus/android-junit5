package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.util.Properties

/* General */

fun requireGradle(version: String, message: () -> String) {
  require(GradleVersion.current() < GradleVersion.version(version)) {
    throw GradleException(message.invoke())
  }
}

fun loadProperties(resource: String): Properties {
  val properties = Properties()
  val stream = AndroidJUnitPlatformPlugin::class.java.getResourceAsStream(resource)
  stream.use { properties.load(it) }
  return properties
}

/* Project */

fun Project.hasPlugin(name: String) = this.plugins.findPlugin(name) != null
