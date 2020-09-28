package de.mannodermaus.gradle.plugins.junit5.util.projects

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@Suppress("unused")
class SemanticVersionTests {

  companion object {
    @JvmStatic
    fun dataParseTests() = listOf(
        Arguments.of("5.0", 50000, Int.MAX_VALUE),
        Arguments.of("6.6.1-alpha1", 60601, 1),
        Arguments.of("6.6.1-beta1", 60601, 100),
        Arguments.of("6.6.1", 60601, Int.MAX_VALUE),
        Arguments.of("6.6-ALPHA-1", 60600, 1),
        Arguments.of("6.6-alpha1", 60600, 1),
        Arguments.of("6.6-alpha12", 60600, 12),
        Arguments.of("7.0.1-beta05", 70001, 500),
        Arguments.of("7.0.1-beta5", 70001, 500),
        Arguments.of("8.0.0-rc1", 80000, 10000),
        Arguments.of("8.0.0-rc-1", 80000, 10000),
        Arguments.of("11.4.3-rc-10", 110403, 100000),
        Arguments.of("99.15.67", 991567, Int.MAX_VALUE)
    )

    @JvmStatic
    fun dataCompareTo() = listOf(
        Arguments.of("4.4.0", "5.7.2", -1),
        Arguments.of("4.4.0", "4.5.0", -1),
        Arguments.of("4.4.2", "4.4.1", 1),
        Arguments.of("4.4.0-alpha-1", "4.4.0-alpha5", -1),
        Arguments.of("7.2-alpha-4", "7.2-beta-6", -1),
        Arguments.of("7.4-beta-1", "7.3-beta-2", 1),
        Arguments.of("8.0", "8.0.0-alpha-2", 1),
        Arguments.of("2.5", "2.5.0", 0),
        Arguments.of("4.2-rc-50", "4.2", -1),
        Arguments.of("4.2.1-rc-50", "4.2", 1)
    )

    @JvmStatic
    fun dataIncorrectInput() = listOf(
        Arguments.of("2", "unsupported number of components for version: %s"),
        Arguments.of("1.6.2.0", "unsupported number of components for version: %s"),
        Arguments.of("ONE DOT TWO", "unknown stable value for version: %s"),
        Arguments.of("ONE DOT TWO-alpha-1", "unknown stable value for version: %s"),
        Arguments.of("2.zErO.6", "unknown stable value for version: %s"),
        Arguments.of("1.5-preview-4", "unknown suffix category for version: %s"),
        Arguments.of("3.2.0-3", "unknown suffix category for version: %s"),
        Arguments.of("2.4-ALPHA", "unknown numerical suffix value for version: %s")
    )
  }

  @MethodSource("dataParseTests")
  @ParameterizedTest
  fun `parse version into stable and suffix parts correctly`(input: String, expectedStable: Int, expectedSuffix: Int) {
    val actual = SemanticVersion(input)

    assertThat(actual.stableValue).isEqualTo(expectedStable)
    assertThat(actual.suffixValue).isEqualTo(expectedSuffix)
  }

  @MethodSource("dataCompareTo")
  @ParameterizedTest
  fun `compares versions correctly`(version1: SemanticVersion, version2: SemanticVersion, expectedResult: Int) {
    val actualResult = version1.compareTo(version2)

    assertThat(actualResult).isEqualTo(expectedResult)
  }

  @MethodSource("dataIncorrectInput")
  @ParameterizedTest
  fun `throws on incorrect inputs`(version: String, expectedError: String) {
    val exception = assertThrows<IllegalArgumentException> {
      SemanticVersion(version)
    }

    assertThat(exception).hasMessageThat().isEqualTo(expectedError.format(version))
  }
}
