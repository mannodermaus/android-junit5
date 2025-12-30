package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.outputLocationFile
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.JACOCO_TASK_NAME
import de.mannodermaus.gradle.plugins.junit5.util.assertAll
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

interface AgpJacocoBaseTests : AgpVariantAwareTests {

    @Test
    fun `if jacoco plugin is not applied, do not create a parent task`() {
        val project = createProject().buildAndEvaluate()

        assertThat(project.tasks.findByName(JACOCO_TASK_NAME)).isNull()
    }

    @Test
    fun `if jacoco plugin is applied, generate a parent task`() {
        val project = createProject().applyJacocoPlugin().buildAndEvaluate()

        assertThat(project.tasks.findByName(JACOCO_TASK_NAME)).isNotNull()
    }

    @Test
    fun `do not cause a conflict if a jacoco task already exists`() {
        val project = createProject().applyJacocoPlugin().build()
        project.tasks.create("${JACOCO_TASK_NAME}Debug", JacocoReport::class.java)
        project.evaluate()

        assertThat(project.tasks.findByName(JACOCO_TASK_NAME)).isNotNull()
        assertThat(project.tasks.findByName("${JACOCO_TASK_NAME}Debug"))
            .isInstanceOf(JacocoReport::class.java)
        assertThat(project.tasks.findByName("${JACOCO_TASK_NAME}Release"))
            .isInstanceOf(AndroidJUnit5JacocoReport::class.java)
    }

    @TestFactory
    fun `acknowledge disabling of jacoco task generation`(): List<DynamicTest> {
        val project = createProject().applyJacocoPlugin().build()
        project.junitPlatform.jacocoOptions { it.taskGenerationEnabled.set(false) }
        project.evaluate()

        return listOf(JACOCO_TASK_NAME, "${JACOCO_TASK_NAME}Debug", "${JACOCO_TASK_NAME}Release")
            .map { task ->
                dynamicTest("does not generate $task task if generation is disabled") {
                    assertThat(project.tasks.findByName(task)).isNull()
                }
            }
    }

    // Reporting

    @Test
    fun `acknowledge custom report folders`() {
        val project = createProject().applyJacocoPlugin().build()
        project.junitPlatform.jacocoOptions {
            it.xml.destination.set(project.file("build/other-jacoco-folder/xml"))
            it.csv.destination.set(project.file("build/CSVISDABEST"))
            it.html.destination.set(project.file("build/html-reports/jacoco"))
        }
        project.evaluate()

        project.tasks
            .withType(AndroidJUnit5JacocoReport::class.java)
            .map { it.reports }
            .forEach { report ->
                assertAll(
                    {
                        assertThat(report.xml.outputLocationFilePath)
                            .endsWith("build/other-jacoco-folder/xml")
                    },
                    { assertThat(report.csv.outputLocationFilePath).endsWith("build/CSVISDABEST") },
                    {
                        assertThat(report.html.outputLocationFilePath)
                            .endsWith("build/html-reports/jacoco")
                    },
                )
            }
    }

    @ValueSource(booleans = [true, false])
    @ParameterizedTest(name = "acknowledge status of report tasks when enabled={0}")
    fun `acknowledge status of report tasks`(required: Boolean) {
        val project = createProject().applyJacocoPlugin().build()
        project.junitPlatform.jacocoOptions {
            it.xml.enabled.set(required)
            it.csv.enabled.set(required)
            it.html.enabled.set(required)
        }
        project.evaluate()

        project.tasks
            .withType(AndroidJUnit5JacocoReport::class.java)
            .map { it.reports }
            .forEach {
                assertAll(
                    { assertThat(it.xml.required.get() == required) },
                    { assertThat(it.csv.required.get() == required) },
                    { assertThat(it.html.required.get() == required) },
                )
            }
    }

    /* Private */

    private val ConfigurableReport.outputLocationFilePath
        get() = outputLocationFile.asFile.orNull?.absolutePath
}
