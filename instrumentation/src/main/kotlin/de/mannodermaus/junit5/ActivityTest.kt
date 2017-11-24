package de.mannodermaus.junit5

import android.app.Activity
import android.support.test.rule.ActivityTestRule
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.AnnotationSupport
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@ExtendWith(ActivityExtension::class)
annotation class ActivityTest(val value: KClass<out Activity>)

class ActivityExtension : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private lateinit var delegate: ActivityTestRule<out Activity>

  override fun beforeTestExecution(context: ExtensionContext) {
    val target = findTargetActivityClass(context) ?: return

    this.delegate = ActivityTestRule(target, true)
    this.delegate.launchActivity(null)
  }

  override fun supportsParameter(parameterContext: ParameterContext,
      extensionContext: ExtensionContext): Boolean {
    val target = findTargetActivityClass(extensionContext) ?: return false
    val parameter = parameterContext.parameter

    return target == parameter.type
  }

  override fun resolveParameter(parameterContext: ParameterContext,
      extensionContext: ExtensionContext): Any? {
    val activityClass = findTargetActivityClass(extensionContext) ?: return null
    val parameter = parameterContext.parameter

    if (activityClass == parameter.type) {
      return delegate.activity
    }

    return null
  }

  override fun afterTestExecution(context: ExtensionContext) {
    delegate.finishActivity()
  }

  /* Private */

  private fun findTargetActivityClass(context: ExtensionContext): Class<out Activity>? {
    return sequenceOf(context.element, context.parent.flatMap { it.element })
        .filter { it.isPresent }
        .map { AnnotationSupport.findAnnotation(it.get(), ActivityTest::class.java) }
        .filter { it.isPresent }
        .map { it.get().value.java }
        .firstOrNull()
  }
}
