import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/* RepositoryHandler */

fun RepositoryHandler.jitpack() = maven {
    setUrl("https://jitpack.io")
}

fun RepositoryHandler.sonatypeSnapshots() = maven {
    setUrl("https://oss.sonatype.org/content/repositories/snapshots")
}

/* Project */

fun Project.fixCompileTaskChain() {
    setupCompileChain(
        sourceCompileName = "compileKotlin",
        targetCompileName = "compileGroovy"
    )

    setupCompileChain(
        sourceCompileName = "compileTestKotlin",
        targetCompileName = "compileTestGroovy"
    )
}

/**
 * @param sourceCompileName The sources in this task may call into the target
 * @param targetCompileName The sources in this task must not call into the source
 */
private fun Project.setupCompileChain(
    sourceCompileName: String,
    targetCompileName: String
) {
    val targetCompile = tasks.getByName(targetCompileName) as AbstractCompile
    val sourceCompile = tasks.getByName(sourceCompileName)

    // Allow calling the source language's classes from the target language.
    // In this case, we allow calling Kotlin from Groovy - it has to be noted however,
    // that the other way does not work!
    val sourceDir = sourceCompile.withGroovyBuilder { getProperty("destinationDirectory") } as DirectoryProperty
    targetCompile.classpath += project.files(sourceDir.get().asFile)
}
