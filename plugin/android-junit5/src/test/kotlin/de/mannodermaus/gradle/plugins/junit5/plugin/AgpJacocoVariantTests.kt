package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.JACOCO_TASK_NAME
import de.mannodermaus.gradle.plugins.junit5.util.assertAll
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import de.mannodermaus.gradle.plugins.junit5.util.get
import de.mannodermaus.gradle.plugins.junit5.util.getDependentTaskNames
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

interface AgpJacocoVariantTests : AgpVariantAwareTests {

    private fun jacocoVariantTaskName(variant: String) =
            "${JACOCO_TASK_NAME}${variant.capitalize()}"

    @Test
    fun `create jacoco task for custom build type`() {
        val project = createProject().applyJacocoPlugin().build()
        project.android.buildTypes {
            it.create("staging")
        }
        project.evaluate()

        assertWithMessage("create a child task")
                .that(project.tasks.findByName("${JACOCO_TASK_NAME}Staging"))
                .isNotNull()

        assertWithMessage("connect to parent task")
                .that(project.tasks.getByName(JACOCO_TASK_NAME).getDependentTaskNames())
                .contains("${JACOCO_TASK_NAME}Staging")
    }

    @TestFactory
    fun `hook in build-type-specific jacoco task to parent`() = forEachBuildType(
            beforeBuild = { it.applyJacocoPlugin() }
    ) { project, buildType ->
        val name = jacocoVariantTaskName(buildType)

        assertThat(project.tasks.getByName(JACOCO_TASK_NAME)
                .getDependentTaskNames()
                .contains(name))
    }

    @TestFactory
    fun `create variant-specific jacoco task`() = forEachVariant(
            beforeBuild = { it.applyJacocoPlugin() }
    ) { project, variant ->
        val name = jacocoVariantTaskName(variant)
        assertThat(project.tasks.findByName(name)).isNotNull()
    }

    @TestFactory
    fun `hook in variant-specific jacoco task to parent`() = forEachVariant(
            beforeBuild = { it.applyJacocoPlugin() }
    ) { project, variant ->
        assertThat(project.tasks.getByName(JACOCO_TASK_NAME)
                .getDependentTaskNames())
                .contains(jacocoVariantTaskName(variant))
    }

    @TestFactory
    fun `jacoco task includes main-scoped source directories`() = forEachBuildType(
            beforeBuild = { it.applyJacocoPlugin() }
    ) { project, buildType ->
        val name = jacocoVariantTaskName(buildType)
        val sourceDirs = project.tasks.get<AndroidJUnit5JacocoReport>(name)
                .sourceDirectories!!
                .map { it.absolutePath }

        // Expected items: "src/main/java" & "src/<TypeName>/java"
        val mainDir = sourceDirs.find { it.endsWith("src/main/java") }
        val typeDir = sourceDirs.find { it.endsWith("src/$buildType/java") }

        assertAll(
                "Mismatch! Actual dirs: $sourceDirs",
                { assertWithMessage("main").that(mainDir).isNotNull() },
                { assertWithMessage(buildType).that(typeDir).isNotNull() }
        )
    }

    @TestFactory
    fun `jacoco task does not include test-scoped source directories`() = forEachBuildType(
            beforeBuild = { it.applyJacocoPlugin() }
    ) { project, buildType ->
        val name = jacocoVariantTaskName(buildType)
        val sourceDirs = project.tasks.get<AndroidJUnit5JacocoReport>(name)
                .sourceDirectories!!.asPath

        // Expected omissions: "src/test/java" & "src/test<TypeName>/java"
        assertAll(
                "Mismatch! Actual dirs: $sourceDirs",
                { assertThat(sourceDirs).doesNotContain("src/test/java") },
                {
                    assertThat(sourceDirs).doesNotContain("src/test${buildType.capitalize()}/java")
                }
        )
    }

    @TestFactory
    fun `jacoco task does not include test-scoped class directories`() = forEachBuildType(
            beforeBuild = { it.applyJacocoPlugin() }
    ) { project, buildType ->
        val name = jacocoVariantTaskName(buildType)
        val classDirs = project.tasks.get<AndroidJUnit5JacocoReport>(name)
                .classDirectories!!.asPath

        // Expected omissions: "classes/test"
        assertThat(classDirs).doesNotContain("classes/test")
    }

    @TestFactory
    fun `only generate jacoco task for debug builds`(): List<DynamicTest> {
        val project = createProject().applyJacocoPlugin().build()
        project.junitPlatform.jacocoOptions {
            onlyGenerateTasksForVariants("debug")
        }
        project.evaluate()

        return listOf(
                JACOCO_TASK_NAME to true,
                "${JACOCO_TASK_NAME}Debug" to true,
                "${JACOCO_TASK_NAME}Release" to false
        ).map { (taskName, shouldExist) ->
            dynamicTest("$taskName task is${if (shouldExist) " " else " not"} generated") {
                val task = project.tasks.findByName(taskName)

                if (shouldExist) {
                    assertThat(task).isNotNull()
                } else {
                    assertThat(task).isNull()
                }
            }
        }
    }

    @TestFactory
    fun `only generate jacoco task for certain variants`(): List<DynamicTest> {
        val project = createProject().applyJacocoPlugin().build()
        project.registerProductFlavors()
        project.junitPlatform.jacocoOptions {
            onlyGenerateTasksForVariants("paidDebug", "freeRelease")
        }
        project.evaluate()

        return listOf(
                JACOCO_TASK_NAME to true,
                "${JACOCO_TASK_NAME}PaidDebug" to true,
                "${JACOCO_TASK_NAME}FreeRelease" to true,
                "${JACOCO_TASK_NAME}PaidRelease" to false,
                "${JACOCO_TASK_NAME}FreeDebug" to false
        ).map { (taskName, shouldExist) ->
            dynamicTest("$taskName task is${if (shouldExist) " " else " not"} generated") {
                val task = project.tasks.findByName(taskName)

                if (shouldExist) {
                    assertThat(task).isNotNull()
                } else {
                    assertThat(task).isNull()
                }
            }
        }
    }
}
