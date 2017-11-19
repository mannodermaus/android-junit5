package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

import javax.annotation.Nullable

/*
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through the Java-based JUnitPlatformExtension.
 *
 * Note: Because the inheritance chain of this class reaches into the JUnit 5 codebase
 * written in Groovy, this class cannot be easily translated into Kotlin.
 */

class AndroidJUnitPlatformExtension extends JUnitPlatformExtension {

  AndroidJUnitPlatformExtension(Project project) {
    super(project)
  }

  /* The version of JUnit Jupiter to use. */
  @Nullable
  String jupiterVersion

  /* The version of JUnit Vintage Engine to use. */
  @Nullable
  String vintageVersion
}
