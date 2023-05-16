package de.mannodermaus.junit5.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario

/**
 * A context through which composable blocks can be orchestrated within a [ComposeExtension].
 */
public sealed interface ComposeContext : SemanticsNodeInteractionsProvider {
    // Internal note: The below method list is a copy of `ComposeUiTest`,
    // preventing the viral spread of its ExperimentalTestApi annotation
    // into the consumer's codebase
    public val density: Density
    public val mainClock: MainTestClock
    public fun <T> runOnUiThread(action: () -> T): T
    public fun <T> runOnIdle(action: () -> T): T
    public fun waitForIdle()
    public suspend fun awaitIdle()
    public fun waitUntil(timeoutMillis: Long, condition: () -> Boolean)
    public fun registerIdlingResource(idlingResource: IdlingResource)
    public fun unregisterIdlingResource(idlingResource: IdlingResource)
    public fun setContent(composable: @Composable () -> Unit)
}

@OptIn(ExperimentalTestApi::class)
internal class ComposeContextImpl(
    private val delegate: ComposeUiTest
) : ComposeContext, SemanticsNodeInteractionsProvider by delegate {

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection {
        return delegate.onAllNodes(matcher, useUnmergedTree)
    }

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction {
        return delegate.onNode(matcher, useUnmergedTree)
    }

    override val density: Density get() = delegate.density
    override val mainClock: MainTestClock get() = delegate.mainClock

    override fun <T> runOnUiThread(action: () -> T): T {
        return delegate.runOnUiThread(action)
    }

    override fun <T> runOnIdle(action: () -> T): T {
        return delegate.runOnIdle(action)
    }

    override fun waitForIdle() {
        delegate.waitForIdle()
    }

    override suspend fun awaitIdle() {
        delegate.awaitIdle()
    }

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
        delegate.waitUntil(timeoutMillis, condition)
    }

    override fun registerIdlingResource(idlingResource: IdlingResource) {
        delegate.registerIdlingResource(idlingResource)
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) {
        delegate.unregisterIdlingResource(idlingResource)
    }

    override fun setContent(composable: @Composable () -> Unit) {
        delegate.setContent(composable)
    }
}
