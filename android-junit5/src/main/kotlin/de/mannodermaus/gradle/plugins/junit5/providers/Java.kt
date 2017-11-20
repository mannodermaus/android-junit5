package de.mannodermaus.gradle.plugins.junit5.providers

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.unitTestVariant
import de.mannodermaus.gradle.plugins.junit5.variantData

/**
 * Default Provider implementation for Java-based test root directories.
 * This will look up the main & test root directories
 * of the variant connected to a given JUnit 5 task.
 */
class JavaDirectoryProvider(private val variant: BaseVariant) : DirectoryProvider {

  override fun mainSourceDirectories() =
      javaSourceFoldersOf(variant)

  override fun mainClassDirectories() =
      javaClassFoldersOf(variant)

  override fun testSourceDirectories() =
      javaSourceFoldersOf(variant.unitTestVariant)

  override fun testClassDirectories() =
      javaClassFoldersOf(variant.unitTestVariant)

  /* Private */

  private fun javaSourceFoldersOf(variant: BaseVariant) =
      variant.sourceSets
          .flatMap { it.javaDirectories }
          .toSet()

  private fun javaClassFoldersOf(variant: BaseVariant) =
      setOf(variant.variantData.scope.javaOutputDir)
}
