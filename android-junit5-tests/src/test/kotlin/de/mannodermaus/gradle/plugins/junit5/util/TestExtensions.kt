package de.mannodermaus.gradle.plugins.junit5.util

import de.mannodermaus.gradle.plugins.junit5.FunctionalTests
import org.apache.commons.lang.StringUtils
import org.assertj.core.api.AbstractAssert
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskContainer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
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

fun assertThat(buildResult: BuildResult) = AssertBuildResult(buildResult)

class AssertBuildResult(buildResult: BuildResult) : AbstractAssert<AssertBuildResult, BuildResult>(
    buildResult, AssertBuildResult::class.java) {
  fun executedTaskSuccessfully(name: String) {
    val task = actual.task(name) ?: throw AssertionError("didn't execute task $name")
    org.assertj.core.api.Assertions.assertThat(task.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  fun hasOutputContaining(substring: String, times: Int = 1) {
    val actualCount = StringUtils.countMatches(actual.output, substring)
    org.assertj.core.api.Assertions.assertThat(actualCount)
        .withFailMessage(
            "expected substring '$substring' to be contained $times times in output, but it was actually contained $actualCount times")
        .isEqualTo(times)
  }
}

fun loadClassPathManifestResource(name: String): List<File> {
  val classpathResource = FunctionalTests::class.java.classLoader.getResourceAsStream(name)
      ?: throw IllegalStateException("Did not find required resource with name $name")

  return classpathResource.bufferedReader()
      .lineSequence()
      .map { File(it) }
      .toList()
}

/* Extensions */

fun Project.evaluate() {
  (this as ProjectInternal).evaluate()
}

@Suppress("UNCHECKED_CAST")
fun <T : Task> TaskContainer.get(name: String): T =
    this.getByName(name) as T

fun JavaExec.getArgument(name: String): String? =
    TaskUtils.argument(this, name)

fun File.newFile(filePath: String, separator: String = "/"): File {
  val path = Paths.get(this.toString(),
      *filePath.splitToArray(delimiter = separator))
  path.parent.mkdirs()
  return path.toFile()
}

fun String.countMatches(sub: String) = StringUtils.countMatches(this, sub)

fun String.splitToArray(delimiter: String = "/"): Array<String> =
    this.split(delimiter).toTypedArray()

fun Path.mkdirs() = Files.createDirectories(this)

fun Path.newFile(path: String) = this.resolve(path).toFile()

/* Operators */

/**
 * Produces the [cartesian product](http://en.wikipedia.org/wiki/Cartesian_product#n-ary_product) as a sequence of ordered pairs of elements lazily obtained
 * from two [[Iterable]] instances
 */
operator fun <T : Any> Iterable<T>.times(other: Iterable<T>): Sequence<Pair<T, T>> {
  val first = iterator()
  var second = other.iterator()
  var a: T? = null

  fun nextPair(): Pair<T, T>? {
    if (a == null && first.hasNext()) a = first.next()
    if (second.hasNext()) return Pair(a!!, second.next())
    if (first.hasNext()) {
      a = first.next(); second = other.iterator()
      return Pair(a!!, second.next())
    }
    return null
  }

  return generateSequence { nextPair() }
}
