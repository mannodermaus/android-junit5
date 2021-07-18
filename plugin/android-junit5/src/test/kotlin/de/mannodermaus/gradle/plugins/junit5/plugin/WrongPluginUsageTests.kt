package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import org.gradle.api.Action
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.plugins.PluginApplicationException
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension

class WrongPluginUsageTests {

    @RegisterExtension
    @JvmField
    val projectExtension = TestProjectProviderExtension()

    @Test
    fun `not applying any supported Android plugin`() {
        val exception = assertThrows<PluginApplicationException> {
            projectExtension.newProject().build()
        }
        assertThat(exception.cause?.message)
                .contains("One of the following plugins must be applied to this project")
    }

    @Test
    fun `configuring unavailable DSL values`() {
        val project = projectExtension.newProject()
                .asAndroidLibrary()
                .applyJUnit5Plugin(true)
                .build()

        project.junitPlatform {
            filters("unknown", Action {
                it.includeTags("doesnt-matter")
            })
        }

        val exception = assertThrows<ProjectConfigurationException> {
            project.evaluate()
        }
        assertThat(exception.cause?.message).contains("Extension with name")
        assertThat(exception.cause?.message).contains("does not exist")
    }

    @TestFactory
    fun `configuring instrumentation test support only partially (1)`(): List<DynamicNode> {
        val project = projectExtension.newProject()
                .asAndroidApplication()
                .applyJUnit5Plugin(true)
                .build()

        project.android.defaultConfig {
            it.testInstrumentationRunnerArgument("runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder")
        }

        return listOf(
                dynamicTest("fail because the library dependency is missing") {
                    val exception = assertThrows<ProjectConfigurationException> {
                        project.evaluate()
                    }

                    assertThat(exception.cause?.message).contains("Incomplete configuration for JUnit 5 instrumentation tests")
                    assertThat(exception.cause?.message).contains("Add the android-test-runner library")
                },

                dynamicTest("succeed when the check is explicitly disabled") {
                    project.junitPlatform.instrumentationTests.integrityCheckEnabled = false
                    project.evaluate()
                }
        )
    }

    @TestFactory
    fun `configuring instrumentation test support only partially (2)`(): List<DynamicNode> {
        val project = projectExtension.newProject()
                .asAndroidApplication()
                .applyJUnit5Plugin(true)
                .build()

        project.dependencies.add("androidTestRuntimeOnly", "de.mannodermaus.junit5:android-test-runner:+")

        return listOf(
                dynamicTest("fail because the RunnerBuilder is missing") {
                    val expected = assertThrows<ProjectConfigurationException> {
                        project.evaluate()
                    }

                    assertThat(expected.cause!!.message).contains("Incomplete configuration for JUnit 5 instrumentation tests")
                    assertThat(expected.cause!!.message).contains("Add the JUnit 5 RunnerBuilder")
                },

                dynamicTest("suceed when the check is explicitly disabled") {
                    project.junitPlatform.instrumentationTests.integrityCheckEnabled = false
                    project.evaluate()
                }
        )
    }
}
