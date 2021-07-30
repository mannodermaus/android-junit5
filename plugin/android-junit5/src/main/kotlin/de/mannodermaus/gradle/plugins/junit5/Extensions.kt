package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.config.EXTENSION_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionByName
import org.gradle.api.Project

/**
 * Kotlin Extension Functions available for usage by clients, e.g. inside a Kotlin Gradle Script environment.
 * Shorthand properties to access different plugins' extension models and configuration in a concise matter.
 */

/**
 * Provides access to the JUnit Platform configuration settings exposed by the plugin
 */
val Project.junitPlatform
  get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)
