package de.mannodermaus.gradle.plugins.junit5

import com.google.common.truth.Truth.assertWithMessage
import de.mannodermaus.gradle.plugins.junit5.annotations.DisabledOnCI
import de.mannodermaus.gradle.plugins.junit5.util.*
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File

@TestInstance(PER_CLASS)
@DisabledOnCI
class FunctionalTests {

  private val environment = TestEnvironment2()
  private lateinit var projectProvider: FunctionalProjectProvider

  @BeforeAll
  fun beforeAll() {
    // The "project provider" is responsible for the construction
    // of all virtual Gradle projects, using a template file located in
    // the project's test resources.
    val folder = File("build/tmp/virtualProjectsRoot")
    folder.mkdirs()
    projectProvider = FunctionalProjectProvider(folder, environment)
  }

  @TestFactory
  fun execute(): List<DynamicNode> =
      environment.supportedAgpVersions.map { agp ->
        dynamicContainer(
            "AGP ${agp.shortVersion}",
            projectProvider.allSpecs.map { spec ->
              dynamicTest(spec.name) {
                // Required for visibility inside IJ's logging console (display names are still bugged in the IDE)
                println("AGP: ${agp.version}, Project: ${spec.name}, Forced Gradle: ${agp.requiresGradle ?: "no"}")

                // Create a virtual project with the given settings & AGP version
                val project = projectProvider.createProject(spec, agp)

                // Execute the tests of the virtual project with Gradle
                val result = runGradle(agp)
                    .withProjectDir(project)
                    .build()

                // Check that the task execution was successful in general
                when (val outcome = result.task(":test")?.outcome) {
                  TaskOutcome.UP_TO_DATE -> {
                    // Nothing to do, a previous build already checked this
                    println("Test task up-to-date; skipping assertions.")
                  }

                  TaskOutcome.SUCCESS -> {
                    // Based on the spec's configuration in the test project,
                    // assert that all test classes have been executed as expected
                    for (expectation in spec.expectedTests) {
                      result.assertAgpTests(
                          buildType = expectation.buildType,
                          productFlavor = expectation.productFlavor,
                          tests = expectation.testsList
                      )
                    }
                  }

                  else -> {
                    // Unexpected result; fail
                    fail { "Unexpected task outcome: $outcome" }
                  }
                }
              }
            }
        )
      }

  /* Private */

  private fun runGradle(agpVersion: AgpUnderTest) =
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
    assertWithMessage("AGP Tests for '$taskName' did not match expectations")
        .about(::BuildResultSubject)
        .that(this)
        .output()
        .ofTask(taskName)
        .apply {
          tests.forEach { expectedClass ->
            contains("$expectedClass > test() PASSED")
          }
          executedTestCount().isEqualTo(tests.size)
        }
  }
}
