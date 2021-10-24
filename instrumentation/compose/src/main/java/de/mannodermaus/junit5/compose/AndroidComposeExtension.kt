package de.mannodermaus.junit5.compose

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 *
 */
public fun createComposeExtension(): ComposeExtension =
    createAndroidComposeExtension<ComponentActivity>()

/**
 *
 */
public inline fun <reified A : ComponentActivity> createAndroidComposeExtension(): AndroidComposeExtension {
    return createAndroidComposeExtension(A::class.java)
}

/**
 *
 */
public fun <A : ComponentActivity> createAndroidComposeExtension(
    activityClass: Class<A>
): AndroidComposeExtension {
    return AndroidComposeExtension(
        ruleFactory = { createAndroidComposeRule(activityClass) }
    )
}

/**
 *
 */
@SuppressLint("NewApi")
public class AndroidComposeExtension
@JvmOverloads
internal constructor(
    private val ruleFactory: () -> ComposeContentTestRule = { createComposeRule() }
) :
    BeforeEachCallback,
    ParameterResolver,
    ComposeExtension {

    private var description: Description? = null

    /* BeforeEachCallback */

    override fun beforeEach(context: ExtensionContext) {
        description = Description.createTestDescription(
            context.testClass.orElse(this::class.java),
            context.displayName
        )
    }

    /* ParameterResolver */

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        return ComposeExtension::class.java.isAssignableFrom(parameterContext.parameter.type)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return this
    }

    /* ComposeExtension */

    override fun runComposeTest(block: ComposeContext.() -> Unit) {
        ruleFactory().also { rule ->
            rule.apply(object : Statement() {
                override fun evaluate() {
                    rule.block()
                }
            }, description).evaluate()
        }
    }
}
