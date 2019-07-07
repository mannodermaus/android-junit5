package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.gradle.api.TestVariant
import de.mannodermaus.gradle.plugins.junit5.INSTRUMENTATION_FILTER_RES_FILE_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.junit5ConfigurationOf
import de.mannodermaus.gradle.plugins.junit5.variantData
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

private const val TASK_NAME_DEFAULT = "writeFilters"

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
open class AndroidJUnit5WriteFilters : DefaultTask() {

  companion object {
    fun create(project: Project,
               instrumentationTestVariant: TestVariant): AndroidJUnit5WriteFilters {
      val configAction = ConfigAction(project, instrumentationTestVariant)
      return project.tasks.create(configAction.name, configAction.type) {
        configAction.execute(it)
      }
    }
  }

  private lateinit var variant: TestVariant

  @Input
  var includeTags = emptyList<String>()
  @Input
  var excludeTags = emptyList<String>()

  @OutputDirectory
  var outputFolder: File? = null

  @TaskAction
  fun execute() {
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
      private val instrumentationTestVariant: TestVariant
  ) {

    private val scope = instrumentationTestVariant.variantData.scope

    val name: String = scope.getTaskName(TASK_NAME_DEFAULT)

    val type = AndroidJUnit5WriteFilters::class.java

    fun execute(task: AndroidJUnit5WriteFilters) {
      task.variant = instrumentationTestVariant
      task.outputFolder = File("${project.buildDir}/generated/res/android-junit5/androidTest/${instrumentationTestVariant.name})")

      // Access filters for this particular variant & provide them to the task, too
      val configuration = project.junit5ConfigurationOf(instrumentationTestVariant.testedVariant)
      task.includeTags = configuration.combinedIncludeTags.toList()
      task.excludeTags = configuration.combinedExcludeTags.toList()
    }
  }
}
