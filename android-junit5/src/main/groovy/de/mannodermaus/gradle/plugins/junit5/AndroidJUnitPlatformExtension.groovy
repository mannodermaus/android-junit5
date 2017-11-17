package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.jacoco.AndroidJUnit5JacocoExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

import javax.annotation.Nullable

/*
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through JUnitPlatformExtension
 */

class AndroidJUnitPlatformExtension extends JUnitPlatformExtension implements ExtensionAware {

  AndroidJUnitPlatformExtension(Project project) {
    super(project)
  }

  /* The version of JUnit Jupiter to use. */
  @Nullable
  String jupiterVersion

  /* The version of JUnit Vintage Engine to use. */
  @Nullable
  String vintageVersion

  /* Configuration of Jacoco Code Coverage reports. */

  void jacoco(Action<AndroidJUnit5JacocoExtension> closure) {
    closure.execute(getProperty(
        de.mannodermaus.gradle.plugins.android_junit5.AndroidJUnitPlatformPlugin.JACOCO_EXTENSION_NAME) as AndroidJUnit5JacocoExtension)
  }

  @Override
  ExtensionContainer getExtensions() {
    return super.extensions
  }
}
