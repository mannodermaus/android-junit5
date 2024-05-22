@file:Suppress("DEPRECATION")

package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant

internal val BaseVariant.unitTestVariant: UnitTestVariant
    get() {
        if (this !is TestedVariant) {
            throw IllegalArgumentException("Argument is not TestedVariant: $this")
        }

        return requireNotNull(this.unitTestVariant)
    }
