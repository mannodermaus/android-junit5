package de.mannodermaus.gradle.plugins.junit5.providers

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SourceProvider
import de.mannodermaus.gradle.plugins.junit5.unitTestVariant
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/* Types */

/**
 * Provides test root directories for Kotlin sources,
 * with which a JUnit 5 Task can be enhanced.
 *
 * Note: The resulting Sets might be empty for modules powered by AGP2:
 * The legacy Kotlin integration automatically copies over Kotlin classes
 * into the Java directories, which renders dedicated Gradle tasks useless.
 */
class KotlinDirectoryProvider(
    private val project: Project,
    private val variant: BaseVariant) : DirectoryProvider {

  override fun mainSourceDirectories() = sourceFoldersOf(variant)
  override fun testSourceDirectories() = sourceFoldersOf(variant.unitTestVariant)
  override fun mainClassDirectories() = classFoldersOf(variant)
  override fun testClassDirectories() = classFoldersOf(variant.unitTestVariant)

  /* Private */

  private fun sourceFoldersOf(variant: BaseVariant) =
      variant.sourceSets
          .flatMap { it.kotlin.srcDirs }
          .toSet()

  private fun classFoldersOf(variant: BaseVariant): Set<File> {
    val kotlinTask = project.tasks.findByName(variant.kotlinTaskName) ?: return emptySet()
    return setOf((kotlinTask as KotlinCompile).destinationDir)
  }
}

/* Extensions */

private val BaseVariant.kotlinTaskName
  get() = "compile${this.name.capitalize()}Kotlin"

private val SourceProvider.kotlin: SourceDirectorySet
  get() {
    if (this !is HasConvention) {
      throw IllegalArgumentException("Argument doesn't have Conventions: $this")
    }

    val kotlinConvention = this.convention.plugins[KOTLIN_DSL_NAME] as KotlinSourceSet
    return kotlinConvention.kotlin
  }
