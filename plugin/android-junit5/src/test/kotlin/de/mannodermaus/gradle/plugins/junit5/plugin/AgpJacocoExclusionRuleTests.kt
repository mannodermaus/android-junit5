package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.JACOCO_TASK_NAME
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import de.mannodermaus.gradle.plugins.junit5.util.get
import org.gradle.api.Project
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

interface AgpJacocoExclusionRuleTests : AgpVariantAwareTests {

    private fun createPreparedProject(): Project {
        val project = createProject().applyJacocoPlugin().build()

        // Create some fake files to verify the Jacoco tree
        project.createFakeFiles()

        return project
    }

    @TestFactory
    fun `add exclusion rules`(): List<DynamicTest> {
        val project = createPreparedProject()
        project.junitPlatform.jacocoOptions {
            it.excludedClasses.add("Second*.class")
        }
        project.evaluate()

        return listOf(
                dynamicTest("honor the debug class exclusion rules") {
                    // Should be included:
                    //  * FirstFile.class
                    // Should be excluded:
                    //  * R.class (by default)
                    //  * SecondFile.class (through rule)
                    val fileNames = project.tasks.get<AndroidJUnit5JacocoReport>(
                            "${JACOCO_TASK_NAME}Debug")
                            .classDirectories!!.asFileTree.files
                            .map { it.name }

                    assertThat(fileNames).apply {
                        contains("FirstFile.class")
                        doesNotContain("R.class")
                        doesNotContain("SecondFile.class")
                    }
                },

                dynamicTest("honor the release class exclusion rules") {
                    // Should be included:
                    //  (nothing)
                    // Should be excluded:
                    //  * R.class (by default)
                    //  * FirstFile.class (other source set)
                    //  * SecondFile.class (through rule)
                    val fileNames = project.tasks.get<AndroidJUnit5JacocoReport>(
                            "${JACOCO_TASK_NAME}Release")
                            .classDirectories!!.asFileTree.files
                            .map { it.name }

                    assertThat(fileNames).apply {
                        doesNotContain("R.class")
                        doesNotContain("FirstFile.class")
                        doesNotContain("SecondFile.class")
                    }
                }
        )
    }

    @TestFactory
    fun `replace exclusion rules`() = forEachBuildType(
            beforeBuild = { it.applyJacocoPlugin() },
            beforeEvaluate = { project ->
                project.createFakeFiles()
                project.junitPlatform.jacocoOptions {
                    it.excludedClasses = mutableListOf()
                }
            }
    ) { project, buildType ->
        val name = "${JACOCO_TASK_NAME}${buildType.capitalize()}"
        val fileNames = project.tasks.get<AndroidJUnit5JacocoReport>(name)
                .classDirectories!!.asFileTree.files
                .map { it.name }

        // Should not exclude R.class any longer
        assertThat(fileNames).contains("R.class")
    }
}

private fun Project.createFakeFiles() {
    // Since the location of intermediate class files changed in different versions of the Android Gradle Plugin,
    // create each class file in multiple directories to remain compatible with all approaches.
    // First up, populate the empty folder structures, then add the files to them
    fakeFiles
            .map { it.substringBeforeLast("/") }
            .distinct()
            .forEach { folder -> this.file(folder).mkdirs() }

    fakeFiles.forEach { file -> this.file(file).createNewFile() }
}

private val fakeFiles = listOf(
        // Debug classes
        "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/R.class",
        "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/FirstFile.class",
        "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/SecondFile.class",
        "build/intermediates/classes/debug/R.class",
        "build/intermediates/classes/debug/FirstFile.class",
        "build/intermediates/classes/debug/SecondFile.class",
        "build/intermediates/javac/debug/classes/R.class",
        "build/intermediates/javac/debug/classes/FirstFile.class",
        "build/intermediates/javac/debug/classes/SecondFile.class",

        // Release classes
        "build/intermediates/javac/release/compileReleaseJavaWithJavac/classes/R.class",
        "build/intermediates/javac/release/compileReleaseJavaWithJavac/classes/SecondFile.class",
        "build/intermediates/classes/release/R.class",
        "build/intermediates/classes/release/SecondFile.class",
        "build/intermediates/javac/release/classes/R.class",
        "build/intermediates/javac/release/classes/SecondFile.class",

        // Source files
        "src/main/java/OkFile.java",
        "src/main/java/AnnoyingFile.java",
        "src/release/java/ReleaseOnlyFile.java"
)
