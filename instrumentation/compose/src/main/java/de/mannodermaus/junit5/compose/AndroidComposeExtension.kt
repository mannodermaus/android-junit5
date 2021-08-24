package de.mannodermaus.junit5.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import de.mannodermaus.junit5.compose.internal.ComposeRuleAdapter
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 *
 */
public fun createComposeExtension(): ComposeContentExtension =
    createAndroidComposeExtension<ComponentActivity>()

/**
 *
 */
public inline fun <reified A : ComponentActivity> createAndroidComposeExtension(): AndroidComposeExtension {
    return createAndroidComposeExtension(A::class.java)
}

public fun <A : ComponentActivity> createAndroidComposeExtension(
    activityClass: Class<A>
): AndroidComposeExtension {
    return AndroidComposeExtension(
        rule = createAndroidComposeRule(activityClass)
    )
}

/**
 *
 */
public class AndroidComposeExtension
internal constructor(rule: ComposeContentTestRule) :
    BeforeEachCallback,
    AfterEachCallback,
    ComposeContentExtension {

    private val adapter = ComposeRuleAdapter(rule)

    override fun beforeEach(context: ExtensionContext?) {
        adapter.setup()
    }

    override fun afterEach(context: ExtensionContext?) {
        adapter.teardown()
    }

    public override fun setContent(block: @Composable () -> Unit) {
        adapter.rule.setContent(block)
    }

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection {
        return adapter.rule.onAllNodes(matcher, useUnmergedTree)
    }

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction {
        return adapter.rule.onNode(matcher, useUnmergedTree)
    }
}
