package de.mannodermaus.gradle.plugins.junit5.internal.utils

import com.github.zafarkhaja.semver.Version
import org.gradle.api.GradleException
import org.gradle.util.GradleVersion

internal fun excludedPackagingOptions() = listOf(
        "/META-INF/LICENSE.md",
        "/META-INF/LICENSE-notice.md"
)

internal fun requireGradle(version: String, message: () -> String) {
    require(GradleVersion.current() >= GradleVersion.version(version)) {
        throw GradleException(message())
    }
}

internal fun requireVersion(actual: String, required: String, message: () -> String) {
    val actualVersion = Version.parse(actual)
    val requiredVersion = Version.parse(required)
    require(actualVersion.isHigherThanOrEquivalentTo(requiredVersion)) {
        throw GradleException(message())
    }
}
