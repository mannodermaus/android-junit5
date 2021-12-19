package de.mannodermaus.junit5.compose

import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * A context through which composable blocks can be orchestrated within a [ComposeExtension].
 */
public typealias ComposeContext = ComposeContentTestRule
