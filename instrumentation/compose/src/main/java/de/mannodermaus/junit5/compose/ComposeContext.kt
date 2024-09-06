package de.mannodermaus.junit5.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.test.waitUntilNodeCount
import androidx.compose.ui.unit.Density

/**
 * A context through which composable blocks can be orchestrated within a [ComposeExtension].
 */
public sealed interface ComposeContext : SemanticsNodeInteractionsProvider {
    // Internal note: The below method list is a copy of `ComposeContentTestRule`,
    // preventing the viral spread of its ExperimentalTestApi annotation
    // into the consumer's codebase and separating it from JUnit 4's TestRule
    public val density: Density
    public val mainClock: MainTestClock
    public fun <T> runOnUiThread(action: () -> T): T
    public fun <T> runOnIdle(action: () -> T): T
    public fun waitForIdle()
    public suspend fun awaitIdle()
    public fun waitUntil(timeoutMillis: Long = 1_000L, condition: () -> Boolean)
    public fun waitUntil(
        conditionDescription: String,
        timeoutMillis: Long = 1_000L,
        condition: () -> Boolean
    )

    public fun waitUntilNodeCount(
        matcher: SemanticsMatcher,
        count: Int,
        timeoutMillis: Long = 1_000L
    )

    public fun waitUntilAtLeastOneExists(matcher: SemanticsMatcher, timeoutMillis: Long = 1_000L)
    public fun waitUntilExactlyOneExists(matcher: SemanticsMatcher, timeoutMillis: Long = 1_000L)
    public fun waitUntilDoesNotExist(matcher: SemanticsMatcher, timeoutMillis: Long = 1_000L)
    public fun registerIdlingResource(idlingResource: IdlingResource)
    public fun unregisterIdlingResource(idlingResource: IdlingResource)
    public fun setContent(composable: @Composable () -> Unit)
}

@OptIn(ExperimentalTestApi::class)
internal class ComposeContextImpl(
    private val delegate: ComposeUiTest
) : ComposeContext, SemanticsNodeInteractionsProvider by delegate {

    override val density: Density get() = delegate.density

    override val mainClock: MainTestClock get() = delegate.mainClock

    override fun <T> runOnUiThread(action: () -> T): T = delegate.runOnUiThread(action)

    override fun <T> runOnIdle(action: () -> T): T = delegate.runOnIdle(action)

    override fun waitForIdle() = delegate.waitForIdle()

    override suspend fun awaitIdle() = delegate.awaitIdle()

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) =
        delegate.waitUntil(conditionDescription = null, timeoutMillis, condition)

    override fun waitUntil(
        conditionDescription: String,
        timeoutMillis: Long,
        condition: () -> Boolean
    ) {
        delegate.waitUntil(conditionDescription, timeoutMillis, condition)
    }

    override fun waitUntilNodeCount(matcher: SemanticsMatcher, count: Int, timeoutMillis: Long) =
        delegate.waitUntilNodeCount(matcher, count, timeoutMillis)

    override fun waitUntilAtLeastOneExists(matcher: SemanticsMatcher, timeoutMillis: Long) =
        delegate.waitUntilAtLeastOneExists(matcher, timeoutMillis)

    override fun waitUntilExactlyOneExists(matcher: SemanticsMatcher, timeoutMillis: Long) =
        delegate.waitUntilExactlyOneExists(matcher, timeoutMillis)

    override fun waitUntilDoesNotExist(matcher: SemanticsMatcher, timeoutMillis: Long) =
        delegate.waitUntilDoesNotExist(matcher, timeoutMillis)

    override fun registerIdlingResource(idlingResource: IdlingResource) =
        delegate.registerIdlingResource(idlingResource)

    override fun unregisterIdlingResource(idlingResource: IdlingResource) =
        delegate.unregisterIdlingResource(idlingResource)

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction = delegate.onNode(matcher, useUnmergedTree)

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection = delegate.onAllNodes(matcher, useUnmergedTree)

    override fun setContent(composable: @Composable () -> Unit) = delegate.setContent(composable)
}
