package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import groovy.lang.Closure
import java.io.File

/*
 * Special Extension Methods for accessors
 * that need to reach into Groovy because of the
 * unfortunate lack of visibility modifiers in the main JUnit 5 Gradle Plugin,
 * which prevents the static typing of Kotlin from working properly.
 */

val BaseVariant.variantData: BaseVariantData
  get() = GroovyInterop.baseVariant_variantData(this)

val VariantScope.safeJavaOutputDir: File
  get() = GroovyInterop.variantScope_javaOutputDir(this)

/* Types */

/**
 * Multi-language functional construct with no parameters,
 * mapped to Groovy's dynamic Closures as well as Kotlin's invoke syntax.
 *
 * A [Callable0] can be invoked with the short-hand
 * function syntax from both Kotlin & Groovy:
 *
 * <code><pre>
 *   val callable = Callable0 { 2 + 2 }
 *   val result = callable()  // result == 4
 * </pre></code>
 *
 * <code><pre>
 *   def callable = new Callable0({ 2 + 2 })
 *   def result = callable()  // result == 4
 * </pre></code>
 */
@Suppress("unused")
class Callable0<R>(private val body: () -> R) : Closure<R>(null) {
  /** Kotlin's call syntax */
  operator fun invoke(): R = body()

  /** Groovy's call syntax */
  fun doCall(): R = body()
}

/**
 * Multi-language functional construct with 1 parameter,
 * mapped to Groovy's dynamic Closures as well as Kotlin's invoke syntax.
 *
 * A [Callable1] can be invoked with the short-hand
 * function syntax from both Kotlin & Groovy:
 *
 * <code><pre>
 *   val callable = Callable1 { 2 + it }
 *   val result = callable(2)  // result == 4
 * </pre></code>
 *
 * <code><pre>
 *   def callable = new Callable1({ input ->  2 + input })
 *   def result = callable(2)  // result == 4
 * </pre></code>
 */
@Suppress("unused")
class Callable1<T : Any, out R : Any>(private val body: T.() -> R?) : Closure<T>(null) {
  /** Kotlin's call syntax */
  operator fun invoke(arg: T): R? = arg.body()

  /** Groovy's call syntax */
  fun doCall(arg: T): R? = arg.body()
}
