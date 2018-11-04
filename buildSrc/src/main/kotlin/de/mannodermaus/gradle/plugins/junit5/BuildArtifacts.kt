package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.Platform.Android
import de.mannodermaus.gradle.plugins.junit5.Platform.Java

sealed class Platform(val name: String) {
  object Java : Platform("java")
  class Android(val minSdk: Int) : Platform("android")
}

/**
 * Encapsulation for "deployable" library artifacts,
 * containing all sorts of configuration related to Maven coordinates, for instance.
 */
open class Artifact internal constructor(
    val platform: Platform,
    val groupId: String,
    val artifactId: String,
    val currentVersion: String,
    val latestStableVersion: String,
    val description: String,
    val license: String
)

object Artifacts {
  val githubUrl = "https://github.com/mannodermaus/android-junit5"
  val githubRepo = "mannodermaus/android-junit5"
  val license = "Apache-2.0"

  /**
   * Gradle Plugin artifact
   */
  val Plugin = Artifact(
      platform = Java,
      groupId = "de.mannodermaus.gradle.plugins",
      artifactId = "android-junit5",
      currentVersion = "1.3.1.2-SNAPSHOT",
      latestStableVersion = "1.3.1.1",
      license = license,
      description = "Unit Testing with JUnit 5 for Android."
  )

  /**
   * Instrumentation Test artifacts
   */
  object Instrumentation {
    private val groupId = "de.mannodermaus.junit5"
    private val currentVersion = "0.2.3-SNAPSHOT"
    val latestStableVersion = "0.2.2"

    val Library = Artifact(
        platform = Android(minSdk = 26),
        groupId = groupId,
        artifactId = "android-instrumentation-test",
        currentVersion = currentVersion,
        latestStableVersion = latestStableVersion,
        license = license,
        description = "Extensions for instrumented Android tests with JUnit 5."
    )

    val Runner = Artifact(
        platform = Android(minSdk = 14),
        groupId = groupId,
        artifactId = "android-instrumentation-test-runner",
        currentVersion = currentVersion,
        latestStableVersion = latestStableVersion,
        license = license,
        description = "Runner for integration of instrumented Android tests with JUnit 5."
    )
  }
}
