package de.mannodermaus.gradle.plugins.android_junit5

import de.mannodermaus.gradle.plugins.android_junit5.jacoco.AndroidJUnit5JacocoExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

/*
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through JUnitPlatformExtension
 */

class AndroidJUnitPlatformExtension extends JUnitPlatformExtension {

  AndroidJUnitPlatformExtension(Project project) {
    super(project)
  }

  /* The version of JUnit Jupiter to use. */
  String jupiterVersion

  /* The version of JUnit Vintage Engine to use. */
  String vintageVersion

  /* Configuration of Jacoco Code Coverage reports. */

  void jacoco(Action<AndroidJUnit5JacocoExtension> closure) {
    closure.execute(getProperty(
        AndroidJUnitPlatformPlugin.JACOCO_EXTENSION_NAME) as AndroidJUnit5JacocoExtension)
  }
}
