package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.BaseExtension
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.util.GradleVersion
import java.util.Properties

/* General */

fun requireGradle(version: String, message: () -> String) {
  require(GradleVersion.current() >= GradleVersion.version(version)) {
    throw GradleException(message())
  }
}

fun loadProperties(resource: String): Properties {
  val properties = Properties()
  val stream = AndroidJUnitPlatformPlugin::class.java.getResourceAsStream(resource)
  stream.use { properties.load(it) }
  return properties
}

/* Extension Functions & Interoperability layer for Gradle */

/**
 * Create & add an Extension to the given container by name.
 */
inline fun <reified T> Any.createExtension(
    name: String,
    args: Array<Any> = emptyArray(),
    noinline init: (T.() -> Unit)? = null): T {
  // Access the Extension container of an object,
  // or raise an Exception if none are available
  if (this !is ExtensionAware) {
    throw IllegalArgumentException("Argument is not ExtensionAware: $this")
  }

  // Create & Configure the new extension
  val created = this.extensions.create(name, T::class.java, *args)
  init?.let { init(created) }
  return created
}

/**
 * Shorthand function to check for the existence of a plugin on a Project.
 */
fun Project.hasPlugin(name: String) = this.plugins.findPlugin(name) != null

/**
 * Obtain a project's Extension by name & directly cast it.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Project.extensionByName(name: String): T =
    this.extensions.getByName(name) as T

/**
 * Access the Android extension applied by a respective plugin.
 * Equivalent to "Project#android" in Groovy.
 */
val Project.android: BaseExtension
  get() = this.extensions.getByName("android") as BaseExtension

/**
 * Access the extra properties of a DependencyHandler.
 * Equivalent to "DependencyHandler#ext" in Groovy.
 */
val DependencyHandler.ext: ExtraPropertiesExtension
  get() {
    val aware = this as ExtensionAware
    return aware.extensions.getByName(
        ExtraPropertiesExtension.EXTENSION_NAME) as ExtraPropertiesExtension
  }

/**
 * Executes the given block within the context of
 * the plugin's transitive dependencies.
 * This is used in our custom dependency handlers, and is required
 * to be used lazily instead of eagerly. This is motivated by the
 * user's capability to override the versions utilized by the plugin to work.
 * We need to wait until the configuration is evaluated by Gradle before
 * accessing our plugin Extension's parameters.
 */
fun Project.withDependencies(defaults: Properties, config: (Versions) -> Any): Any {
  val versions = Versions(
      project = this,
      extension = extensionByName(Constants.EXTENSION_NAME),
      defaults = defaults)
  return config(versions)
}

/* Types */

/**
 * Multi-language functional construct,
 * mapped to Groovy's dynamic Closures as well as Kotlin's invoke syntax.
 *
 * A [Callable] can be invoked with the short-hand
 * function syntax from both Kotlin & Groovy:
 *
 * <code><pre>
 *   val callable = Callable { 2 + 2 }
 *   val result = callable()  // result == 4
 * </pre></code>
 *
 * <code><pre>
 *   def callable = new Callable({ 2 + 2 })
 *   def result = callable()  // result == 4
 * </pre></code>
 */
@Suppress("unused")
class Callable(private val body: () -> Any) : Closure<Any>(null) {
  /** Kotlin's call syntax */
  operator fun invoke(): Any = body()

  /** Groovy's call syntax */
  fun doCall(): Any = body()
}
