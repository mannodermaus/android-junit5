package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.internal.dsl.TestOptions
import de.mannodermaus.gradle.plugins.junit5.internal.ext
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Kotlin Extension Functions available for usage by clients, e.g. inside a Kotlin Gradle Script environment.
 * Shorthand properties to access different plugins' extension models and configuration in a concise matter.
 */

/**
 * Provides access to the JUnit Platform configuration settings exposed by the plugin
 */
val TestOptions.junitPlatform
  get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

/**
 * Access the JUnit 5 dependency handler containing all relevant dependency groups.
 */
val DependencyHandler.junit5: JUnit5DependencyHandler
  get() = this.ext[DEP_HANDLER_NAME] as JUnit5DependencyHandler
