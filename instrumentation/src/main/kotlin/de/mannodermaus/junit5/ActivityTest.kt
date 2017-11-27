package de.mannodermaus.junit5

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.support.test.InstrumentationRegistry
import android.support.test.runner.MonitoringInstrumentation
import android.util.Log
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

/* Constants */

private const val ABSENT_TARGET_PACKAGE = "-"
private const val NO_FLAGS_SET = 0
private const val DEFAULT_LAUNCH_ACTIVITY = true
private const val DEFAULT_INITIAL_TOUCH_MODE = true

private const val LOG_TAG = "ActivityTest"

/* Public API */

/**
 * Marker annotation providing functional testing of an [Activity], applied
 * to either a single method annotated with [Test], or the class in which
 * it is contained. What used to be ActivityTestRule is now this annotation.
 *
 * If [launchActivity] is set to <code>true</code>, the Activity under test
 * will be launched automatically before the test is executing. This is also
 * the default behaviour if the parameter is unspecified. If this is undesired,
 * the Activity can be launched manually by adding a parameter of type [Tested]
 * to the method in question.
 *
 * @param value The activity under test. This must be a class in the instrumentation
 *     targetPackage specified in the AndroidManifest.xml
 * @param targetPackage The name of the target package that the Activity is started under
 * @param launchFlags [Intent] flags to start the Activity under test with
 * @param launchActivity Whether or not to automatically launch the Activity before the test execution
 */
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@ExtendWith(ActivityTestExtension::class)
annotation class ActivityTest(
    val value: KClass<out Activity>,
    val targetPackage: String = ABSENT_TARGET_PACKAGE,
    val launchFlags: Int = NO_FLAGS_SET,
    val initialTouchMode: Boolean = DEFAULT_INITIAL_TOUCH_MODE,
    val launchActivity: Boolean = DEFAULT_LAUNCH_ACTIVITY)

/**
 * Controller object representing an Activity under test.
 * It wraps around the current Activity and provides functionality
 * related to launching, finishing and recreating the Activity under test.
 *
 * To obtain an instance, add a parameter of type [Tested] to your test method
 * and assign it the generic type of the Activity described in the scope's [ActivityTest].
 */
class Tested<T : Activity>
internal constructor(
    // Configuration values provided through the test method, or its surrounding class
    internal val config: ActivityTest,
    // List of parameter types in the test method's signature
    private val parameterTypes: List<ParameterType>) {

  private val instrumentation = InstrumentationRegistry.getInstrumentation()

  // Public getter, private setter
  private var _activity: T? = null
  var activity: T? = null
    get() {
      if (_activity == null) {
        Log.w(LOG_TAG, "Activity wasn't created yet")
      }
      return _activity
    }

  /* Public API */

  /**
   * Launches the Activity under test.
   *
   * Don't call this method directly, unless you explicitly requested
   * not to lazily launch the Activity manually using the launchActivity flag
   * in the [ActivityTest] configuration annotation.
   *
   * @throws ActivityAlreadyLaunchedException if the Activity was already launched
   */
  @Suppress("UNCHECKED_CAST")
  fun launchActivity(intent: Intent? = null): T {
    if (activity != null) {
      throw ActivityAlreadyLaunchedException()
    }

    instrumentation.setInTouchMode(config.initialTouchMode)

    // Construct launcher Intent, injecting configuration along the way
    val startIntent = if (intent == null) {
      Log.i(LOG_TAG, "Launching with default: Intent(Intent.ACTION_MAIN)")
      Intent(Intent.ACTION_MAIN)
    } else {
      intent
    }

    if (startIntent.component == null) {
      // Fall back to the default Target Context's package name if none is set
      val targetPackage = if (config.targetPackage == ABSENT_TARGET_PACKAGE) {
        InstrumentationRegistry.getTargetContext().packageName
      } else {
        config.targetPackage
      }
      startIntent.setClassName(targetPackage, config.value.java.name)
    }

    if (startIntent.flags == NO_FLAGS_SET) {
      startIntent.addFlags(config.launchFlags)
    }

    Log.i(LOG_TAG, "Launching activity: ${startIntent.component}")

    this._activity = config.value.java.cast(instrumentation.startActivitySync(startIntent)) as T

    instrumentation.waitForIdleSync()

    if (activity == null) {
      // Log an error message because the Activity failed to launch
      val errorMessage = "$LOG_TAG Activity ${startIntent.component} failed to launch"
      val bundle = Bundle()
      bundle.putString(Instrumentation.REPORT_KEY_STREAMRESULT, errorMessage)
      instrumentation.sendStatus(0, bundle)
      Log.e(LOG_TAG, errorMessage)
    }

    // Blow up if necessary
    return activity!!
  }

  /**
   * Finishes the currently launched Activity.
   *
   * @throws ActivityNotLaunchedException if the Activity is not running
   */
  fun finishActivity() {
    val activity = this.activity ?: throw ActivityNotLaunchedException()

    activity.finish()
    _activity = null
    instrumentation.waitForIdleSync()
  }

  /**
   * This method can be used to retrieve the Activity result of an Activity that has called setResult.
   * Usually, the result is handled in onActivityResult of parent activity, that has called startActivityForResult.
   * This method must not be called before Activity.finish was called.
   *
   * @throws ActivityNotLaunchedException if the Activity is not running
   */
  fun getActivityResult(): Instrumentation.ActivityResult {
    val activity = this.activity ?: throw ActivityNotLaunchedException()
    return activity.result
  }

  /* Internal API */

  internal fun parameterTypeAt(index: Int): ParameterType = parameterTypes[index]

  internal fun onBeforeTestExecution() {
    // Check for undesirable states:
    // * Lacking a Tested<T> parameter with manual Activity launching is useless
    if (!config.launchActivity && !parameterTypes.contains(ParameterType.ValidTestedWrapper)) {
      throw MissingTestedParameterException(config.value.java)
    }

    // From here on, this method is mirroring the first half of
    // ActivityTestRule.ActivityStatement#evaluate().
    // TODO Include ActivityFactory checks eventually
//    val monitoringInstrumentation = this.instrumentation as? MonitoringInstrumentation

    if (config.launchActivity) {
      launchActivity(null)
    }
  }

  internal fun onAfterTestExecution() {
    // This method is mirroring the second half of
    // ActivityTestRule.ActivityStatement#evaluate().
    val monitoringInstrumentation = this.instrumentation as? MonitoringInstrumentation
    monitoringInstrumentation?.useDefaultInterceptingActivityFactory()

    if (activity != null) {
      finishActivity()
    }
  }
}

