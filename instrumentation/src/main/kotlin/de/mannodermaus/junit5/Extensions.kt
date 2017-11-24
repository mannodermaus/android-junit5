package de.mannodermaus.junit5

import java.util.Optional

fun <T> Optional<T>.or(other: Optional<T>) =
    if (this.isPresent) {
      this
    } else {
      other
    }
