package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.api.variant.SourceDirectories
import com.android.build.api.variant.Variant
import de.mannodermaus.gradle.plugins.junit5.internal.config.INSTRUMENTATION_FILTER_RES_FILE_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.config.JUnitPlatformTaskConfig
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.getTaskName
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Helper task for instrumentation tests. It writes out the filters configured through the Gradle
 * plugin's DSL into a resource file, used at runtime to set up the execution of the JUnit Platform.
 *
 * Note: This only allows tests to be filtered with @Tag annotations even in the instrumentation
 * test realm. Other plugin DSL settings, like includeEngines/excludeEngines or
 * includePattern/excludePattern are not copied out to file. This has to do with limitations of the
 * backport implementation of the JUnit Platform Runner, as well as some incompatibilities between
 * Gradle and Java regarding how class name patterns are formatted.
 */
@CacheableTask
public abstract class AndroidJUnit5WriteFilters : DefaultTask() {

    internal companion object {
        fun register(
            project: Project,
            variant: Variant,
            sourceDirs: SourceDirectories.Layered,
        ): Boolean {
            val configAction = ConfigAction(project, variant)

            val provider =
                project.tasks.register(configAction.name, configAction.type, configAction::execute)

            // Connect the output folder of the task to the instrumentation tests
            // so that they are bundled into the built test application
            sourceDirs.addGeneratedSourceDirectory(
                taskProvider = provider,
                wiredWith = AndroidJUnit5WriteFilters::outputFolder,
            )

            return true
        }
    }

    @get:Input public abstract val includeTags: ListProperty<String>

    @get:Input public abstract val excludeTags: ListProperty<String>

    @get:OutputDirectory public abstract val outputFolder: DirectoryProperty

    @TaskAction
    public fun execute() {
        // Clear out current contents of the generated folder
        val folder = outputFolder.get().asFile
        folder.deleteRecursively()

        val includeTags = includeTags.get()
        val excludeTags = excludeTags.get()

        if (includeTags.isNotEmpty() || excludeTags.isNotEmpty()) {
            folder.mkdirs()

            // Re-write the new file structure into it;
            // the generated file will have a fixed name & is located
            // as a "raw" resource inside the output folder
            val rawFolder = File(folder, "raw").apply { mkdirs() }
            File(rawFolder, INSTRUMENTATION_FILTER_RES_FILE_NAME).bufferedWriter().use { writer ->
                // This format is a nod towards the real JUnit 5 ConsoleLauncher's arguments
                includeTags.forEach { tag -> writer.appendLine("-t $tag") }
                excludeTags.forEach { tag -> writer.appendLine("-T $tag") }
            }
        }
    }

    private class ConfigAction(private val project: Project, private val variant: Variant) {

        val name: String = variant.getTaskName(prefix = "writeFilters", suffix = "androidTest")

        val type = AndroidJUnit5WriteFilters::class.java

        fun execute(task: AndroidJUnit5WriteFilters) {
            // Access filters for this particular variant & provide them to the task
            val configuration = JUnitPlatformTaskConfig(variant, project.junitPlatform)
            task.includeTags.set(configuration.combinedIncludeTags.toList())
            task.excludeTags.set(configuration.combinedExcludeTags.toList())

            // Output folder is applied by Android Gradle Plugin, so there is no reason to provide a
            // value ourselves
        }
    }
}
