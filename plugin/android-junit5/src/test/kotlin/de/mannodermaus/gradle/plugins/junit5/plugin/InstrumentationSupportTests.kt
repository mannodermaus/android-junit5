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
    fun `add the dependencies`() {
        project.addJUnitJupiterApi()
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core"))
            .isEqualTo("${Libraries.instrumentationCore}:${Libraries.instrumentationVersion}")
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner"))
            .isEqualTo("${Libraries.instrumentationRunner}:${Libraries.instrumentationVersion}")
    }

    @Test
    fun `allow overriding the version`() {
        project.addJUnitJupiterApi()
        project.junitPlatform.instrumentationTests.version.set("1.3.3.7")
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core")).endsWith("1.3.3.7")
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner")).endsWith("1.3.3.7")
    }

    @Test
    fun `allow overriding the dependencies`() {
        project.addJUnitJupiterApi()
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
        project.addJUnitJupiterApi()
        project.junitPlatform.instrumentationTests.enabled.set(false)
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core")).isNull()
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner")).isNull()
    }

    @Test
    fun `do not add the dependencies when Jupiter is not added`() {
        project.evaluate()

        assertThat(project.dependencyNamed("androidTestImplementation", "android-test-core")).isNull()
        assertThat(project.dependencyNamed("androidTestRuntimeOnly", "android-test-runner")).isNull()
    }

    /* Private */

    private fun Project.addJUnitJupiterApi() {
        dependencies.add("androidTestImplementation", "org.junit.jupiter:junit-jupiter-api:+")
    }

    private fun Project.dependencyNamed(configurationName: String, name: String): String? {
        return configurations.getByName(configurationName)
            .dependencies
            .firstOrNull { it.name == name }
            ?.run { "$group:$name:$version" }
    }
}
