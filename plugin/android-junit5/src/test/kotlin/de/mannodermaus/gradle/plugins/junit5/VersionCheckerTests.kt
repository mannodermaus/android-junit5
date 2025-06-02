package de.mannodermaus.gradle.plugins.junit5

import com.android.build.api.AndroidPluginVersion
import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_AGP_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireAgp
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Created by Marcel Schnelle on 2018/06/19.
 *
 * Sanity checks for the external Semver library, used to determine compatibility of the AGP.
 */
class VersionCheckerTests {

    @CsvSource(
        "7.0.0-alpha1, false",
        "7.0.0-alpha1, false",
        "7.0.0-beta1, false",
        "7.0.0-rc1, false",
        "7.0.0, false",
        "8.0.0-beta1, false",
        "8.0.0, false",
        "8.0.1, false",
        "8.0.1-alpha1, false",
        "8.1.0, false",
        "8.1.0-beta1, false",
        "8.2.0, true",
        "8.3.0-rc1, true",
        "8.4.0-alpha5, true",
        "8.10.1-alpha11, true",
        "8.10.0, true",
        "8.11.0-beta1, true",
        "8.11.0, true",
    )
    @ParameterizedTest
    fun `check AGP compatibility`(version: String, compatible: Boolean) {
        val pluginVersion = version.toAndroidPluginVersion()
        assertThat(versionCompatible(pluginVersion)).isEqualTo(compatible)
    }

    private fun versionCompatible(version: AndroidPluginVersion): Boolean {
        return try {
            requireAgp(
                actual = version,
                required = MIN_REQUIRED_AGP_VERSION,
                message = { "" }
            )
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun String.toAndroidPluginVersion(): AndroidPluginVersion {
        // Split into stable and optional preview parts
        val firstSplit = split('-')

        // Split first part further into major, minor, patch
        val stableComponents = firstSplit[0].split('.')

        var version = AndroidPluginVersion(
            major = stableComponents[0].toInt(),
            minor = stableComponents[1].toInt(),
            micro = stableComponents.getOrNull(2)?.toInt() ?: 0
        )

        // Attach preview part
        val preview = firstSplit.getOrNull(1)

        version = when {
            preview == null -> version
            preview.startsWith("alpha") -> version.alpha(preview.substringAfter("alpha").toInt())
            preview.startsWith("beta") -> version.beta(preview.substringAfter("beta").toInt())
            preview.startsWith("rc") -> version.rc(preview.substringAfter("rc").toInt())
            else -> version
        }

        // Validate correctness
        assertThat(version.toString()).endsWith(this)

        return version
    }
}
