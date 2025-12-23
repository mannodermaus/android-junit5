package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.Libraries
import de.mannodermaus.gradle.plugins.junit5.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.config.ANDROID_JUNIT5_RUNNER_BUILDER_CLASS
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
        project.addJUnitJupiterApi("androidTest")
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
            .isEqualTo(ANDROID_JUNIT5_RUNNER_BUILDER_CLASS)
    }

    @Test
    fun `maintain any existing RunnerBuilder`() {
        project.addJUnitJupiterApi("androidTest")
        project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"] = "something.else"
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
            .isEqualTo("something.else,$ANDROID_JUNIT5_RUNNER_BUILDER_CLASS")
    }

    @Test
    fun `do not add the RunnerBuilder when disabled`() {
        project.addJUnitJupiterApi("androidTest")
        project.junitPlatform.instrumentationTests.enabled.set(false)
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"]).isNull()
    }

    @Test
    fun `do not add the RunnerBuilder when Jupiter is not added`() {
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"]).isNull()
    }

    /* Configuration parameters */

    @Test
    fun `copy configuration parameters to test runner arguments`() {
        project.addJUnitJupiterApi("androidTest")
        with (project.junitPlatform) {
            configurationParameter("my.parameter1", "true")
            configurationParameter("my.parameter2", "1234")
        }
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["configurationParameters"])
            .isEqualTo("my.parameter1=true,my.parameter2=1234")
    }

    @Test
    fun `do not copy configuration parameters if disabled via dsl`() {
        project.addJUnitJupiterApi("androidTest")
        with (project.junitPlatform) {
            configurationParameter("my.parameter1", "true")
            configurationParameter("my.parameter2", "1234")
            instrumentationTests.useConfigurationParameters.set(false)
        }
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["configurationParameters"])
            .isNull()
    }

    /* Dependencies */

    @Test
    fun `add only the main dependencies (test)`() {
        project.addJUnitJupiterApi("test")
        project.evaluate()

        assertThat(project).configuration("testImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("testRuntimeOnly").doesNotHaveDependency(runnerLibrary())

        assertThat(project).configuration("testImplementation").doesNotHaveDependency(extensionsLibrary())
        assertThat(project).configuration("testImplementation").doesNotHaveDependency(composeLibrary())
    }

    @Test
    fun `add only the main dependencies (androidTest)`() {
        project.addJUnitJupiterApi("androidTest")
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())

        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestImplementation").doesNotHaveDependency(composeLibrary())
    }

    @Test
    fun `allow overriding the version`() {
        project.addJUnitJupiterApi("androidTest")
        project.junitPlatform.instrumentationTests.version.set("1.3.3.7")
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary("1.3.3.7"))
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary("1.3.3.7"))
    }

    @Test
    fun `allow overriding the dependencies`() {
        project.addJUnitJupiterApi("androidTest")
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
        project.addJUnitJupiterApi("androidTest")
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
        project.addJUnitJupiterApi("androidTest")
        project.junitPlatform.instrumentationTests.includeExtensions.set(true)
        project.evaluate()

        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())
    }

    @Test
    fun `add the compose library if configured`() {
        project.addJUnitJupiterApi("test")
        project.addJUnitJupiterApi("androidTest")
        project.addCompose("test")
        project.addCompose("androidTest")
        project.evaluate()

        assertThat(project).configuration("testImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("testImplementation").hasDependency(composeLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(composeLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())
    }

    @Test
    fun `add the extensions and compose libraries if configured`() {
        project.addJUnitJupiterApi("test")
        project.addJUnitJupiterApi("androidTest")
        project.addCompose("test")
        project.addCompose("androidTest")
        project.junitPlatform.instrumentationTests.includeExtensions.set(true)
        project.evaluate()

        assertThat(project).configuration("testImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("testImplementation").hasDependency(composeLibrary())
        assertThat(project).configuration("testImplementation").hasDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(coreLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(composeLibrary())
        assertThat(project).configuration("androidTestImplementation").hasDependency(extensionsLibrary())
        assertThat(project).configuration("androidTestRuntimeOnly").hasDependency(runnerLibrary())
    }

    @Test
    fun `register the filter-write tasks`() {
        project.addJUnitJupiterApi("androidTest")
        project.evaluate()

        // AGP only registers androidTest tasks for the debug build type
        assertThat(project).task("writeFiltersDebugAndroidTest").exists()
        assertThat(project).task("writeFiltersReleaseAndroidTest").doesNotExist()
    }

    /* Private */

    private fun Project.addJUnitJupiterApi(prefix: String) {
        val configuration = if (prefix.isEmpty()) "implementation" else "${prefix}Implementation"
        dependencies.add(configuration, "org.junit.jupiter:junit-jupiter-api:+")
    }

    private fun Project.addCompose(prefix: String) {
        val configuration = if (prefix.isEmpty()) "implementation" else "${prefix}Implementation"
        dependencies.add(configuration, "androidx.compose.ui:ui-test-android:+")
    }

    private fun composeLibrary(withVersion: String? = Libraries.Instrumentation.version) =
        library(Libraries.Instrumentation.compose, withVersion)

    private fun coreLibrary(withVersion: String? = Libraries.Instrumentation.version) =
        library(Libraries.Instrumentation.core, withVersion)

    private fun extensionsLibrary(withVersion: String? = Libraries.Instrumentation.version) =
        library(Libraries.Instrumentation.extensions, withVersion)

    private fun runnerLibrary(withVersion: String? = Libraries.Instrumentation.version) =
        library(Libraries.Instrumentation.runner, withVersion)

    private fun library(artifactId: String, version: String?) = buildString {
        append(artifactId)
        if (version != null) {
            append(':')
            append(version)
        }
    }
}
