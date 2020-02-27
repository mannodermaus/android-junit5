package de.mannodermaus.gradle.plugins.junit5

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.requireVersion
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * Created by Marcel Schnelle on 2018/06/19.
 *
 * Sanity checks for the external Semver library, used to determine compatibility of the AGP.
 */
class VersionCheckerSpec : Spek({

  listOf(
      "2.3.3" to false,
      "3.0.0" to false,
      "3.1.3" to false,
      "3.2.0-alpha01" to false,
      "3.2.0-alpha14" to false,
      MIN_REQUIRED_AGP_VERSION to true,
      "3.3.0" to false,
      "3.4.0" to false,
      "3.5.0-alpha13" to false,
      "3.3.0" to false,
      "4.0.0-alpha01" to true
  ).forEach { (actual, expected) ->
    on("checking against AGP $actual") {
      val testIsSuccessful = if (expected) "compatible" else "not compatible"
      it("determines correctly that this version is $testIsSuccessful") {
        assertThat(versionCompatible(actual)).isEqualTo(expected)
      }
    }
  }
})

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
