package de.mannodermaus.junit5.internal.extensions

import de.mannodermaus.junit5.internal.formatters.TestNameFormatter
import org.junit.platform.launcher.TestIdentifier

private val DYNAMIC_TEST_PREFIXES = listOf(
    "[test-template-invocation",
    "[dynamic-test",
    "[dynamic-container",
    "[test-factory",
    "[test-template"
)

private val TestIdentifier.shortId: String
    get() {
        var id = this.uniqueId
        val lastSlashIndex = id.lastIndexOf('/')
        if (lastSlashIndex > -1 && id.length >= lastSlashIndex) {
            id = id.substring(lastSlashIndex + 1)
        }
        return id
    }

/**
 * Check if the given TestIdentifier describes a "test template invocation",
 * i.e. a dynamic test generated at runtime.
 */
internal val TestIdentifier.isDynamicTest: Boolean
    get() {
        val shortId = this.shortId
        return DYNAMIC_TEST_PREFIXES.any { shortId.startsWith(it) }
    }

/**
 * Returns a formatted version of this identifier's name,
 * which is compatible with the quirks and limitations
 * of the Android Instrumentation, esp. when the [isIsolatedMethodRun]
 * flag is enabled.
 */
internal fun TestIdentifier.format(isIsolatedMethodRun: Boolean = false): String =
    TestNameFormatter.format(this, isIsolatedMethodRun)
