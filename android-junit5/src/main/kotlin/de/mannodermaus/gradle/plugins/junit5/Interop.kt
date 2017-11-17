package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension

/*
 * Interoperability layer to Gradle,
 * bridging the gap to accessing Groovy's
 * dynamic extension properties from Kotlin.
 */

/**
 * Access the extra properties of a DependencyHandler.
 * Equivalent to "DependencyHandler#ext" in Groovy.
 */
val DependencyHandler.ext: ExtraPropertiesExtension
  get() {
    val aware = this as ExtensionAware
    return aware.extensions.getByName("ext") as ExtraPropertiesExtension
  }

/**
 * Access the Android extension applied by a respective plugin.
 * Equivalent to "Project#android" in Groovy.
 */
val Project.android: BaseExtension
  get() = this.extensions.getByName("android") as BaseExtension

/**
 * Invokable functional construct,
 * mapped to Groovy's dynamic Closures.
 */
class Callable(private val body: () -> Any) {
  operator fun invoke(): Any = body.invoke()
}
