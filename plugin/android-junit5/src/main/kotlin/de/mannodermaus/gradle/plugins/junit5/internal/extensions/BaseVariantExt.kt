package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant

internal val BaseVariant.unitTestVariant: UnitTestVariant
    get() {
        if (this !is TestedVariant) {
            throw IllegalArgumentException("Argument is not TestedVariant: $this")
        }

        return requireNotNull(this.unitTestVariant)
    }

internal val BaseVariant.instrumentationTestVariant: TestVariant?
    get() {
        if (this !is TestedVariant) {
            throw IllegalArgumentException("Argument is not TestedVariant: $this")
        }

        return this.testVariant
    }

internal fun BaseVariant.getTaskName(prefix: String = "", suffix: String = ""): String {
    // At least one value must be provided
    require(prefix.isNotEmpty() || suffix.isNotEmpty())

    return StringBuilder().apply {
        append(prefix)
        append(if (isEmpty()) {
            name
        } else {
            name.capitalize()
        })
        append(suffix.capitalize())
    }.toString()
}
