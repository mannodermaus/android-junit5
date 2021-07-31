package de.mannodermaus.junit5

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import de.mannodermaus.junit5.ActivityScenarioExtension.Companion.launch
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.RegisterExtension
import java.lang.reflect.ParameterizedType

/**
 * JUnit 5 Extension for the [ActivityScenario] API,
 * provided by the AndroidX test core library.
 *
 * This extension is used in lieu of a JUnit 4 Rule to automatically
 * launch/stop an [ActivityScenario] for each test case.
 *
 * To use this extension in your test class, add it as a non-private instance field
 * to your test class, using one of the factory methods named [launch].
 * Then, annotate this field with JUnit Jupiter's [RegisterExtension] annotation.
 * In Kotlin, also add [JvmField] or else the generated property won't be visible to the TestEngine!
 *
 * ```
 *   // Java
 *   class MyActivityTests {
 *
 *     @RegisterExtension
 *     final ActivityScenarioExtension<MyActivity> scenarioExtension = ActivityScenarioExtension.launch(MyActivity.class);
 *   }
 *
 *   // Kotlin
 *   class MyActivityTests {
 *
 *     @JvmField
 *     @RegisterExtension
 *     val scenarioExtension = ActivityScenarioExtension.launch<MyActivity>()
 *   }
 * ```
 *
 * In your test method, you can obtain a reference to the scenario in two ways:
 *
 * A) You obtain it from the extension directly through its accessor method:
 *
 * ```
 *   // Java
 *   class MyActivityTests {
 *
 *     @RegisterExtension
 *     final ActivityScenarioExtension<MyActivity> scenarioExtension = ActivityScenarioExtension.launch(MyActivity.class);
 *
 *     @Test
 *     void myTest() {
 *       ActivityScenario<MyActivity> scenario = scenarioExtension.getScenario();
 *       // Use the scenario...
 *     }
 *   }
 *
 *   // Kotlin
 *   class MyActivityTests {
 *
 *     @JvmField
 *     @RegisterExtension
 *     val scenarioExtension = ActivityScenarioExtension.launch<MyActivity>()
 *
 *     @Test
 *     fun myTest() {
 *       val scenario = scenarioExtension.scenario
 *       // Use the scenario...
 *     }
 *   }
 * ```
 *
 * B) You add a parameter of type [ActivityScenario], with the activity class as its generic type:
 *
 * ```
 *   // Java
 *   class MyActivityTests {
 *
 *     @RegisterExtension
 *     final ActivityScenarioExtension<MyActivity> scenarioExtension = ActivityScenarioExtension.launch(MyActivity.class);
 *
 *     @Test
 *     void myTest(ActivityScenario<MyActivity> scenario) {
 *       // Use the scenario...
 *     }
 *   }
 *
 *   // Kotlin
 *   class MyActivityTests {
 *
 *     @JvmField
 *     @RegisterExtension
 *     val scenarioExtension = ActivityScenarioExtension.launch<MyActivity>()
 *
 *     @Test
 *     fun myTest(scenario: ActivityScenario<MyActivity>) {
 *       // Use the scenario...
 *     }
 *   }
 * ```
 *
 */
@TargetApi(Build.VERSION_CODES.O)
public class ActivityScenarioExtension<A : Activity>
private constructor(private val scenarioSupplier: () -> ActivityScenario<A>) : BeforeEachCallback,
    AfterEachCallback, ParameterResolver {

    public companion object {

        /**
         * Launches an activity of a given class and constructs an [ActivityScenario] for it.
         * A default launch intent without specific extras is used to launch the activity.
         */
        @JvmStatic
        public fun <A : Activity> launch(activityClass: Class<A>): ActivityScenarioExtension<A> =
            ActivityScenarioExtension { ActivityScenario.launch(activityClass) }

        /**
         * Launches an activity of a given class and constructs an [ActivityScenario] for it.
         * The given intent is used to launch the activity.
         */
        @JvmStatic
        public fun <A : Activity> launch(startActivityIntent: Intent): ActivityScenarioExtension<A> =
            ActivityScenarioExtension { ActivityScenario.launch<A>(startActivityIntent) }

        /* Kotlin-specific convenience variations */

        /**
         * Launches an activity of a given class and constructs an [ActivityScenario] for it.
         * A default launch intent without specific extras is used to launch the activity.
         */
        public inline fun <reified A : Activity> launch(): ActivityScenarioExtension<A> =
            launch(A::class.java)
    }

    /* Fields */

    private var _scenario: ActivityScenario<A>? = null

    /**
     * Returns the current [ActivityScenario] of the activity class.
     * @throws NullPointerException If this method is called while no test is running
     */
    public val scenario: ActivityScenario<A>
        get() = _scenario!!

    /* Methods */

    override fun beforeEach(context: ExtensionContext) {
        _scenario = scenarioSupplier()
    }

    override fun afterEach(context: ExtensionContext) {
        scenario.close()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        // The extension can resolve ActivityScenario parameters that use the correct activity type.
        val paramType = parameterContext.parameter.parameterizedType
        return paramType is ParameterizedType
                && paramType.rawType == ActivityScenario::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any = scenario
}
