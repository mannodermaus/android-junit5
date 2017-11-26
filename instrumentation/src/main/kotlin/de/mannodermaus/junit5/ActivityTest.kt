package de.mannodermaus.junit5

import android.app.Activity
import android.content.Intent
import android.support.test.rule.ActivityTestRule
import android.util.Log
import de.mannodermaus.junit5.ParameterType.PLAIN
import de.mannodermaus.junit5.ParameterType.WRAPPED
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.ParameterizedType
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

/* Public API */

/**
 * Marker annotation providing functional testing of an [Activity], applied
 * to either a single method annotated with [Test], or the class in which
 * it is contained.
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
    val targetPackage: String = "",
    val launchFlags: Int = 0,
    val launchActivity: Boolean = true)

/**
 * Controller object representing an Activity under test.
 * It wraps around the current Activity and provides functionalities
 * related to launching, finishing and recreating the Activity under test.
 *
 * To obtain an instance, add a parameter of type [Tested] to your test method
 * and assign it the generic type of the Activity described in the scope's [ActivityTest].
 */
class Tested<out T : Activity>(private val cls: Class<T>) {

  // TODO For example - populate using ActivityTestRule<T>
  fun launchActivity(intent: Intent? = null): T? = null
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

  // TODO Move to Tested<T>
  private lateinit var delegate: ActivityTestRule<out Activity>

  /* BeforeTestExecution */

  override fun beforeTestExecution(context: ExtensionContext) {
    val target = context.testedActivityClass() ?: return

    // TODO Move to Tested<T>
    this.delegate = ActivityTestRule(target, true)
    this.delegate.launchActivity(null)
  }

  /* ParameterResolver */

  override fun supportsParameter(parameterContext: ParameterContext,
      extensionContext: ExtensionContext): Boolean {
    val klass = extensionContext.testedActivityClass() ?: return false
    return getParameterType(klass, parameterContext) != null
  }

  override fun resolveParameter(parameterContext: ParameterContext,
      extensionContext: ExtensionContext): Any? {
    val klass = extensionContext.testedActivityClass() ?: return null
    val parameterType = getParameterType(klass, parameterContext) ?: return null
    return when (parameterType) {
      PLAIN -> delegate.activity
      WRAPPED -> Tested(klass)
    }
  }

  private fun getParameterType(
      targetClass: Class<out Activity>,
      parameterContext: ParameterContext): ParameterType? {
    val parameter = parameterContext.parameter
    val type = parameter.parameterizedType

    when (type) {
      is Class<*> -> {
        if (type == targetClass) {
          Log.w("ActivityTestExtension", "Is 'Class': $parameter")
          return PLAIN
        }
      }
      is ParameterizedType -> {
        if (type.rawType == Tested::class.java && type.actualTypeArguments[0] == targetClass) {
          Log.w("ActivityTestExtension", "Is 'Wrapped': $parameter")
          return WRAPPED
        }
      }
    }

    Log.w("ActivityTestExtension", "Is Nothing: ${parameter.parameterizedType}")
    return null
  }

  /* AfterTestExecution */

  override fun afterTestExecution(context: ExtensionContext) {
    // TODO Move to Tested<T>
    delegate.finishActivity()
  }

  /* Extension Functions */

  private fun ExtensionContext.testedActivityClass(): Class<out Activity>? =
      sequenceOf(element, parent.flatMap { it.element })
          .filter { it.isPresent }
          .map { AnnotationSupport.findAnnotation(it.get(), ActivityTest::class.java) }
          .filter { it.isPresent }
          .map { it.get().value.java }
          .firstOrNull()
}

/**
 * Marker values representing the kind of parameter
 * used by an [ActivityTest] method.
 */
internal enum class ParameterType { PLAIN, WRAPPED }
