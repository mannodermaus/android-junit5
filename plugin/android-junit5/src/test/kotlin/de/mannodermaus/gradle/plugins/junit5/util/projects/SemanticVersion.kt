package de.mannodermaus.gradle.plugins.junit5.util.projects

import java.util.Locale

private val NUMERICAL_REGEX = Regex("(\\d+)")

/**
 * Wrapper for a semantic version string (e.g. "6.0" or "7.1.5-alpha-12"), interpreting that string
 * as a numerical value eligible for comparisons against other objects.
 */
class SemanticVersion(version: String) : Comparable<SemanticVersion> {
    val stableValue: Int = version.extractStableValue()
    val suffixValue: Int = version.extractSuffixValue()

    override fun compareTo(other: SemanticVersion): Int {
        val result = this.stableValue.compareTo(other.stableValue)
        return if (result == 0) {
            this.suffixValue.compareTo(other.suffixValue)
        } else {
            result
        }
    }
}

/* Private */

private fun String.extractStableValue(): Int {
    val stripped = this.substringBefore('-')
    val split =
        stripped.split('.').map {
            it.toIntOrNull()
                ?: throw IllegalArgumentException("unknown stable value for version: $this")
        }

    require(split.size in 2..3) { "unsupported number of components for version: $this" }

    // Add up the components, with the patch component being optional
    return split[0] * 10000 + split[1] * 100 + if (split.size > 2) split[2] else 0
}

private fun String.extractSuffixValue(): Int {
    if ('-' in this) {
        val suffix = this.substringAfter('-').lowercase(Locale.ROOT)

        // Find known suffix types
        val suffixMultiplier =
            when {
                "alpha" in suffix -> 1
                "beta" in suffix -> 100
                "rc" in suffix -> 10000
                else -> throw IllegalArgumentException("unknown suffix category for version: $this")
            }

        // Find numerical value of suffix
        val numericalSuffix =
            NUMERICAL_REGEX.find(suffix)?.groupValues?.lastOrNull()?.toIntOrNull()
                ?: throw IllegalArgumentException(
                    "unknown numerical suffix value for version: $this"
                )

        return suffixMultiplier * numericalSuffix
    } else {
        // Not a preview version - use highest possible suffix value to be "newer" than any preview
        // could be
        return Int.MAX_VALUE
    }
}
