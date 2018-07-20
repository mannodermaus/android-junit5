package de.mannodermaus.gradle.plugins.junit5.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import com.github.zafarkhaja.semver.Version
import de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformPlugin
import de.mannodermaus.gradle.plugins.junit5.JUnit5TaskConfig
import de.mannodermaus.gradle.plugins.junit5.VariantTypeCompat
import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationKind.APP
import de.mannodermaus.gradle.plugins.junit5.variantData
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.ERROR
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.logging.LogLevel.WARN
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.util.GradleVersion
import java.util.Properties

/**
 * Kotlin Extension Functions internal to the plugin's features and functionality.
 * Going forward, these members are subject to unforeseen renaming, deletion or other modifications.
 * Clients should not rely on the stability of this file's contents.
 */

/* General */

internal fun requireGradle(version: String, message: () -> String) {
  require(GradleVersion.current() >= GradleVersion.version(version)) {
    throw GradleException(message())
  }
}

fun requireVersion(actual: String, required: String, message: () -> String) {
  val actualVersion = Version.valueOf(actual)
  val requiredVersion = Version.valueOf(required)
  require(actualVersion.greaterThanOrEqualTo(requiredVersion)) {
    throw GradleException(message())
  }
}

internal fun loadProperties(resource: String): Properties {
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

/* Extensions for Gradle */

/**
 * Log a message with the Android Gradle Plugin style syntax,
 * which will cause Android Studio to pick it up & display it inside the Messages window.
 */
fun Logger.agpLog(level: LogLevel, message: String) {
  val pair: Pair<String, (String) -> Unit> = when (level) {
    ERROR -> "error" to { s -> error(s) }
    WARN -> "warning" to { s -> warn(s) }
    INFO -> "info" to { s -> info(s) }
    else -> "debug" to { s -> debug(s) }
  }
  val (kind, log) = pair
  log("""AGBPI: {"kind": "$kind","text":"$message"}""")
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

fun Any.extensionExists(name: String): Boolean {
  if (this !is ExtensionAware) {
    throw IllegalArgumentException("Argument is not ExtensionAware: $this")
  }

  return this.extensions.findByName(name) != null
}

/**
 * Obtain an Extension by name & directly cast it to the expected type.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.extensionByName(name: String): T {
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

internal fun Project.createJUnit5ConfigurationFor(variant: BaseVariant) =
    JUnit5TaskConfig(variant, this)

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
 * Obtains the {AndroidUnitTest} for the provided variant.
 */
fun TaskContainer.testTaskOf(variant: BaseVariant): AndroidUnitTest {
  val taskName = variant.variantData.scope.getTaskName(VariantTypeCompat.UNIT_TEST_PREFIX,
      VariantTypeCompat.UNIT_TEST_SUFFIX)
  return getByName(taskName) as AndroidUnitTest
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

val Test.junitPlatformOptions: JUnitPlatformOptions
  get() = (this.testFramework as JUnitPlatformTestFramework).options

val Test.includeTags: Set<String>
  get() = junitPlatformOptions.includeTags

val Test.excludeTags: Set<String>
  get() = junitPlatformOptions.excludeTags

val Test.includeEngines: Set<String>
  get() = junitPlatformOptions.includeEngines

val Test.excludeEngines: Set<String>
  get() = junitPlatformOptions.excludeEngines

/**
 * Obtains a configuration using version-agnostic identifiers and an optional BaseVariant.
 * For instance, using ConfigurationKind.ANDROID_TEST and ConfigurationScope.RUNTIME_ONLY,
 * this would resolve the "androidTestApk" configuration on AGP 2,
 * and the "androidTestRuntimeOnly" configuration on AGP 3.
 */
fun Set<Configuration>.find(
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
