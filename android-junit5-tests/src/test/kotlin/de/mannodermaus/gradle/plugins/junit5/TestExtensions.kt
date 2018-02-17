package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.util.TaskUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskContainer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable

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

/* Extensions */

fun Project.evaluate() {
  (this as ProjectInternal).evaluate()
}

@Suppress("UNCHECKED_CAST")
fun <T : Task> TaskContainer.get(name: String): T =
    this.getByName(name) as T

fun JavaExec.getArgument(name: String): String? =
    TaskUtils.argument(this, name)

/* Operators */

/**
 * Produces the [cartesian product](http://en.wikipedia.org/wiki/Cartesian_product#n-ary_product) as a sequence of ordered pairs of elements lazily obtained
 * from two [[Iterable]] instances
 */
operator fun <T: Any> Iterable<T>.times(other: Iterable<T>): Sequence<Pair<T, T>> {
  val first = iterator(); var second = other.iterator(); var a: T? = null

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
