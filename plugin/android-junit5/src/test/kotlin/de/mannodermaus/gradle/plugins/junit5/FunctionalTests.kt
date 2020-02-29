package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.annotations.DisabledOnCI
import de.mannodermaus.gradle.plugins.junit5.util.AgpVersion
import de.mannodermaus.gradle.plugins.junit5.util.assertThat
import de.mannodermaus.gradle.plugins.junit5.util.withPrunedPluginClasspath
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.io.File

class FunctionalTests {

  // Iterate over all values in the AgpVersion enum and,
  // using the project with that name located in the test resource folder,
  // run a basic integration test with the JUnit 5 plugin.
  //
  // The CI server does not have enough memory to run multiple nested virtual Gradle builds.
  // Restrict execution of this test method to the local machine only
  @DisplayName("Integration Tests with Android Gradle Plugin")
  @TestFactory
  @DisabledOnCI
  fun agpIntegrationTests(): List<DynamicTest> =
      AgpVersion.values()
          .map { agpVersion ->
            dynamicTest("Version ${agpVersion.prettyName}") {
              // Required for visibility inside IJ's logging console (display names are still bugged in the IDE)
              println("Testing AGP ${agpVersion.prettyName}")

              // Validate presence of test project
              val projectName = agpVersion.fileKey
              val fixtureRoot = File("src/test/projects/$projectName")
              require(fixtureRoot.exists()) { "Make sure that there is a project folder at: '$fixtureRoot'" }
              File(fixtureRoot, "build").deleteRecursively()

              // Run virtual Gradle
              val result = runGradle(agpVersion)
                  .withProjectDir(fixtureRoot)
                  .build()

              // Assert outputs;
              assertThat(result).task(":test").hasOutcome(TaskOutcome.SUCCESS)

              with(result) {
                assertAgpTests(buildType = "debug", productFlavor = "free", tests = listOf("JavaTest"))
                assertAgpTests(buildType = "debug", productFlavor = "paid", tests = listOf("JavaTest", "KotlinPaidDebugTest"))
                assertAgpTests(buildType = "release", productFlavor = "free", tests = listOf("JavaTest", "KotlinReleaseTest", "JavaFreeReleaseTest"))
                assertAgpTests(buildType = "release", productFlavor = "paid", tests = listOf("JavaTest", "KotlinReleaseTest"))
              }
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

    assertThat(result).task(":test").hasOutcome(TaskOutcome.SUCCESS)
    result.assertAgpTests(buildType = "debug", tests = listOf("JavaTest", "AndroidTest"))
    result.assertAgpTests(buildType = "release", tests = listOf("JavaTest", "AndroidTest"))
  }

  @Test
  @DisplayName("Include Android resources")
  fun includeAndroidResources() {
    val fixtureRoot = File("src/test/projects/include-android-resources")
    File(fixtureRoot, "build").deleteRecursively()

    val result = runGradle()
        .withProjectDir(fixtureRoot)
        .build()

    assertThat(result).task(":test").hasOutcome(TaskOutcome.SUCCESS)
    result.assertAgpTests(buildType = "debug", tests = listOf("AndroidTest"))
    result.assertAgpTests(buildType = "release", tests = listOf("AndroidTest"))
  }

  /* Private */

  private fun runGradle(agpVersion: AgpVersion = AgpVersion.latest()) =
      GradleRunner.create()
          .apply {
            if (agpVersion.requiresGradle != null) {
              withGradleVersion(agpVersion.requiresGradle)
            }
          }
          .withArguments("test", "--stacktrace")
          .withPrunedPluginClasspath(agpVersion)

  // Helper DSL to assert AGP-specific results of the virtual Gradle executions.
  // This asserts the output of the build against the given criteria
  private fun BuildResult.assertAgpTests(
      buildType: String,
      productFlavor: String? = null,
      tests: List<String>) {
    // Construct task name from given build type and/or product flavor
    // Examples:
    // - buildType="debug", productFlavor=null --> ":testDebugUnitTest"
    // - buildType="debug", productFlavor="free" --> ":testFreeDebugUnitTest"
    val taskName = ":test${productFlavor?.capitalize() ?: ""}${buildType.capitalize()}UnitTest"

    // Perform assertions
    assertThat(this).output().ofTask(taskName).apply {
      tests.forEach { expectedClass ->
        contains("$expectedClass > test() PASSED")
      }
      executedTestCount().isEqualTo(tests.size)
    }
  }
}
