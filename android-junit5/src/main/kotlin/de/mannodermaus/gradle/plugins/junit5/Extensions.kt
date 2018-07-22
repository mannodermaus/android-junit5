package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.internal.dsl.TestOptions
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName

/**
 * Kotlin Extension Functions available for usage by clients, e.g. inside a Kotlin Gradle Script environment.
 * Shorthand properties to access different plugins' extension models and configuration in a concise matter.
 */

/**
 * Provides access to the JUnit Platform configuration settings exposed by the plugin
 */
val TestOptions.junitPlatform
  get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)
