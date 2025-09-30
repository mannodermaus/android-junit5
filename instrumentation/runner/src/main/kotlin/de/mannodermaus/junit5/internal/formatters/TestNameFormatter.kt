package de.mannodermaus.junit5.internal.formatters

import org.junit.platform.launcher.TestIdentifier

/**
 * A class for naming Jupiter test methods in a compatible manner,
 * taking into account several limitations imposed by the
 * Android instrumentation (e.g. on isolated test runs).
 */
internal object TestNameFormatter {
    fun format(identifier: TestIdentifier, legacyFormat: Boolean = false): String {
        // When requesting the legacy format of the formatter,
        // construct a technical version of its name for backwards compatibility
        // with the JUnit 4-based instrumentation of Android by stripping the brackets of parameterized tests completely.
        // If this didn't happen, running them from the IDE will cause "No tests found" errors.
        // See AndroidX's TestRequestBuilder$MethodFilter for where this is cross-referenced in the instrumentation!
        //
        // History:
        // - #199 & #207 (the original unearthing of this behavior)
        // - #317 (making an exception for dynamic tests)
        // - #339 (retain indices of parameterized methods to avoid premature filtering by JUnit 4's test discovery)
        if (legacyFormat) {
            val reportName = identifier.legacyReportingName
            val paramStartIndex = reportName.indexOf('(')
            if (paramStartIndex > -1) {
                val result = reportName.substring(0, paramStartIndex)

                val paramEndIndex = reportName.lastIndexOf('[')

                return if (paramEndIndex > -1) {
                    // Retain suffix of parameterized methods (i.e. "[1]", "[2]" etc)
                    // so that they won't be filtered out by JUnit 4 on isolated method runs
                    result + reportName.substring(paramEndIndex)
                } else {
                    result
                }
            }
        }

        // Process the display name before handing it out,
        // maintaining compatibility with the expectations of Android's instrumentation:
        // - Cut off no-parameter brackets '()'
        // - Replace any other round brackets with square brackets (for parameterized tests)
        //   to ensure that logs are displayed in the test results window (ref. #350)
        // - Remove quotation marks (for parameterized tests)
        return identifier.displayName
            .replace("()", "")
            .replace('(', '[')
            .replace(')', ']')
            .replace("\"", "")
    }
}