/* Internal API */

/**
 * JUnit Platform Extension revolving around support
 * for Activity-based instrumentation testing on Android.
 *
 * This Extension takes the place of the ActivityTestRule
 * from the JUnit4-centered Test Support Library.
 */
internal class ActivityTestExtension : BeforeTestExecutionCallback, ParameterResolver, AfterTestExecutionCallback {

  private lateinit var delegate: Tested<out Activity>

  /* BeforeTestExecution */

  override fun beforeTestExecution(context: ExtensionContext) {
    // Construct a controlling Delegate to drive the test with
    val config = context.findActivityTestConfig() ?: return
    val parameterTypes = context.requiredTestMethod.parameters
        .map { it.describeTypeInRelationToClass(config.value) }

    this.delegate = Tested(config, parameterTypes)
    this.delegate.onBeforeTestExecution()
  }

  private fun ExtensionContext.findActivityTestConfig(): ActivityTest? =
      sequenceOf(element, parent.flatMap { it.element })
          .filter { it.isPresent }
          .map { AnnotationSupport.findAnnotation(it.get(), ActivityTest::class.java) }
          .filter { it.isPresent }
          .map { it.get() }
          .firstOrNull()

  private fun Parameter.describeTypeInRelationToClass(targetClass: KClass<*>): ParameterType {
    val type = parameterizedType
    val activityClassJava = targetClass.java

    when (type) {
      is Class<*> -> {
        if (type == activityClassJava) {
          return ParameterType.Activity
        }
      }
      is ParameterizedType -> {
        if (type.rawType == Tested::class.java) {
          val argumentType = type.actualTypeArguments[0] as Class<*>
          return if (argumentType == activityClassJava) {
            ParameterType.ValidTestedWrapper

          } else {
            ParameterType.InvalidTestedWrapper(argumentType)
          }
        }
      }
    }

    return ParameterType.Unknown
  }

  /* ParameterResolver */

  override fun supportsParameter(parameterContext: ParameterContext,
      extensionContext: ExtensionContext): Boolean {
    val parameterType = delegate.parameterTypeAt(parameterContext.index)

    return when (parameterType) {
    // Possibly a developer error; throw a descriptive exception early
      is ParameterType.InvalidTestedWrapper -> throw UnexpectedActivityException(
          expected = delegate.config.value.java,
          actual = parameterType.actual)

    // Otherwise, allow only valid types to pass
      else -> parameterType.valid
    }
  }

  override fun resolveParameter(parameterContext: ParameterContext,
      extensionContext: ExtensionContext): Any? {
    val parameterType = delegate.parameterTypeAt(parameterContext.index)

    return when (parameterType) {
    // val parameter: Activity
      ParameterType.Activity -> delegate.activity

    // val parameter: Tested<Activity>
      ParameterType.ValidTestedWrapper -> delegate

    // Otherwise, library error (supportsParameter() should filter it)
      else -> throw IllegalArgumentException(
          "Unexpected ParameterType resolution requested for '$parameterType'")
    }
  }

  /* AfterTestExecution */

  override fun afterTestExecution(context: ExtensionContext) {
    this.delegate.onAfterTestExecution()
  }
}

/**
 * Marker values representing the kind of parameter
 * used by an [ActivityTest] method.
 */
internal sealed class ParameterType(val valid: Boolean) {

  /* Positive */

  /**
   * The parameter is equal to the [ActivityTestExtension]'s
   * "Activity under test".
   */
  object Activity : ParameterType(valid = true)

  /**
   * The parameter is a [Tested] controller with the correct
   * "Activity under test" sub-type.
   */
  object ValidTestedWrapper : ParameterType(valid = true)

  /* Negative */

  /**
   * The parameter is a [Tested] controller, but the sub-type
   * doesn't match the declared "Activity under test" in the
   * [ActivityTestExtension].
   */
  class InvalidTestedWrapper(val actual: Class<*>) : ParameterType(valid = false)

  /**
   * The parameter is of unknown type to the [ActivityTestExtension].
   */
  object Unknown : ParameterType(valid = false)
}
