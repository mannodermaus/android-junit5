package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.api.variant.Variant

internal fun Variant.getTaskName(prefix: String = "", suffix: String = ""): String {
    // At least one value must be provided
    require(prefix.isNotEmpty() || suffix.isNotEmpty())

    return StringBuilder().apply {
        append(prefix)
        append(
            if (isEmpty()) {
                name
            } else {
                name.capitalized()
            }
        )
        append(suffix.capitalized())
    }.toString()
}
