package de.mannodermaus.gradle.plugins.junit5

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.config.MIN_REQUIRED_AGP_VERSION
import de.mannodermaus.gradle.plugins.junit5.internal.utils.requireVersion
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Created by Marcel Schnelle on 2018/06/19.
 *
 * Sanity checks for the external Semver library, used to determine compatibility of the AGP.
 */
class VersionCheckerTests {

    @CsvSource(
            "4.0.0-alpha01, false",
            "7.0.0-alpha01, false",
            "7.0.0-beta01, false",
            "7.0.0-rc01, false",
            "7.0.0, true",
            "7.0.1, true",
            "7.0.1-alpha01, true",
            "7.1.0, true",
            "7.1.0-beta01, true",
            "7.2.0, true",
            "7.3.0-rc01, true",
    )
    @ParameterizedTest
    fun `check AGP compatibility`(version: String, compatible: Boolean) {
        assertThat(versionCompatible(version)).isEqualTo(compatible)
    }

    private fun versionCompatible(version: String): Boolean {
        return try {
            requireVersion(
                    actual = version,
                    required = MIN_REQUIRED_AGP_VERSION,
                    message = { "" })
            true
        } catch (error: Throwable) {
            false
        }
    }
}
