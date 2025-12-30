package de.mannodermaus.gradle.plugins.junit5.tasks

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.config.INSTRUMENTATION_FILTER_RES_FILE_NAME
import de.mannodermaus.gradle.plugins.junit5.plugin.TestProjectProviderExtension
import de.mannodermaus.gradle.plugins.junit5.util.assertAll
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.readLines
import org.gradle.api.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AndroidJUnit5WriteFiltersTests {
    @RegisterExtension @JvmField val projectExtension = TestProjectProviderExtension()

    private lateinit var project: Project

    @BeforeEach
    fun beforeEach() {
        project =
            projectExtension
                .newProject()
                .asAndroidApplication()
                .applyJUnit5Plugin(true) { junitPlatform ->
                    junitPlatform.filters().includeTags("included")
                    junitPlatform.filters().excludeTags("excluded", "another-group")
                }
                .build()
        project.evaluate()
    }

    @Test
    fun `generates file structure correctly`() {
        // Expect a 'raw' folder inside the output, then the actual filters file in that sub-folder
        val output = project.runTaskAndGetOutputFolder()

        File(output, "raw").apply {
            assertAll(
                "output contains 'raw' folder",
                { assertThat(exists()).isTrue() },
                { assertThat(isDirectory).isTrue() },
            )

            File(this, INSTRUMENTATION_FILTER_RES_FILE_NAME).apply {
                assertAll(
                    "'raw' folder contains filters file'",
                    { assertThat(exists()).isTrue() },
                    { assertThat(isFile).isTrue() },
                )
            }
        }
    }

    @Test
    fun `file contains expected content`() {
        val output = project.runTaskAndGetOutputFolder()
        val file = Paths.get(output.absolutePath, "raw", INSTRUMENTATION_FILTER_RES_FILE_NAME)

        val content = file.readLines()
        assertThat(content).containsExactly("-t included", "-T excluded", "-T another-group")
    }

    /* Private */

    private fun Project.runTaskAndGetOutputFolder(): File {
        val task =
            project.tasks.getByName("writeFiltersDebugAndroidTest") as AndroidJUnit5WriteFilters
        task.execute()
        return requireNotNull(task.outputFolder.get().asFile)
    }
}
