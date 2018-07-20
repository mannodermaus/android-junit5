package de.mannodermaus.gradle.plugins.junit5

const val MIN_REQUIRED_GRADLE_VERSION = "4.7"
const val MIN_REQUIRED_AGP_VERSION = "3.2.0-alpha18"
const val VERSIONS_RESOURCE_NAME = "versions.properties"

const val EXTENSION_NAME = "junitPlatform"
const val DEP_CONFIGURATION_NAME = "junitPlatform"
const val FILTERS_EXTENSION_NAME = "filters"
const val TAGS_EXTENSION_NAME = "tags"
const val ENGINES_EXTENSION_NAME = "engines"

// Mirrored from "versions.properties" resource file
const val ANDROID_JUNIT5_VERSION_PROP = "androidJunit5Version"
const val ANDROID_JUNIT5_RUNTIME_VERSION_PROP = "androidJunit5RuntimeVersion"
const val JUNIT_PLATFORM_VERSION_PROP = "junitPlatformVersion"
const val JUNIT_JUPITER_VERSION_PROP = "junitJupiterVersion"
const val JUNIT_VINTAGE_VERSION_PROP = "junitVintageVersion"
const val JUNIT4_VERSION_PROP = "junit4Version"
const val INSTRUMENTATION_TEST_VERSION_PROP = "instrumentationTestVersion"

// Instrumentation Test integration
const val RUNNER_BUILDER_ARG = "runnerBuilder"
const val JUNIT5_RUNNER_BUILDER_CLASS_NAME = "de.mannodermaus.junit5.AndroidJUnit5Builder"

// Dependency Handler Names
const val DEP_HANDLER_NAME = "junit5"
