package de.mannodermaus.junit5.compose

import androidx.compose.runtime.Composable
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension

/**
 * A JUnit 5 [Extension] that allows you to test and control [Composable]s and application using Compose.
 * The functionality of testing Compose is provided by means of the [runComposeTest] method,
 * which receives a [ComposeContext] from which the test can be orchestrated. The test will block
 * until the app or composable is idle, to ensure the tests are deterministic.
 *
 * This extension can be added to any JUnit 5 class using the [ExtendWith] annotation,
 * or registered globally through a configuration file. Alternatively, you can instantiate the extension
 * in a field within the test class using any of the [createComposeExtension] or
 * [createAndroidComposeExtension] factory methods.
 */
public interface ComposeExtension : Extension {
    /**
     * Set up and drive the execution of a Compose test within the provided [block].
     * Depending on the time this is called, it will either queue up a preparatory action for the test
     * (e.g. in @BeforeEach)
     * The receive of this block is a [ComposeContext], through which you can access all sorts of
     * utilities to drive the execution of the test, such as driving the clock or executing actions
     * on the UI thread. The main purpose is provided through [ComposeContext.setContent], however:
     * With this function, you can pass an arbitrary composable tree to the extension and evaluate it afterwards.
     */
    public fun use(block: ComposeContext.() -> Unit)

    @Deprecated(message = "Change to use()", replaceWith = ReplaceWith("use(block)"))
    public fun runComposeTest(block: ComposeContext.() -> Unit) {
        use(block)
    }
}
