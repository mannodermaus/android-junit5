package de.mannodermaus.gradle.plugins.junit5

import com.google.common.truth.Truth.assertWithMessage
import de.mannodermaus.gradle.plugins.junit5.annotations.DisabledOnCI
import de.mannodermaus.gradle.plugins.junit5.util.BuildResultSubject
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestedAgp
import de.mannodermaus.gradle.plugins.junit5.util.prettyPrint
import de.mannodermaus.gradle.plugins.junit5.util.projects.FunctionalTestProjectCreator
import de.mannodermaus.gradle.plugins.junit5.util.withPrunedPluginClasspath
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.fail
import java.io.File

@TestInstance(PER_CLASS)
@DisabledOnCI
class FunctionalTests {
    private val environment = TestEnvironment()
    private lateinit var folder: File

    // Test permutations for AGP (default: empty set, which will exercise all)
    private val testedAgpVersions: Set<String> = setOf(
    )

    // Test permutations for projects (default: empty set, which will exercise all)
    private val testedProjects: Set<String> = setOf(
    )

    // Whether to pass "-i" to the Gradle runners, increasing insight into their output
    private val verboseOutput = false

    // Whether to delete all virtual project root folders after executing these tests
    private val cleanOutputFolderAfterTests = true

    @BeforeAll
    fun beforeAll() {
        // The "project provider" is responsible for the construction
        // of all virtual Gradle projects, using a template file located in
        // the project's test resources.
        folder = File("build/tmp/virtualProjectsRoot").also { it.mkdirs() }
    }

    @AfterAll
    fun afterAll() {
        if (cleanOutputFolderAfterTests) {
            folder.deleteRecursively()
        }
    }

    @TestFactory
    fun execute(): List<DynamicNode> = environment.supportedAgpVersions.filterAgpVersions()
        .map { agp ->
            // Create a matrix of permutations between the AGP versions to test
            // and the language of the project's build script
            val projectCreator = FunctionalTestProjectCreator(folder, environment)

            // Generate a container for all tests with this specific AGP/Language combination
            dynamicContainer("AGP ${agp.shortVersion}",

                // Exercise each test project within the given environment
                projectCreator.allSpecs.filterSpecs().map { spec ->
                    dynamicTest(spec.name) {
                        // Required for visibility inside IJ's logging console (display names are still bugged in the IDE)
                        println(buildList {
                            add("AGP: ${agp.version}")
                            add("Project: ${spec.name}")
                            add("Gradle: ${agp.requiresGradle}")
                            agp.requiresCompileSdk?.let { add("SDK: $it") }
                        }.joinToString(", "))

                        // Create a virtual project with the given settings & AGP version.
                        // This call will throw a TestAbortedException if the spec is not eligible for this version,
                        // marking the test as ignored in the process
                        val project = projectCreator.createProject(spec, agp)

                        // Execute the tests of the virtual project with Gradle
                        val taskName = spec.task ?: "test"
                        val result = runGradle(agp, taskName)
                            .withProjectDir(project)
                            .build()

                        // Check that the task execution was successful in general
                        when (val outcome = result.task(":$taskName")?.outcome) {
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
                                fail {
                                    "Unexpected task outcome: $outcome\n\nRaw output:\n\n${result.output}"
                                }
                            }
                        }
                    }
                }
            )
        }

    /* Private */

    private fun List<TestedAgp>.filterAgpVersions(): List<TestedAgp> =
        if (testedAgpVersions.isEmpty()) {
            // Nothing to do, exercise functional tests on all AGP versions
            // (but in reverse order, so that the newest AGP is tested first)
            reversed()
        } else {
            filter { agp ->
                testedAgpVersions.any { it == agp.shortVersion }
            }
        }

    private fun List<FunctionalTestProjectCreator.Spec>.filterSpecs(): List<FunctionalTestProjectCreator.Spec> =
        if (testedProjects.isEmpty()) {
            // Nothing to do, exercise all different projects
            this
        } else {
            filter { spec ->
                testedProjects.any { it == spec.name }
            }
        }

    private fun runGradle(agpVersion: TestedAgp, task: String): GradleRunner {
        val arguments = buildList {
            add(task)
            add("--stacktrace")
            if (verboseOutput) add("-i")
        }

        return GradleRunner.create()
            .withGradleVersion(agpVersion.requiresGradle)
            .withArguments(arguments)
            .withPrunedPluginClasspath(agpVersion)
    }

    // Helper DSL to assert AGP-specific results of the virtual Gradle executions.
    // This asserts the output of the build against the given criteria
    private fun BuildResult.assertAgpTests(
        buildType: String,
        productFlavor: String? = null,
        tests: List<String>
    ) {
        this.prettyPrint()

        // Construct task name from given build type and/or product flavor
        // Examples:
        // - buildType="debug", productFlavor=null --> ":testDebugUnitTest"
        // - buildType="debug", productFlavor="free" --> ":testFreeDebugUnitTest"
        val taskName = ":test${productFlavor?.capitalized() ?: ""}${buildType.capitalized()}UnitTest"

        // Perform assertions
        assertWithMessage("AGP Tests for '$taskName' did not match expectations")
            .about(::BuildResultSubject)
            .that(this)
            .output()
            .ofTask(taskName)
            .apply {
                tests.forEach { expectedClass ->
                    val line = "$expectedClass > test() PASSED"
                    contains(line)
                    println(line)
                }
                executedTestCount().isEqualTo(tests.size)
            }
    }
}
