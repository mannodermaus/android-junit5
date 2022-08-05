package de.mannodermaus.gradle.plugins.junit5.internal.config

internal const val MIN_REQUIRED_GRADLE_VERSION = "7.0"
internal const val MIN_REQUIRED_AGP_VERSION = "7.0.0"

internal const val EXTENSION_NAME = "junitPlatform"
internal const val FILTERS_EXTENSION_NAME = "filters"

internal const val ANDROID_JUNIT5_RUNNER_BUILDER_CLASS = "de.mannodermaus.junit5.AndroidJUnit5Builder"
internal const val INSTRUMENTATION_RUNNER_LIBRARY_GROUP = "de.mannodermaus.junit5"
internal const val INSTRUMENTATION_RUNNER_LIBRARY_ARTIFACT = "android-test-runner"

// Android doesn't allow '.' in resource file names,
// saying that it is not a valid file-based resource name character:
// File-based resource names must contain only lowercase a-z, 0-9, or underscore
internal const val INSTRUMENTATION_FILTER_RES_FILE_NAME = "de_mannodermaus_junit5_filters"
