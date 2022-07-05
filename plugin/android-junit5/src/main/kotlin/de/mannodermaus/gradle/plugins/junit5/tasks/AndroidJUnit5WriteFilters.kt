@file:Suppress("DEPRECATION")

package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.api.variant.Variant
import com.android.build.gradle.api.TestVariant
import de.mannodermaus.gradle.plugins.junit5.internal.config.INSTRUMENTATION_FILTER_RES_FILE_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.config.JUnit5TaskConfig
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.getTaskName
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Helper task for instrumentation tests.
 * It writes out the filters configured through the Gradle plugin's DSL
 * into a resource file, used at runtime to set up the execution of the JUnit Platform.
 *
 * Note:
 * This only allows tests to be filtered with @Tag annotations even in the instrumentation test realm.
 * Other plugin DSL settings, like includeEngines/excludeEngines or includePattern/excludePattern
 * are not copied out to file. This has to do with limitations of the backport implementation
 * of the JUnit Platform Runner, as well as some incompatibilities between Gradle and Java with regards to
 * how class name patterns are formatted.
 */
@CacheableTask
public abstract class AndroidJUnit5WriteFilters : DefaultTask() {

    internal companion object {
        fun register(
            project: Project,
            variant: Variant,
            instrumentationTestVariant: TestVariant
        ): Boolean {
            val outputFolder = File("${project.buildDir}/generated/res/android-junit5/${instrumentationTestVariant.name}")
            val configAction = ConfigAction(project, variant, outputFolder)

            val provider = project.tasks.register(
                configAction.name,
                configAction.type,
                configAction::execute
            )

            // Connect the output folder of the task to the instrumentation tests
            // so that they are bundled into the built test application
            instrumentationTestVariant.registerGeneratedResFolders(
                project.files(outputFolder).builtBy(provider)
            )
            instrumentationTestVariant.mergeResourcesProvider.configure { it.dependsOn(provider) }

            return true
        }
    }

    @Input
    public var includeTags: List<String> = emptyList()

    @Input
    public var excludeTags: List<String> = emptyList()

    @OutputDirectory
    public var outputFolder: File? = null

    @TaskAction
    public fun execute() {
        this.outputFolder?.let { folder ->
            // Clear out current contents of the generated folder
            folder.deleteRecursively()

            if (this.hasFilters()) {
                folder.mkdirs()

                // Re-write the new file structure into it;
                // the generated file will have a fixed name & is located
                // as a "raw" resource inside the output folder
                val rawFolder = File(folder, "raw").apply { mkdirs() }
                File(rawFolder, INSTRUMENTATION_FILTER_RES_FILE_NAME)
                    .bufferedWriter()
                    .use { writer ->
                        // This format is a nod towards the real JUnit 5 ConsoleLauncher's arguments
                        includeTags.forEach { tag -> writer.write("-t $tag") }
                        excludeTags.forEach { tag -> writer.write("-T $tag") }
                    }
            }
        }
    }

    private fun hasFilters() = includeTags.isNotEmpty() || excludeTags.isNotEmpty()

    private class ConfigAction(
        private val project: Project,
        private val variant: Variant,
        private val outputFolder: File
    ) {

        val name: String = variant.getTaskName(prefix = "writeFilters", suffix = "androidTest")

        val type = AndroidJUnit5WriteFilters::class.java

        fun execute(task: AndroidJUnit5WriteFilters) {
            task.outputFolder = outputFolder

            // Access filters for this particular variant & provide them to the task, too
            val configuration = JUnit5TaskConfig(variant, project.junitPlatform)
            task.includeTags = configuration.combinedIncludeTags.toList()
            task.excludeTags = configuration.combinedExcludeTags.toList()
        }
    }
}
