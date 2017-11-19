package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Helper Task class writing a classpath into a file.
 * Used for tests that verify the plugin's behavior
 * using locally built dependencies.
 *
 * More info:
 * https://docs.gradle.org/current/userguide/test_kit.html#sub:test-kit-classpath-injection
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
open class WriteClasspathResource : DefaultTask() {

  @InputFiles
  var inputFiles: FileCollection? = null
  @OutputDirectory
  var outputDir: File? = null
  var resourceFileName: String? = null

  override fun getDescription() = "Generates a local classpath resource for functional tests"
  override fun getGroup() = "build"

  @TaskAction
  fun doWork() {
    if (inputFiles == null) {
      throw IllegalStateException("ClasspathWriteTask requires 'inputFiles'")
    }
    val inputFiles = this.inputFiles!!

    if (resourceFileName == null) {
      throw IllegalStateException("ClasspathWriteTask requires 'resourceFileName'")
    }
    val resourceFileName = this.resourceFileName!!

    if (outputDir == null) {
      throw IllegalStateException("ClasspathWriteTask requires 'outputDir'")
    }
    val outputDir = this.outputDir!!

    outputDir.mkdirs()
    val outputFile = File(outputDir, resourceFileName)
    outputFile.writer(Charsets.UTF_8).use {
      it.write(inputFiles.joinToString(separator = System.lineSeparator()))
    }
  }
}
