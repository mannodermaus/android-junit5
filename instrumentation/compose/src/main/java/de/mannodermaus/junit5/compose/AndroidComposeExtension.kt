package de.mannodermaus.junit5.compose

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API
 * for field injection. Prefer this over [createAndroidComposeExtension] if you don't care
 * about the specific activity class to use under the hood.
 *
 * ```
 * class MyTests {
 *   @JvmField
 *   @RegisterExtension
 *   val extension = createComposeExtension()
 * }
 * ```
 */
public fun createComposeExtension(): ComposeExtension =
    createAndroidComposeExtension<ComponentActivity>()

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API
 * for field injection. Prefer this over [createComposeExtension] if your tests require a custom Activity.
 * This is usually the case for tests where the Compose content is set by that Activity, instead of
 * via [ComposeContext.setContent], provided through the extension. Make sure that you add the provided
 * Activity to your app's manifest file.
 */
public inline fun <reified A : ComponentActivity> createAndroidComposeExtension(): AndroidComposeExtension {
    return createAndroidComposeExtension(A::class.java)
}

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API
 * for field injection. Prefer this over [createComposeExtension] if your tests require a custom Activity.
 * This is usually the case for tests where the Compose content is set by that Activity, instead of
 * via [ComposeContext.setContent], provided through the extension. Make sure that you add the provided
 * Activity to your app's manifest file.
 */
public fun <A : ComponentActivity> createAndroidComposeExtension(
    activityClass: Class<A>
): AndroidComposeExtension {
    return AndroidComposeExtension(
        ruleFactory = { createAndroidComposeRule(activityClass) }
    )
}

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
