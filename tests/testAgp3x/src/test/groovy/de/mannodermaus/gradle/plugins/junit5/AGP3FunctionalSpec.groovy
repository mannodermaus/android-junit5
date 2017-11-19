package de.mannodermaus.gradle.plugins.junit5

/*
 * Unit testing the functionality of JUnit 5
 * with the Android Gradle Plugin version 3.
 */

class AGP3FunctionalSpec extends BaseFunctionalSpec {
  @Override
  String pluginClasspathResource() {
    return "plugin-3x-classpath.txt"
  }

  @Override
  String functionalTestCompileClasspathResource() {
    return "functional-test-compile-3x-classpath.txt"
  }

  @Override
  String concatProductFlavorsToString(List<String> flavorNames) {
    def flavors = flavorNames.collect { """$it { dimension "tier" }""" }.join("\n")

    return """
flavorDimensions "tier"
productFlavors {
  $flavors
}
"""
  }
}
