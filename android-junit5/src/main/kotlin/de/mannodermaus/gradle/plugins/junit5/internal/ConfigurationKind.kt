package de.mannodermaus.gradle.plugins.junit5.internal

enum class ConfigurationKind(internal val value: String) {
  APP(""),
  TEST("test"),
  ANDROID_TEST("androidTest")
}
