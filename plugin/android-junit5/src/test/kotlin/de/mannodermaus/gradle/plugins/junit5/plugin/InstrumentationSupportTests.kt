package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.Libraries
import de.mannodermaus.gradle.plugins.junit5.internal.config.ANDROID_JUNIT5_RUNNER_BUILDER_CLASS
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
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
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
            .isEqualTo(ANDROID_JUNIT5_RUNNER_BUILDER_CLASS)
    }

    @Test
    fun `maintain any existing RunnerBuilder`() {
        project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"] = "something.else"
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
            .isEqualTo("something.else,$ANDROID_JUNIT5_RUNNER_BUILDER_CLASS")
    }

    @Test
    fun `do not add the RunnerBuilder when disabled`() {
        project.junitPlatform.instrumentationTests.enabled = false
        project.evaluate()

        assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"]).isNull()
    }

    /* Dependencies */

    @Test
    fun `add the dependencies`() {
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core"))
            .isEqualTo(Libraries.instrumentationCore)
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner"))
            .isEqualTo(Libraries.instrumentationRunner)
    }

    @Test
    fun `allow overriding the dependencies`() {
        val addedCore = "de.mannodermaus.junit5:android-test-core:0.1.3.3.7"
        val addedRunner = "de.mannodermaus.junit5:android-test-runner:0.1.3.3.7"
        project.dependencies.add("androidTestImplementation", addedCore)
        project.dependencies.add("androidTestRuntimeOnly", addedRunner)
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core")).isEqualTo(addedCore)
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner")).isEqualTo(addedRunner)
    }

    @Test
    fun `do not add the dependencies when disabled`() {
        project.junitPlatform.instrumentationTests.enabled = false
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core")).isNull()
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner")).isNull()
    }

    /* Private */
    private fun Project.dependencyNamed(configurationName: String, name: String): String? {
        return configurations.getByName(configurationName)
            .dependencies
            .firstOrNull { it.name == name }
            ?.run { "$group:$name:$version" }
    }
}
