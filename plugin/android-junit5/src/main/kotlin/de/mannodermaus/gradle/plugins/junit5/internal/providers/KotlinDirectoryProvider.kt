package de.mannodermaus.gradle.plugins.junit5.internal.providers

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SourceProvider
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.agpLog
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.unitTestVariant
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.logging.LogLevel.WARN
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/* Types */

/**
 * Provides test root directories for Kotlin sources,
 * with which a JUnit 5 Task can be enhanced.
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
        val kotlinTask = project.tasks.findByName(variant.kotlinTaskName)
        return if (kotlinTask != null) {
            // Read folder directly from the Kotlin task
            setOf((kotlinTask as KotlinCompile).destinationDir)
        } else {
            // If the Kotlin plugin is applied _after_ JUnit 5 in the build file,
            // fall back to the expected path… However, make sure to log a warning to users!
            project.logger.agpLog(WARN, "The kotlin-android plugin is currently applied after android-junit5! To guarantee full compatibility, please declare it above the JUnit 5 plugin.")
            setOf(File(project.buildDir, "tmp/kotlin-classes/${variant.name}"))
        }
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
