package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.annotations.DisabledOnCI
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestedJUnit
import de.mannodermaus.gradle.plugins.junit5.util.assertThat
import de.mannodermaus.gradle.plugins.junit5.util.prettyPrint
import de.mannodermaus.gradle.plugins.junit5.util.projects.FunctionalTestProjectCreator
import de.mannodermaus.gradle.plugins.junit5.util.splitToArray
import de.mannodermaus.gradle.plugins.junit5.util.withPrunedPluginClasspath
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File

@TestInstance(PER_CLASS)
@DisabledOnCI
class ConfigurationCacheTests {

    private val environment = TestEnvironment()
    private val agp = environment.supportedAgpVersions.last()
    private lateinit var projectCreator: FunctionalTestProjectCreator

    @BeforeAll
    fun beforeAll(@TempDir folder: File) {
        projectCreator = FunctionalTestProjectCreator(folder, environment)
        println("Running configuration cache tests against latest AGP ($agp)...")

        // Ensure that no ADB device is connected before starting these tests,
        // as they have a certain expectation of "no device connected" for asserting their output
        Runtime.getRuntime().exec("adb disconnect".splitToArray(" "))
    }

    @EnumSource(TestedJUnit::class)
    @ParameterizedTest
    fun `test instrumentation tasks`(junit: TestedJUnit) {
        // Test configuration cache with one specific project and AGP version
        val spec = projectCreator.specNamed("instrumentation-tests")
        val project = projectCreator.createProject(spec, agp, junit)

        // Run it once; this is supposed to fail, but JUST because of 'no connected device',
        // not because of other errors including the configuration cache.
        runGradle(project, "connectedCheck", expectSuccess = false).assertWithLogging {
            assertThat(it).task(":connectedDebugAndroidTest").hasOutcome(FAILED)
            assertThat(it).output().contains("DeviceException: No connected devices!")
        }

        // Run it again, expecting to see a successful reuse of the configuration cache
        runGradle(project, "connectedCheck", expectSuccess = false).assertWithLogging {
            assertThat(it).output().contains("Reusing configuration cache.")
        }
    }

    @EnumSource(TestedJUnit::class)
    @ParameterizedTest
    fun `test unit tasks`(junit: TestedJUnit) {
        val spec = projectCreator.specNamed("product-flavors")
        val project = projectCreator.createProject(spec, agp, junit)

        runGradle(project, "help", expectSuccess = true).assertWithLogging {
            assertThat(it).task(":help").hasOutcome(SUCCESS)
        }

        runGradle(project, "help", expectSuccess = true).assertWithLogging {
            assertThat(it).output().contains("Reusing configuration cache.")
        }
    }

    /* Private */

    private fun runGradle(project: File, task: String, expectSuccess: Boolean) =
        GradleRunner.create()
            .withProjectDir(project)
            .withGradleVersion(agp.requiresGradle)
            .withArguments("--configuration-cache", "--stacktrace", task)
            .withPrunedPluginClasspath(agp)
            .run {
                if (expectSuccess) build()
                else buildAndFail()
            }

    private fun BuildResult.assertWithLogging(block: (BuildResult) -> Unit) {
        try {
            block(this)
        } catch (e: Throwable) {
            this.prettyPrint()
            throw e
        }
    }
}
