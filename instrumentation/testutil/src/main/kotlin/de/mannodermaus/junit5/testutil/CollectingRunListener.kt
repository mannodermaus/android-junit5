package de.mannodermaus.junit5.testutil

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/** A JUnit 4 [RunListener] that collects information about executed and failed tests. */
public class CollectingRunListener : RunListener() {
    public data class Results(
        val successfulTests: List<Description>,
        val failedTests: List<Failure>,
        val ignoredTests: List<Description>,
    )

    private val success = mutableListOf<Description>()
    private val failures = mutableListOf<Failure>()
    private val ignored = mutableListOf<Description>()

    override fun testFinished(description: Description) {
        success += description
    }

    override fun testFailure(failure: Failure) {
        failures += failure
    }

    override fun testIgnored(description: Description) {
        ignored += description
    }

    public fun getResults(): Results = Results(success, failures, ignored)
}
