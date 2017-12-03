package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.internal.dsl.TestOptions
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.TaskContainer
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.util.GradleVersion
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.PackagesExtension
import org.junit.platform.gradle.plugin.SelectorsExtension
import org.junit.platform.gradle.plugin.TagsExtension
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

/**
 * Adds the provided key-value pair to the Map.
 * If there already is a value associated with the key,
 * the value is appended to the end of the current value
 * using the given delimiter.
 */
fun MutableMap<String, String>.append(
    key: String, value: String, delimiter: String = ","): String? {
  val insertedValue = if (containsKey(key)) {
    "${this[key]}$delimiter$value"
  } else {
    value
  }

  return this.put(key, insertedValue)
}

/*
 * "Extension" Extension Functions:
 * Shorthand properties to access different plugins' extension models.
 */

val AndroidJUnitPlatformExtension.selectors
  get() = extensionByName<SelectorsExtension>(SELECTORS_EXTENSION_NAME)

val AndroidJUnitPlatformExtension.filters
  get() = extensionByName<FiltersExtension>(FILTERS_EXTENSION_NAME)

val FiltersExtension.tags
  get() = extensionByName<TagsExtension>(TAGS_EXTENSION_NAME)

val FiltersExtension.packages
  get() = extensionByName<PackagesExtension>(PACKAGES_EXTENSION_NAME)

val FiltersExtension.engines
  get() = extensionByName<EnginesExtension>(ENGINES_EXTENSION_NAME)

@Deprecated(message = "will be removed")
val AndroidJUnitPlatformExtension.jacoco
  get() = extensionByName<AndroidJUnit5JacocoReport.Extension>(JACOCO_EXTENSION_NAME)

val TestOptions.junitPlatform
  get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

val Project.jacoco
  get() = extensionByName<JacocoPluginExtension>("jacoco")

val AndroidJUnit5UnitTest.jacoco
  get() = extensionByName<JacocoTaskExtension>("jacoco")

/* Interoperability layer for Gradle */

/**
 * Create & add an Extension to the given container by name.
 */
inline fun <reified T> Any.extend(
    name: String,
    args: Array<Any> = emptyArray(),
    noinline init: ((T) -> Unit)? = null): T {
  // Access the Extension container of an object,
  // or raise an Exception if none are available
  if (this !is ExtensionAware) {
    throw IllegalArgumentException("Argument is not ExtensionAware: $this")
  }

  // Create & Configure the new extension
  val created: T = this.extensions.create(name, T::class.java, *args)
  init?.let { init(created) }
  return created
}

/**
 * Obtain an Extension by name & directly cast it to the expected type.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> Any.extensionByName(name: String): T {
  if (this !is ExtensionAware) {
    throw IllegalArgumentException("Argument is not ExtensionAware: $this")
  }

  return this.extensions.getByName(name) as T
}

/**
 * Log the provided info message using the plugin's Log Tag.
 */
fun Logger.junit5Info(text: String) {
  info("[android-junit5]: $text")
}

/**
 * Shorthand function to check for the existence of a plugin on a Project.
 */
fun Project.hasPlugin(name: String) = this.plugins.findPlugin(name) != null

/**
 * Access the Android extension applied by a respective plugin.
 * Equivalent to "Project#android" in Groovy.
 */
val Project.android: BaseExtension
  get() = this.extensions.getByName("android") as BaseExtension

val Project.junit5: AndroidJUnitPlatformExtension
  get() = this.android.testOptions.junitPlatform

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

val BaseVariant.unitTestVariant: UnitTestVariant
  get() {
    if (this !is TestedVariant) {
      throw IllegalArgumentException("Argument is not TestedVariant: $this")
    }

    return this.unitTestVariant
  }

/**
 * Creates a task with the given properties,
 * unless it already exists in the task container,
 * in which case the existing task is returned.
 */
@Suppress("UNCHECKED_CAST")
fun TaskContainer.maybeCreate(name: String, group: String? = null): Task {
  val existing = findByName(name)
  return if (existing != null) {
    existing
  } else {
    val new = create(name)
    new.group = group
    new
  }
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
      extension = project.junit5,
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
class Callable0<R>(private val body: () -> R) : Closure<R>(null) {
  /** Kotlin's call syntax */
  operator fun invoke(): R = body()

  /** Groovy's call syntax */
  fun doCall(): R = body()
}

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
class Callable1<in T : Any, R>(private val body: (T) -> R) : Closure<R>(null) {
  /** Kotlin's call syntax */
  operator fun invoke(arg: T): R = body(arg)

  /** Groovy's call syntax */
  fun doCall(arg: T): R = body(arg)
}
