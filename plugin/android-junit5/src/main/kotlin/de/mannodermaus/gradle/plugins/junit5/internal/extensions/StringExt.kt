package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import java.util.*

/** Replacement for Kotlin's deprecated [capitalize] method on strings */
internal fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) {
        it.titlecase(Locale.getDefault())
    } else {
        it.toString()
    }
}
