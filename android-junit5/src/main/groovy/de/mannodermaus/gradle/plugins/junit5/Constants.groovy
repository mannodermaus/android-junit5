package de.mannodermaus.gradle.plugins.junit5

class Constants {
  public static final String LOG_TAG = "[android-junit5]"

  public static final String MIN_REQUIRED_GRADLE_VERSION = "2.5"
  public static final String VERSIONS_RESOURCE_NAME = "versions.properties"

  public static final String EXTENSION_NAME = "junitPlatform"
  public static final String DEP_CONFIGURATION_NAME = "junitPlatform"
  public static final String SELECTORS_EXTENSION_NAME = "selectors"
  public static final String FILTERS_EXTENSION_NAME = "filters"
  public static final String PACKAGES_EXTENSION_NAME = "packages"
  public static final String TAGS_EXTENSION_NAME = "tags"
  public static final String ENGINES_EXTENSION_NAME = "engines"
  public static final String JACOCO_EXTENSION_NAME = "jacoco"

  // Mirrored from "versions.properties" resource file
  public static final String ANDROID_JUNIT5_VERSION_PROP = "androidJunit5Version"
  public static final String JUNIT_PLATFORM_VERSION_PROP = "junitPlatformVersion"
  public static final String JUNIT_JUPITER_VERSION_PROP = "junitJupiterVersion"
  public static final String JUNIT_VINTAGE_VERSION_PROP = "junitVintageVersion"
  public static final String JUNIT4_VERSION_PROP = "junit4Version"
}
