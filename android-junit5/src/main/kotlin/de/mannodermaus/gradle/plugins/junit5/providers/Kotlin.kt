package de.mannodermaus.gradle.plugins.junit5.providers

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.unitTestVariant
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/**
 * Provides test root directories for Kotlin sources,
 * with which a JUnit 5 Task can be enhanced.
 */
class KotlinTestRootDirectoryProvider(
    private val project: Project,
    private val variant: BaseVariant) : TestRootDirectoryProvider {

  override fun testRootDirectories(): Set<File> {
    // Hook in the Kotlin destination directories to the JUnit 5 Task.
    // Note: The resulting Set might be empty for modules powered by AGP2:
    // The legacy Kotlin integration automatically copies over Kotlin classes
    // into the Java directories, which renders dedicated Gradle tasks useless.
    val kotlinTaskNames = listOf(
        kotlinTaskName(variant),
        kotlinTaskName(variant.unitTestVariant))

    return kotlinTaskNames
        .map { project.tasks.findByName(it) }
        .filter { it != null }
        .map { it as KotlinCompile }
        .map { it.destinationDir }
        .toSet()
  }

  private fun kotlinTaskName(variant: BaseVariant) =
      "compile${variant.name.capitalize()}Kotlin"
}
