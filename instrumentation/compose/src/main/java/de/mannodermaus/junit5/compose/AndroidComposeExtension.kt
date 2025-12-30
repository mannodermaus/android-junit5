package de.mannodermaus.junit5.compose

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.AndroidComposeUiTestEnvironment
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.core.app.ActivityScenario
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API for
 * field injection. Prefer this over [createAndroidComposeExtension] if you don't care about the
 * specific activity class to use under the hood.
 *
 * ```
 * class MyTests {
 *   @JvmField
 *   @RegisterExtension
 *   val extension = createComposeExtension()
 * }
 * ```
 */
@ExperimentalTestApi
public fun createComposeExtension(): ComposeExtension =
    createAndroidComposeExtension<ComponentActivity>()

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API for
 * field injection. Prefer this over [createComposeExtension] if your tests require a custom
 * Activity. This is usually the case for tests where the Compose content is set by that Activity,
 * instead of via [ComposeContext.setContent], provided through the extension. Make sure that you
 * add the provided Activity to your app's manifest file.
 */
@ExperimentalTestApi
public inline fun <reified A : ComponentActivity> createAndroidComposeExtension():
    AndroidComposeExtension<A> {
    return createAndroidComposeExtension(A::class.java)
}

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API for
 * field injection. Prefer this over [createComposeExtension] if your tests require a custom
 * Activity. This is usually the case for tests where the Compose content is set by that Activity,
 * instead of via [ComposeContext.setContent], provided through the extension. Make sure that you
 * add the provided Activity to your app's manifest file. This variant allows you to provide a
 * custom [ActivityScenario] which is useful in cases where you may want to launch an activity with
 * a custom intent, for example.
 */
@ExperimentalTestApi
public inline fun <reified A : ComponentActivity> createAndroidComposeExtension(
    noinline scenarioSupplier: () -> ActivityScenario<A>
): AndroidComposeExtension<A> {
    return createAndroidComposeExtension(A::class.java, scenarioSupplier)
}

/**
 * Factory method to provide a JUnit 5 extension for Compose using its [RegisterExtension] API for
 * field injection. Prefer this over [createComposeExtension] if your tests require a custom
 * Activity. This is usually the case for tests where the Compose content is set by that Activity,
 * instead of via [ComposeContext.setContent], provided through the extension. Make sure that you
 * add the provided Activity to your app's manifest file. You may also provide an optional
 * [ActivityScenario] supplier which is useful in cases where you may want to launch an activity
 * with a custom intent,for example.
 */
@ExperimentalTestApi
public fun <A : ComponentActivity> createAndroidComposeExtension(
    activityClass: Class<A>,
    scenarioSupplier: () -> ActivityScenario<A> = { ActivityScenario.launch(activityClass) },
): AndroidComposeExtension<A> {
    return AndroidComposeExtension(scenarioSupplier)
}

/**
 * A JUnit 5 [Extension] that allows you to test and control [Composable]s and application using
 * Compose. The functionality of testing Compose is provided by means of the [runComposeTest]
 * method, which receives a [ComposeContext] from which the test can be orchestrated. The test will
 * block until the app or composable is idle, to ensure the tests are deterministic.
 *
 * This extension can be added to any JUnit 5 class using the [ExtendWith] annotation, or registered
 * globally through a configuration file. Alternatively, you can instantiate the extension in a
 * field within the test class using any of the [createComposeExtension] or
 * [createAndroidComposeExtension] factory methods.
 */
@SuppressLint("NewApi")
@OptIn(ExperimentalTestApi::class)
public class AndroidComposeExtension<A : ComponentActivity>
internal constructor(private val scenarioSupplier: () -> ActivityScenario<A>) :
    ComposeExtension,
    BeforeEachCallback,
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    AfterEachCallback,
    ParameterResolver {

    // Management of pending test operations
    private val pendingPrepareBlocks = mutableListOf<ComposeContext.() -> Unit>()
    private var environment: AndroidComposeUiTestEnvironment<A>? = null
    private var _scenario: ActivityScenario<A>? = null

    // Instantiated by JUnit 5
    @Suppress("UNCHECKED_CAST", "unused")
    internal constructor() :
        this(
            scenarioSupplier = {
                ActivityScenario.launch(ComponentActivity::class.java) as ActivityScenario<A>
            }
        )

    public val scenario: ActivityScenario<A>
        get() = checkNotNull(_scenario) { "Activity scenario could not be launched" }

    public val activity: A
        get() = checkNotNull(environment?.test?.activity) { "Host activity not found" }

    private var state = STATE_INIT

    override fun use(block: ComposeContext.() -> Unit) {
        when (state) {
            STATE_INIT -> {
                pendingPrepareBlocks.add(block)
            }

            STATE_READY -> {
                requireNotNull(environment).runTest {
                    val bridge = ComposeContextImpl(this)

                    _scenario = scenarioSupplier()
                    pendingPrepareBlocks.forEach { bridge.it() }
                    bridge.block()
                }

                state = STATE_CALLED
            }

            STATE_CALLED -> {
                throw IllegalStateException(
                    "Only a single call to use() is allowed per @Test method"
                )
            }

            else -> {
                throw IllegalStateException("Cannot call use() from @AfterEach or @AfterAll method")
            }
        }
    }

    /* BeforeEachCallback */

    override fun beforeEach(context: ExtensionContext) {
        state = STATE_INIT

        environment = AndroidComposeUiTestEnvironment { getActivityFromScenario(scenario) }
    }

    /* BeforeTestExecutionCallback */

    override fun beforeTestExecution(context: ExtensionContext) {
        state = STATE_READY
    }

    /* AfterTestExecutionCallback */

    override fun afterTestExecution(context: ExtensionContext) {
        state = STATE_STALE
    }

    /* AfterEachCallback */

    override fun afterEach(context: ExtensionContext) {
        pendingPrepareBlocks.clear()
        environment = null

        scenario.close()
        _scenario = null
    }

    /* ParameterResolver */

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        return ComposeExtension::class.java.isAssignableFrom(parameterContext.parameter.type)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any {
        return this
    }
}

private const val STATE_INIT = 0
private const val STATE_READY = 1
private const val STATE_CALLED = 2
private const val STATE_STALE = 3

private fun <A : ComponentActivity> getActivityFromScenario(scenario: ActivityScenario<A>): A? {
    var activity: A? = null
    scenario.onActivity { activity = it }
    return activity
}
