package de.mannodermaus.junit5

import java.lang.reflect.Executable
import java.util.Optional

fun <T> Optional<T>.or(other: Optional<T>) =
    if (this.isPresent) {
      this
    } else {
      other
    }

/* Collections */

fun <T : Annotation> Executable.isAnyAnnotationPresent(vararg annotationTypes: Class<T>): Boolean {
  annotationTypes.forEach {
    if (isAnnotationPresent(it)) {
      return true
    }
  }

  return false
}
