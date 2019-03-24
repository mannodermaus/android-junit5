package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.util.AgpVersion
import de.mannodermaus.gradle.plugins.junit5.util.assertThat
import de.mannodermaus.gradle.plugins.junit5.util.withPrunedPluginClasspath
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class FunctionalTests {

  @Test
  @DisplayName("Android Gradle Plugin 3.2.x, with Build Types")
  fun agp32x() {
    val fixtureRoot = File("src/test/projects/agp32x")
    File(fixtureRoot, "build").deleteRecursively()

    val result = runGradle(AgpVersion.AGP_32X)
        .withProjectDir(fixtureRoot)
        .build()

    assertThat(result).task(":test")
        .hasOutcome(TaskOutcome.SUCCESS)
    assertThat(result).output().ofTask(":testDebugUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      executedTestCount().isEqualTo(1)
    }
    assertThat(result).output().ofTask(":testReleaseUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      contains("de.mannodermaus.app.KotlinReleaseTest > test() PASSED")
      executedTestCount().isEqualTo(2)
    }
  }

  @Test
  @DisplayName("Android Gradle Plugin 3.3.x, simple")
  fun agp33x() {
    val fixtureRoot = File("src/test/projects/agp33x")
    File(fixtureRoot, "build").deleteRecursively()

    val result = runGradle(AgpVersion.AGP_33X)
        .withProjectDir(fixtureRoot)
        .build()

    assertThat(result).task(":test")
        .hasOutcome(TaskOutcome.SUCCESS)
    assertThat(result).output().ofTask(":testDebugUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      executedTestCount().isEqualTo(1)
    }
    assertThat(result).output().ofTask(":testReleaseUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      executedTestCount().isEqualTo(1)
    }
  }

  @Test
  @DisplayName("Android Gradle Plugin 3.4.x, with Flavors and Build Types")
  fun agp34x() {
    val fixtureRoot = File("src/test/projects/agp34x")
    File(fixtureRoot, "build").deleteRecursively()

    val result = runGradle(AgpVersion.AGP_34X)
        .withProjectDir(fixtureRoot)
        .build()

    assertThat(result).task(":test")
        .hasOutcome(TaskOutcome.SUCCESS)
    assertThat(result).output().ofTask(":testFreeDebugUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      executedTestCount().isEqualTo(1)
    }
    assertThat(result).output().ofTask(":testFreeReleaseUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      contains("de.mannodermaus.app.KotlinReleaseTest > test() PASSED")
      contains("de.mannodermaus.app.JavaFreeReleaseTest > test() PASSED")
      executedTestCount().isEqualTo(3)
    }
    assertThat(result).output().ofTask(":testPaidDebugUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      contains("de.mannodermaus.app.KotlinPaidDebugTest > test() PASSED")
      executedTestCount().isEqualTo(2)
    }
    assertThat(result).output().ofTask(":testPaidReleaseUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      contains("de.mannodermaus.app.KotlinReleaseTest > test() PASSED")
      executedTestCount().isEqualTo(2)
    }
  }

  @Test
  @DisplayName("Return Android default values")
  fun androidDefaultValues() {
    val fixtureRoot = File("src/test/projects/default-values")
    File(fixtureRoot, "build").deleteRecursively()

    val result = runGradle()
        .withProjectDir(fixtureRoot)
        .build()

    assertThat(result).task(":test")
        .hasOutcome(TaskOutcome.SUCCESS)
    assertThat(result).output().ofTask(":testDebugUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      contains("de.mannodermaus.app.AndroidTest > test() PASSED")
      executedTestCount().isEqualTo(2)
    }
    assertThat(result).output().ofTask(":testReleaseUnitTest").apply {
      contains("de.mannodermaus.app.JavaTest > test() PASSED")
      contains("de.mannodermaus.app.AndroidTest > test() PASSED")
      executedTestCount().isEqualTo(2)
    }
  }

  @Test
  @DisplayName("Include Android resources")
  fun includeAndroidResources() {
    val fixtureRoot = File("src/test/projects/include-android-resources")
    File(fixtureRoot, "build").deleteRecursively()

    val result = runGradle()
        .withProjectDir(fixtureRoot)
        .build()

    assertThat(result).task(":test")
        .hasOutcome(TaskOutcome.SUCCESS)
    assertThat(result).output().ofTask(":testDebugUnitTest").apply {
      contains("de.mannodermaus.app.AndroidTest > test() PASSED")
      executedTestCount().isEqualTo(1)
    }
    assertThat(result).output().ofTask(":testReleaseUnitTest").apply {
      contains("de.mannodermaus.app.AndroidTest > test() PASSED")
      executedTestCount().isEqualTo(1)
    }
  }

  /* Private */

  private fun runGradle(agpVersion: AgpVersion? = null) =
      GradleRunner.create()
          .withArguments("test", "--stacktrace")
          .withPrunedPluginClasspath(agpVersion)
}
