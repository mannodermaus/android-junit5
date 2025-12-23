@file:SuppressLint("RestrictedApi")

package de.mannodermaus.junit5.internal.runners.notification

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.test.internal.runner.listener.InstrumentationResultPrinter
import de.mannodermaus.junit5.internal.LOG_TAG
import de.mannodermaus.junit5.internal.runners.notification.ParallelRunNotifier.EventThread.Event
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Wrapping implementation of JUnit 4's run notifier for parallel test execution
 * (i.e. when "junit.jupiter.execution.parallel.enabled" is active during the run).
 * It unpacks the singular 'instrumentation result printer' assigned by AndroidX
 * and reroutes its notification mechanism. This allows parallel tests to still execute in parallel,
 * but also allows their results to be reported back in the strictly sequential order required by the instrumentation.
 */
internal class ParallelRunNotifier(private val delegate: RunNotifier) : RunNotifier() {
    private companion object {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private val doneLock = Object()
        private val nopPrinter = InstrumentationResultPrinter()
        private val nopTestState = TestState("", Bundle(), 0)

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

    private data class TestState(
        val testClass: String,
        val testResult: Bundle,
        val testResultCode: Int,
    )

    private val states = mutableMapOf<String, TestState>()

    // Even though parallelism is the name of the game under the hood for this RunNotifier,
    // the nature of the Android Instrumentation is very much bound to synchronous execution internally.
    // Therefore, a single-threaded executor must be used to project the multithreaded notifications
    // from JUnit 5 onto this legacy thread model, resulting in some funky test reporting
    // but allowing the awesome performance benefits of parallel test execution!
    private lateinit var eventThread: EventThread
    private val executor = Executors.newSingleThreadExecutor()

    // Track finished tests, since the instrumentation reports failed tests through two calls
    // but we only need to forward the first call to the actual reporting
    private val finished = mutableSetOf<String>()

    // Original printer registered via Android instrumentation
    private val printer = reflection?.initialize(delegate) ?: nopPrinter

    override fun fireTestSuiteStarted(description: Description) {
        delegate.fireTestSuiteStarted(description)

        // Start asynchronous processing pipeline
        eventThread = EventThread(
            onProcessEvent = ::onProcessEvent,
            onDone = ::onDone,
        ).also(EventThread::start)
    }

    override fun fireTestStarted(description: Description) {
        eventThread.enqueue(Event.Started(description))
    }

    override fun fireTestIgnored(description: Description) {
        eventThread.enqueue(Event.Ignored(description))
    }

    override fun fireTestFailure(failure: Failure) {
        if (finished.add(failure.description.uniqueIdentifier)) {
            eventThread.enqueue(Event.Finished(failure.description, testFailure = failure))
        }
    }

    override fun fireTestAssumptionFailed(failure: Failure) {
        if (finished.add(failure.description.uniqueIdentifier)) {
            eventThread.enqueue(Event.Finished(failure.description, assumptionFailure = failure))
        }
    }

    override fun fireTestFinished(description: Description) {
        if (finished.add(description.uniqueIdentifier)) {
            eventThread.enqueue(Event.Finished(description))
        }
    }

    override fun fireTestSuiteFinished(description: Description) {
        synchronized(doneLock) {
            // Request stopping of the asynchronous processing pipeline
            eventThread.interruptPolitely(description)
            doneLock.wait()
        }
    }

    /* Private */

    private fun onProcessEvent(event: Event) = executor.submit {
        val description = event.description

        when (event) {
            is Event.Started -> {
                delegate.fireTestStarted(description)
                printer.testStarted(description)

                // Persist the current printer state for this test
                // (for later, when this test's finish event comes in)
                states[description.uniqueIdentifier] = printer.captureTestState()
            }

            is Event.Ignored -> {
                delegate.fireTestIgnored(description)
                printer.testIgnored(description)
            }

            is Event.Finished -> {
                // Restore the printer state to the current test case,
                // then fire the relevant lifecycle methods of the delegate notifier
                printer.restoreTestState(description)

                // For failed test cases, always invoke the failure methods first,
                // but invoke the 'finished' method pair for all cases
                when {
                    event.testFailure != null -> {
                        delegate.fireTestFailure(event.testFailure)
                        printer.testFailure(event.testFailure)
                        delegate.fireTestFinished(description)
                        printer.testFinished(description)
                    }

                    event.assumptionFailure != null -> {
                        delegate.fireTestAssumptionFailed(event.assumptionFailure)
                        printer.testAssumptionFailure(event.assumptionFailure)
                        delegate.fireTestFinished(description)
                        printer.testFinished(description)
                    }

                    else -> {
                        delegate.fireTestFinished(description)
                        printer.testFinished(description)
                    }
                }
            }
        }
    }

    private fun onDone(description: Description?) {
        synchronized(doneLock) {
            // Consume any pending asynchronous tasks
            executor.shutdown()
            executor.awaitTermination(15, TimeUnit.SECONDS)

            if (description != null) {
                delegate.fireTestSuiteFinished(description)
                printer.testSuiteFinished(description)
            }

            // Unlocks the blockage from fireTestSuiteFinished(),
            // allowing the test engine to properly finish this class
            finished.clear()
            doneLock.notifyAll()
        }
    }

    private fun InstrumentationResultPrinter.captureTestState(): TestState {
        return reflection?.captureTestState(this) ?: nopTestState
    }

    private fun InstrumentationResultPrinter.restoreTestState(description: Description) {
        val id = description.uniqueIdentifier
        val state = requireNotNull(states[id])
        reflection?.restoreTestState(this, state)
        states.remove(id)
    }

    private val Description.uniqueIdentifier
        get() =
            "$className-$displayName"

    private class EventThread(
        private val onProcessEvent: (Event) -> Unit,
        private val onDone: (Description?) -> Unit,
    ) : Thread("ParallelRunNotifier.EventThread") {
        sealed interface Event {
            val description: Description

            data class Started(override val description: Description) : Event
            data class Finished(
                override val description: Description,
                val testFailure: Failure? = null,
                val assumptionFailure: Failure? = null,
            ) : Event

            data class Ignored(override val description: Description) : Event
        }

        private val startQueue = LinkedBlockingQueue<Event>()
        private val ignoreQueue = mutableListOf<Event>()
        private val finishQueue = LinkedBlockingDeque<Event>()
        private var interruptionDescription: Description? = null

        fun enqueue(event: Event) {
            when (event) {
                is Event.Started -> startQueue.offer(event)
                is Event.Ignored -> ignoreQueue.add(event)
                is Event.Finished -> finishQueue.offerFirst(event)
            }
        }

        fun interruptPolitely(description: Description) {
            interruptionDescription = description
            interrupt()
        }

        private fun sendEvent(event: Event) {
            onProcessEvent(event)
        }

        private fun sendDone() {
            onDone(interruptionDescription)
        }

        override fun run() {
            try {
                while (true) {
                    // Accept the first incoming 'started' event
                    val startEvent = startQueue.take()
                    sendEvent(startEvent)

                    // Now wait until a suitable 'finished' event comes in
                    var finishEvent = finishQueue.take()
                    while (finishEvent.description != startEvent.description) {
                        finishQueue.offer(finishEvent)
                        finishEvent = finishQueue.take()
                    }

                    // If this point is reached, both event references point to the same test case.
                    // Allow the finish event to be processed, too
                    sendEvent(finishEvent)

                    // Take care of any new ignore events at this point
                    ignoreQueue.forEach(::sendEvent)
                    ignoreQueue.clear()
                }
            } catch (_: InterruptedException) {
                // OK
                while (startQueue.isNotEmpty()) {
                    val startEvent = startQueue.take()
                    sendEvent(startEvent)

                    if (finishQueue.isNotEmpty()) {
                        finishQueue
                            .firstOrNull { it.description == startEvent.description }
                            ?.let { finishEvent ->
                                finishQueue.remove(finishEvent)
                                sendEvent(finishEvent)
                            }
                    }

                    ignoreQueue.forEach(::sendEvent)
                    ignoreQueue.clear()
                }

                sendDone()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class Reflection {
        private fun <T : Any> Class<T>.field(name: String) = this.getDeclaredField(name).also { it.isAccessible = true }

        private val synchronizedRunListenerClass = Class.forName("org.junit.runner.notification.SynchronizedRunListener")
        private val synchronizedListenerDelegateField = synchronizedRunListenerClass.field("listener")
        private val runNotifierListenersField = RunNotifier::class.java.field("listeners")
        private val resultPrinterTestResultField = InstrumentationResultPrinter::class.java.field("testResult")
        private val resultPrinterTestResultCodeField = InstrumentationResultPrinter::class.java.field("testResultCode")
        private val resultPrinterTestClassField = InstrumentationResultPrinter::class.java.field("testClass")

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

        fun captureTestState(printer: InstrumentationResultPrinter): TestState {
            return TestState(
                testClass = resultPrinterTestClassField.get(printer) as String,
                testResult = resultPrinterTestResultField.get(printer) as Bundle,
                testResultCode = resultPrinterTestResultCodeField.get(printer) as Int,
            )
        }

        fun restoreTestState(printer: InstrumentationResultPrinter, state: TestState) {
            resultPrinterTestClassField.set(printer, state.testClass)
            resultPrinterTestResultField.set(printer, state.testResult)
            resultPrinterTestResultCodeField.set(printer, state.testResultCode)
        }
    }
}

private val Description.isNotJUnit5: Boolean
    get() = getAnnotation(org.junit.jupiter.api.Test::class.java) == null
