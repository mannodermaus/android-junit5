package de.mannodermaus.gradle.plugins.junit5.util.projects

import com.soywiz.korte.TeFunction
import com.soywiz.korte.TemplateConfig
import com.soywiz.korte.TemplateProvider
import com.soywiz.korte.Templates
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Processor class for virtual build script files, used by Functional Tests.
 * It utilizes a template engine to customize the processed output for the build scripts
 * injected into the virtual projects, based around template files located within src/test/resources.
 */
class BuildScriptTemplateProcessor(
  folder: File,
  private val replacements: Map<String, Any>,
  private val agpVersion: String,
  private val gradleVersion: String,
  private val junitVersion: String,
) {

  private val renderer = Templates(
    root = FileReadingTemplateProvider(folder),
    cache = true,
    config = TemplateConfig(
      // Allow checking for AGP & Gradle versions inside the templates
      extraFunctions = listOf(
        TeFunction("atLeastAgp") { args ->
          isVersionAtLeast(agpVersion, args[0].toDynamicString())
        },
        TeFunction("atLeastGradle") { args ->
          isVersionAtLeast(gradleVersion, args[0].toDynamicString())
        },
        TeFunction("atLeastJUnit") { args ->
          isVersionAtLeast(junitVersion, args[0].toDynamicString())
        }
      )
    )
  )

  fun process(fileName: String): String = runBlocking {
    renderer.render(fileName, replacements)
  }

  /* Private */

  private fun isVersionAtLeast(actual: String, required: String): Boolean {
    val actualVersion = SemanticVersion(actual)
    val requiredVersion = SemanticVersion(required)
    return actualVersion >= requiredVersion
  }

  private class FileReadingTemplateProvider(private val folder: File) : TemplateProvider {
    override suspend fun get(template: String): String {
      return File(folder, template).readText()
    }
  }
}
