package de.mannodermaus.gradle.plugins.junit5.util

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Michael Clausen - encodeering@gmail.com
 */
inline fun <reified T : Throwable> throws(block: () -> Unit): T {
    var ex: Throwable? = null
    var thrown = false
    var matches = false

    try {
        block()
    } catch (e: Throwable) {
        ex = e
        matches = T::class.isInstance(e)
        thrown = true

    } finally {
        if (!matches && ex != null) throw AssertionError(
                "block should have thrown a ${T::class.simpleName}, but threw a ${ex.javaClass.simpleName}")
        if (!thrown) throw AssertionError("block should have thrown a ${T::class.simpleName}")
    }

    return ex as T
}

fun assertAll(vararg assertions: () -> Unit) {
    Assertions.assertAll(*assertions.map { Executable { it() } }.toTypedArray())
}

fun assertAll(heading: String, vararg assertions: () -> Unit) {
    Assertions.assertAll(heading, *assertions.map { Executable { it() } }.toTypedArray())
}

/* Extensions */

fun Project.evaluate() {
    (this as ProjectInternal).evaluate()
}

fun Project.applyPlugin(pluginId: String) = apply { it.plugin(pluginId) }

@Suppress("UNCHECKED_CAST")
fun <T : Task> TaskContainer.get(name: String): T =
        this.getByName(name) as T

fun Task.getDependentTaskNames(): List<String> =
        this.dependsOn.map { dependent ->
            when (dependent) {
                is Task -> dependent.name
                is TaskProvider<*> -> dependent.name
                else -> throw IllegalArgumentException("don't know how to extract task name from: $dependent")
            }
        }

fun File.newFile(filePath: String, separator: String = "/"): File {
    val path = Paths.get(this.toString(),
            *filePath.splitToArray(delimiter = separator))
    path.parent.mkdirs()
    return path.toFile()
}

fun String.splitToArray(delimiter: String = "/"): Array<String> =
        this.split(delimiter).toTypedArray()

fun Path.mkdirs() = Files.createDirectories(this)

fun Path.newFile(path: String) = this.resolve(path).toFile()

val Test.junitPlatformOptions: JUnitPlatformOptions
    get() = (this.testFramework as JUnitPlatformTestFramework).options

fun List<File>.splitClasspath() = this
        .map { it.absolutePath.replace("\\", "\\\\") }
        .joinToString(", ") { "'$it'" }

fun GradleRunner.withPrunedPluginClasspath(agpVersion: TestedAgp) = also {
    val fileKey = agpVersion.fileKey
    val cl = Thread.currentThread().contextClassLoader
    val url = cl.getResource("pruned-plugin-metadata-$fileKey.properties")
    withPluginClasspath(PluginUnderTestMetadataReading.readImplementationClasspath(url))
}

/* Operators */

/**
 * Produces the [cartesian product](http://en.wikipedia.org/wiki/Cartesian_product#n-ary_product) as a sequence of ordered pairs of elements lazily obtained
 * from two [[Iterable]] instances
 */
operator fun <T : Any, U : Any> Iterable<T>.times(other: Iterable<U>): Sequence<Pair<T, U>> {
    val first = iterator()
    var second = other.iterator()
    var a: T? = null

    fun nextPair(): Pair<T, U>? {
        if (a == null && first.hasNext()) a = first.next()
        if (second.hasNext()) return Pair(a!!, second.next())
        if (first.hasNext()) {
            a = first.next(); second = other.iterator()
            return Pair(a, second.next())
        }
        return null
    }

    return generateSequence { nextPair() }
}

fun BuildResult.prettyPrint() {
    // Indent every line to separate it from 'actual' Gradle output
    val prefix = "[BuildResult-${hashCode()}]    "
    val fixedOutput = this.output.lines()
        .joinToString("\n") { "$prefix$it" }

    println(fixedOutput)
}
