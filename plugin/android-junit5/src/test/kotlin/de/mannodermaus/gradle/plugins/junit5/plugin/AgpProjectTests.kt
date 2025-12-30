package de.mannodermaus.gradle.plugins.junit5.plugin

import de.mannodermaus.gradle.plugins.junit5.util.projects.PluginSpecProjectCreator.Builder

/**
 * Entry point for all supported versions of the Android Gradle Plugin. The parent class composes
 * all relevant unit tests for each of the plugin types.
 */
class AndroidAppProjectTests : AbstractProjectTests(Builder::asAndroidApplication)

class AndroidLibraryProjectTests : AbstractProjectTests(Builder::asAndroidLibrary)

class AndroidDynamicFeatureProjectTests : AbstractProjectTests(Builder::asAndroidDynamicFeature)
