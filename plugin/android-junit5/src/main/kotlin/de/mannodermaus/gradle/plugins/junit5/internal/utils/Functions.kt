package de.mannodermaus.gradle.plugins.junit5.internal.utils

import com.android.build.api.AndroidPluginVersion
import org.gradle.api.GradleException
import org.gradle.util.GradleVersion

internal fun excludedPackagingOptions() = listOf(
        "/META-INF/LICENSE.md",
        "/META-INF/LICENSE-notice.md"
)

internal fun requireGradle(actual: GradleVersion, required: GradleVersion, message: () -> String) {
    require(actual >= required) {
        throw GradleException(message())
    }
}

internal fun requireAgp(actual: AndroidPluginVersion, required: AndroidPluginVersion, message: () -> String) {
    require(actual >= required) {
        throw GradleException(message())
    }
}
