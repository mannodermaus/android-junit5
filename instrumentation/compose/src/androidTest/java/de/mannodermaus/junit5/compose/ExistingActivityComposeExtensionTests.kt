package de.mannodermaus.junit5.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.RegisterExtension

@TestInstance(PER_CLASS)
class ExistingActivityComposeExtensionTests {

    @JvmField
    @RegisterExtension
    @OptIn(ExperimentalTestApi::class)
    val extension = createAndroidComposeExtension<ExistingActivity>()

    @BeforeAll fun beforeAll() = extension.use { onNodeWithText("click").performClick() }

    @BeforeEach fun beforeEach() = extension.use { onNodeWithText("click").performClick() }

    @Test fun test() = extension.use { onNodeWithText("Clicked: 2").assertIsDisplayed() }
}
