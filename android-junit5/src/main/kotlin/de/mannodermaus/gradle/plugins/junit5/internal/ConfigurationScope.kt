package de.mannodermaus.gradle.plugins.junit5.internal

enum class ConfigurationScope(internal vararg val values: String) {
  API("api", "compile"),
  IMPLEMENTATION("implementation", "compile"),
  COMPILE_ONLY("compileOnly", "provided"),
  RUNTIME_ONLY("runtimeOnly", "apk")
}
