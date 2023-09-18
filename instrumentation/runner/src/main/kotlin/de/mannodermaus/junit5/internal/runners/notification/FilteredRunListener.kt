package de.mannodermaus.junit5.internal.runners.notification

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/**
 * A wrapper implementation around JUnit's [RunListener] class
 * which only works selectively. In other words, this implementation only delegates
 * to its parameter for test descriptors that pass the given [filter].
 */
internal class FilteredRunListener(
    private val delegate: RunListener,
    private val filter: (Description) -> Boolean,
) : RunListener() {
    override fun testStarted(description: Description) {
        if (filter(description)) {
            delegate.testStarted(description)
        }
    }

    override fun testIgnored(description: Description) {
        if (filter(description)) {
            delegate.testIgnored(description)
        }
    }

    override fun testFailure(failure: Failure) {
        if (filter(failure.description)) {
            delegate.testFailure(failure)
        }
    }

    override fun testAssumptionFailure(failure: Failure) {
        if (filter(failure.description)) {
            delegate.testAssumptionFailure(failure)
        }
    }

    override fun testFinished(description: Description) {
        if (filter(description)) {
            delegate.testFinished(description)
        }
    }
}
