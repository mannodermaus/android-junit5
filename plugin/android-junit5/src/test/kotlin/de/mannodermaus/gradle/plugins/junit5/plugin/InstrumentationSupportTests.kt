package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.Libraries
import de.mannodermaus.gradle.plugins.junit5.internal.config.ANDROID_JUNIT5_RUNNER_BUILDER_CLASS
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.util.assertThat
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import org.gradle.api.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class InstrumentationSupportTests {

    @RegisterExtension
    @JvmField
    val projectExtension = TestProjectProviderExtension()

    private lateinit var project: Project

    @BeforeEach
    fun beforeEach() {
        project = projectExtension.newProject()
            .asAndroidApplication()
            .applyJUnit5Plugin(true)
            .build()
    }

    /* RunnerBuilder */

    @Test
    fun `add the RunnerBuilder`() {
        project.addJUnitJupiterApi()
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
            .isEqualTo(ANDROID_JUNIT5_RUNNER_BUILDER_CLASS)
    }

    @Test
    fun `maintain any existing RunnerBuilder`() {
        project.addJUnitJupiterApi()
        project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"] = "something.else"
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
            .isEqualTo("something.else,$ANDROID_JUNIT5_RUNNER_BUILDER_CLASS")
    }

    @Test
    fun `do not add the RunnerBuilder when disabled`() {
        project.addJUnitJupiterApi()
        project.junitPlatform.instrumentationTests.enabled.set(false)
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"]).isNull()
    }

    @Test
    fun `do not add the RunnerBuilder when Jupiter is not added`() {
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"]).isNull()
    }

    /* Dependencies */

    @Test
    fun `add only the main dependencies`() {
        project.addJUnitJupiterApi()
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())

        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(composeLibrary())
    }

    @Test
    fun `allow overriding the version`() {
        project.addJUnitJupiterApi()
        project.junitPlatform.instrumentationTests.version.set("1.3.3.7")
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary("1.3.3.7"))
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary("1.3.3.7"))
    }

    @Test
    fun `allow overriding the dependencies`() {
        project.addJUnitJupiterApi()
        val addedCore = "de.mannodermaus.junit5:android-test-core:0.1.3.3.7"
        val addedRunner = "de.mannodermaus.junit5:android-test-runner:0.1.3.3.7"
        project.dependencies.add("androidTestImplementation", addedCore)
        project.dependencies.add("androidTestRuntimeOnly", addedRunner)
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary("0.1.3.3.7"))
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary("0.1.3.3.7"))
    }

    @Test
    fun `do not add the dependencies when disabled`() {
        project.addJUnitJupiterApi()
        project.junitPlatform.instrumentationTests.enabled.set(false)
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(coreLibrary(null))
        assertThat(project).configuration("androidTestRuntimeOnly").doesNotHaveDependency(runnerLibrary(null))
    }

    @Test
    fun `do not add the dependencies when Jupiter is not added`() {
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(coreLibrary(null))
        assertThat(project).configuration("androidTestRuntimeOnly").doesNotHaveDependency(runnerLibrary(null))
    }

    @Test
    fun `do not add the dependencies when Jupiter is not added, even if extension is configured to be added`() {
        project.junitPlatform.instrumentationTests.includeExtensions.set(true)
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(coreLibrary(null))
        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(extensionsLibrary(null))
        assertThat(project).configuration("androidTestRuntimeOnly").doesNotHaveDependency(runnerLibrary(null))
    }

    @Test
    fun `add the extension library if configured`() {
        project.addJUnitJupiterApi()
        project.junitPlatform.instrumentationTests.includeExtensions.set(true)
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())
    }

    @Test
    fun `add the compose library if configured`() {
        project.addJUnitJupiterApi()
        project.addCompose()
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(composeLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())
    }

    @Test
    fun `add the extensions and compose libraries if configured`() {
        project.addJUnitJupiterApi()
        project.addCompose()
        project.junitPlatform.instrumentationTests.includeExtensions.set(true)
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(composeLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())
    }

    @Test
    fun `register the filter-write tasks`() {
        project.addJUnitJupiterApi()
        project.evaluate()

        // AGP only registers androidTest tasks for the debug build type
        assertThat(project).task("writeFiltersDebugAndroidTest").exists()
        assertThat(project).task("writeFiltersReleaseAndroidTest").doesNotExist()
    }

    /* Private */

    private fun Project.addJUnitJupiterApi() {
        dependencies.add("androidTestImplementation", "org.junit.jupiter:junit-jupiter-api:+")
    }

    private fun Project.addCompose() {
        dependencies.add("androidTestImplementation", "androidx.compose.ui:ui-test-android:+")
    }

    private fun composeLibrary(withVersion: String? = Libraries.instrumentationVersion) =
        library(Libraries.instrumentationCompose, withVersion)

    private fun coreLibrary(withVersion: String? = Libraries.instrumentationVersion) =
        library(Libraries.instrumentationCore, withVersion)

    private fun extensionsLibrary(withVersion: String? = Libraries.instrumentationVersion) =
        library(Libraries.instrumentationExtensions, withVersion)

    private fun runnerLibrary(withVersion: String? = Libraries.instrumentationVersion) =
        library(Libraries.instrumentationRunner, withVersion)

    private fun library(artifactId: String, version: String?) = buildString {
        append(artifactId)
        if (version != null) {
            append(':')
            append(version)
        }
    }
}
