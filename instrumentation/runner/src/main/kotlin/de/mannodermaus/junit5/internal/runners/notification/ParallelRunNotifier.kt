package de.mannodermaus.junit5.internal.runners.notification

import android.util.Log
import androidx.test.internal.runner.listener.InstrumentationResultPrinter
import de.mannodermaus.junit5.internal.LOG_TAG
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

/**
 * Wrapping implementation of JUnit 4's run notifier for parallel test execution
 * (i.e. when "junit.jupiter.execution.parallel.enabled" is active during the run).
 * It unpacks the singular 'instrumentation result printer' assigned by Android
 * into using one instance per test, preventing its mutable internals from being
 * modified by concurrent threads at the same time.
 */
internal class ParallelRunNotifier(private val delegate: RunNotifier) : RunNotifier() {
    companion object {
        // Reflective access is available via companion object
        // to allow for shared storage of data across notifiers
        private val reflection by lazy {
            try {
                Reflection()
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "FATAL: Cannot initialize reflective access", e)
                null
            }
        }
    }

    private val states = mutableMapOf<String, InstrumentationResultPrinter?>()

    // Original printer registered via Android instrumentation
    private val printer = reflection?.initialize(delegate)

    override fun fireTestSuiteStarted(description: Description) {
        delegate.fireTestSuiteStarted(description)
    }

    override fun fireTestRunStarted(description: Description) {
        delegate.fireTestRunStarted(description)
    }

    override fun fireTestStarted(description: Description) {
        synchronized(this) {
            delegate.fireTestStarted(description)

            // Notify original printer immediately,
            // then freeze its state for the current method for later
            printer?.testStarted(description)
            states[description] = reflection?.copy(printer)
        }
    }

    override fun fireTestIgnored(description: Description) {
        synchronized(this) {
            delegate.fireTestIgnored(description)

            printer?.testIgnored(description)
        }
    }

    override fun fireTestFailure(failure: Failure) {
        delegate.fireTestFailure(failure)

        states[failure.description]?.testFailure(failure)
    }

    override fun fireTestAssumptionFailed(failure: Failure) {
        delegate.fireTestAssumptionFailed(failure)

        states[failure.description]?.testAssumptionFailure(failure)
    }

    override fun fireTestFinished(description: Description) {
        synchronized(this) {
            delegate.fireTestFinished(description)

            states[description]?.testFinished(description)
            states.remove(description)
        }
    }

    override fun fireTestRunFinished(result: Result) {
        delegate.fireTestRunFinished(result)
    }

    override fun fireTestSuiteFinished(description: Description) {
        delegate.fireTestSuiteFinished(description)
    }

    /* Private */

    private operator fun <T> Map<String, T>.get(key: Description): T? {
        return get(key.displayName)
    }

    private operator fun <T> MutableMap<String, T>.set(key: Description, value: T) {
        put(key.displayName, value)
    }

    private fun <T> MutableMap<String, T>.remove(key: Description) {
        remove(key.displayName)
    }

    @Suppress("UNCHECKED_CAST")
    private class Reflection {
        private val synchronizedRunListenerClass =
            Class.forName("org.junit.runner.notification.SynchronizedRunListener")
        private val synchronizedListenerDelegateField = synchronizedRunListenerClass
            .getDeclaredField("listener").also { it.isAccessible = true }
        private val runNotifierListenersField = RunNotifier::class.java
            .getDeclaredField("listeners").also { it.isAccessible = true }

        private var cached: InstrumentationResultPrinter? = null

        fun initialize(notifier: RunNotifier): InstrumentationResultPrinter? {
            try {
                // The printer needs to be retrieved only once per test run
                cached?.let { return it }

                // The Android system registers a global listener
                // for communicating status events back to the instrumentation.
                // In parallel mode, this communication must be piped through
                // a custom piece of logic in order to not lose any mutable data
                // from concurrent method invocations
                val listeners = runNotifierListenersField.get(notifier) as? List<RunListener>

                // The Android instrumentation may wrap the printer inside another JUnit listener,
                // so make sure to search for the result inside its toString() representation
                // (rather than through an 'it is X' check)
                val candidate = listeners?.firstOrNull {
                    InstrumentationResultPrinter::class.java.name in it.toString()
                }

                if (candidate != null) {
                    // Replace the original listener with a wrapped version of itself,
                    // which will allow all non-JUnit 5 tests through the normal pipeline
                    // (tests that actually _are_ JUnit 5 will be handled differently)
                    notifier.removeListener(candidate)
                    notifier.addListener(FilteredRunListener(candidate, Description::isNotJUnit5))
                }

                // The Android instrumentation may wrap the printer inside another JUnit listener,
                // so make sure to search for the result inside its toString() representation
                // (rather than through an 'it is X' check)
                val result = if (synchronizedRunListenerClass.isInstance(candidate)) {
                    synchronizedListenerDelegateField.get(candidate) as? InstrumentationResultPrinter
                } else {
                    candidate as? InstrumentationResultPrinter
                }

                cached = result
                return result
            } catch (e: Throwable) {
                e.printStackTrace()
                return null
            }
        }

        fun copy(original: InstrumentationResultPrinter?): InstrumentationResultPrinter? = try {
            if (original != null) {
                InstrumentationResultPrinter().also { copy ->
                    copy.instrumentation = original.instrumentation

                    InstrumentationResultPrinter::class.java.declaredFields.forEach { field ->
                        field.isAccessible = true
                        field.set(copy, field.get(original))
                    }
                }
            } else {
                null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}

private val Description.isNotJUnit5: Boolean
    get() = getAnnotation(org.junit.jupiter.api.Test::class.java) == null
