import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File

/**
 * Helper Task class writing a classpath into a file.
 * Used for tests that verify the plugin's behavior
 * using locally built dependencies.
 *
 * More info:
 * https://docs.gradle.org/current/userguide/test_kit.html#sub:test-kit-classpath-injection
 */
open class WriteClasspathResource : DefaultTask() {

  @InputFiles
  lateinit var inputFiles: Iterable<File>
  @OutputDirectory
  lateinit var outputDir: File
  lateinit var resourceFileName: String

  override fun getDescription() = "Generates a local classpath resource for functional tests"
  override fun getGroup() = "build"

  @TaskAction
  fun doWork() {
    outputDir.mkdirs()
    val outputFile = File(outputDir, resourceFileName)
    outputFile.writer(Charsets.UTF_8).use {
      it.write(inputFiles.joinToString(separator = System.lineSeparator()))
    }
  }
}

/**
 * Helper Task class for generating an up-to-date version of the project's README.md.
 * Using a template file, the plugin's version constants & other dependency versions
 * are automatically injected into the README.
 */
open class GenerateReadme : DefaultTask() {

  companion object {
    private val PLACEHOLDER_REGEX = Regex("\\\$\\{(.+)}")
    private val EXTERNAL_DEP_REGEX = Regex("Versions\\.(.+)")

    private const val PLUGIN_VERSION = "pluginVersion"
    private const val INSTRUMENTATION_VERSION = "instrumentationVersion"
  }

  @InputFile
  lateinit var inputTemplateFile: File
  @OutputFile
  lateinit var outputFile: File

  override fun getDescription() = "Generates the README.md file from a template"
  override fun getGroup() = "documentation"

  @TaskAction
  fun doWork() {
    val templateText = inputTemplateFile.readText()
    val replacedText = replacePlaceholdersInTemplate(templateText)
    outputFile.writeText(replacedText)
  }

  /* Private */

  private fun replacePlaceholdersInTemplate(templateText: String): String {
    // Apply placeholders in the template with data from Versions.kt & Artifacts.kt:
    // ${pluginVersion}             Artifacts.Plugin.currentVersion
    // ${instrumentationVersion}    Artifacts.Instrumentation.Core.currentVersion
    // ${Versions.<xxx>}            (A constant value taken from Versions.kt)
    val allPlaceholders = mutableMapOf<String, String>()

    PLACEHOLDER_REGEX.findAll(templateText).forEach { match ->
      val placeholder = match.groups.last()?.value
          ?: throw InvalidPlaceholder(match)

      // Local versions (plugin, instrumentation)
      val replacement = when (placeholder) {
        PLUGIN_VERSION -> Artifacts.Plugin.anyStableVersion
        INSTRUMENTATION_VERSION -> Artifacts.Instrumentation.Core.anyStableVersion
        else -> {
          val match2 = EXTERNAL_DEP_REGEX.find(placeholder)
              ?: throw InvalidPlaceholder(match)
          val externalDependency = match2.groups.last()?.value
              ?: throw InvalidPlaceholder(match2)

          val field = Versions.javaClass.getField(externalDependency)
          field.get(null) as String
        }
      }

      // Save placeholder
      allPlaceholders["\${$placeholder}"] = replacement
    }

    var replacedText = templateText
    allPlaceholders.forEach { (key, value) ->
      replacedText = replacedText.replace(key, value)
    }
    return replacedText
  }
}

private class InvalidPlaceholder(matchResult: MatchResult) : Exception("Invalid match result: '${matchResult.groupValues}'")

private val Deployed.anyStableVersion: String
  get() = if (currentVersion.endsWith("-SNAPSHOT")) {
    latestStableVersion
  } else {
    currentVersion
  }
