package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.internal.dsl.TestOptions
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import de.mannodermaus.gradle.plugins.junit5.ConfigurationKind.APP
import de.mannodermaus.gradle.plugins.junit5.LogUtils.Level
import de.mannodermaus.gradle.plugins.junit5.LogUtils.Level.INFO
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
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

fun requireAgp3(message: () -> String) {
  val majorVersion = ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt()

  require(majorVersion >= 3) {
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

val TestOptions.junitPlatform
  get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

val Project.jacoco
  get() = extensionByName<JacocoPluginExtension>("jacoco")

val AndroidJUnit5UnitTest.jacoco
  get() = extensionByName<JacocoTaskExtension>("jacoco")

/* Extensions for Gradle */

fun Logger.agpStyleLog(message: String, level: Level = INFO) {
  LogUtils.agpStyleLog(this, level, message)
}

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
 * Obtains a configuration using version-agnostic identifiers and an optional BaseVariant.
 * For instance, using ConfigurationKind.ANDROID_TEST and ConfigurationScope.RUNTIME_ONLY,
 * this would resolve the "androidTestApk" configuration on AGP 2,
 * and the "androidTestRuntimeOnly" configuration on AGP 3.
 */
fun Set<Configuration>.findConfiguration(
    variant: BaseVariant? = null,
    kind: ConfigurationKind = APP,
    scope: ConfigurationScope): Configuration {
  val stemName = if (variant != null) {
    "${variant.name}${kind.value.capitalize()}"
  } else {
    kind.value
  }

  // Compose the configuration's name,
  // attempting all candidates before failing
  return scope.values
      .map { scopeName ->
        if (stemName.isEmpty()) {
          scopeName
        } else {
          "$stemName${scopeName.capitalize()}"
        }
      }
      .mapNotNull { configName -> this.firstOrNull { it.name == configName } }
      .first()
}

/* Types */

enum class ConfigurationKind(internal val value: String) {
  APP(""),
  TEST("test"),
  ANDROID_TEST("androidTest")
}

enum class ConfigurationScope(internal vararg val values: String) {
  API("api", "compile"),
  IMPLEMENTATION("implementation", "compile"),
  COMPILE_ONLY("compileOnly", "provided"),
  RUNTIME_ONLY("runtimeOnly", "apk")
}

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
class Callable1<in T : Any, R>(private val body: (T) -> R) : Closure<R>(null) {
  /** Kotlin's call syntax */
  operator fun invoke(arg: T): R = body(arg)

  /** Groovy's call syntax */
  fun doCall(arg: T): R = body(arg)
}
