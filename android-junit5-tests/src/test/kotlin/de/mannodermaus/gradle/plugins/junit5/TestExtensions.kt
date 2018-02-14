package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskContainer

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

/* Extensions */

fun Project.evaluate() {
  (this as ProjectInternal).evaluate()
}

@Suppress("UNCHECKED_CAST")
fun <T: Task> TaskContainer.get(name: String): T =
    this.getByName(name) as T
