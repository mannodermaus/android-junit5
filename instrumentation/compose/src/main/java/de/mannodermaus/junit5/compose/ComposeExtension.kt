package de.mannodermaus.junit5.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

public interface ComposeExtension {
    // TODO Add functionality based on ComposeTestRule (clock, idling, etc)
}

public interface ComposeContentExtension : ComposeExtension, SemanticsNodeInteractionsProvider {
    public fun setContent(block: @Composable () -> Unit)
}
