package de.mannodermaus.gradle.plugins.junit5.plugin

import com.android.build.gradle.TestedExtension
import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5WriteFilters
import de.mannodermaus.gradle.plugins.junit5.util.assertAll
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import de.mannodermaus.gradle.plugins.junit5.util.get
import org.gradle.api.Project
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

interface AgpInstrumentationSupportTests : AgpVariantAwareTests {
    @TestFactory
    fun `generate instrumentation resource file (no product flavors)`(): List<DynamicTest> {
        val project = createProject().build()
        project.setupInstrumentationTests()
        project.junitPlatform {
            filters {
                it.includeTags("global-include-tag")
                it.includeEngines("global-include-engine")
                it.includePattern("pattern123")
            }
            filters("debug") {
                it.excludeTags("debug-exclude-tag")
                it.excludeEngines("debug-exclude-engine")
                it.excludePattern("pattern123")
                it.excludePattern("debug-pattern")
            }
            filters("release") {
                it.includeTags("rel-include-tag")
                it.includeEngines("rel-include-engine")
                it.excludeEngines("global-include-engine")
                it.includePattern("release-pattern")
            }
        }
        project.evaluate()

        return listOf(
                dynamicTest("has a task for writing the debug filters DSL to a resource file") {
                    val task = project.tasks.get<AndroidJUnit5WriteFilters>("writeFiltersDebugAndroidTest")
                    assertAll(
                            { assertThat(task).isNotNull() },
                            { assertThat(task.includeTags.get()).containsExactly("global-include-tag") },
                            { assertThat(task.excludeTags.get()).containsExactly("debug-exclude-tag") }
                    )
                },

                dynamicTest("has no task for writing the release DSL to a resource file") {
                    val task = project.tasks.findByName("writeFiltersReleaseAndroidTest")
                    assertThat(task).isNull()
                }
        )
    }

    @Test
    fun `generate instrumentation resource file (non-standard testBuildType)`() {
        val project = createProject().build()
        project.setupInstrumentationTests()
        (project.android as TestedExtension).testBuildType = "release"
        project.evaluate()

        val task = project.tasks.get<AndroidJUnit5WriteFilters>("writeFiltersReleaseAndroidTest")
        assertThat(task).isNotNull()
    }

    @TestFactory
    fun `generate instrumentation resource file (with product flavors)`(): List<DynamicTest> {
        val project = createProject().build()
        project.registerProductFlavors()
        project.setupInstrumentationTests()
        project.junitPlatform {
            filters {
                it.includeTags("global-include-tag")
                it.excludeTags("global-exclude-tag")
                it.includePattern("com.example.package1")
            }
            filters("paid") {
                it.includeEngines("paid-include-engine")
                it.includePattern("com.example.paid")
                it.excludePattern("com.example.package1")
            }
            filters("freeDebug") {
                it.includeTags("freeDebug-include-tag")
            }
            filters("paidRelease") {
                it.includeTags("paidRelease-include-tag")
                it.includeTags("global-exclude-tag")
                it.includePattern("com.example.paid.release")
            }
        }
        project.evaluate()

        return listOf(
                dynamicTest("has a task for writing the freeDebug filters DSL to a resource file") {
                    val task = project.tasks.get<AndroidJUnit5WriteFilters>("writeFiltersFreeDebugAndroidTest")
                    assertThat(task).isNotNull()
                    assertThat(task.includeTags.get()).containsExactly("global-include-tag", "freeDebug-include-tag")
                    assertThat(task.excludeTags.get()).containsExactly("global-exclude-tag")
                },

                dynamicTest("has a task for writing the paidDebug filters DSL to a resource file") {
                    val task = project.tasks.get<AndroidJUnit5WriteFilters>("writeFiltersPaidDebugAndroidTest")
                    assertThat(task).isNotNull()
                    assertThat(task.includeTags.get()).containsExactly("global-include-tag")
                    assertThat(task.excludeTags.get()).containsExactly("global-exclude-tag")
                },

                dynamicTest("doesn't have tasks for writing the release filters DSL to a resource file") {
                    assertThat(project.tasks.findByName("writeFiltersFreeReleaseAndroidTest")).isNull()
                    assertThat(project.tasks.findByName("writeFiltersPaidReleaseAndroidTest")).isNull()
                }
        )
    }
}

private fun Project.setupInstrumentationTests() {
    android.defaultConfig {
        it.testInstrumentationRunnerArgument("runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder")
    }
    dependencies.add("androidTestRuntimeOnly", "de.mannodermaus.junit5:android-test-runner:+")
}
