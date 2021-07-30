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
            "2.3.3, false",
            "3.0.0, false",
            "3.1.3, false",
            "3.2.0-alpha01, false",
            "3.2.0-alpha14, false",
            "3.3.0, false",
            "3.4.0, false",
            "3.5.0-alpha13, false",
            "3.3.0, false",
            "4.0.0-alpha01, false",
            "4.0.0, true",
            "4.0.1, true",
            "4.1.0, true",
            "7.0.0, true",
            "7.1.0, true",
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
