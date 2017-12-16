package de.mannodermaus.gradle.plugins.junit5

/*
 * Unit testing the functionality of JUnit 5
 * with the Android Gradle Plugin version 2.
 */

class AGP2FunctionalSpec extends BaseFunctionalSpec {
  @Override
  String pluginClasspathResource() {
    return "plugin-2x-classpath.txt"
  }

  @Override
  String functionalTestCompileClasspathResource() {
    return "functional-test-compile-2x-classpath.txt"
  }

  @Override
  String concatProductFlavorsToString(List<String> flavorNames) {
    def flavors = flavorNames.collect { "$it {}" }.join("\n")

    return """
      productFlavors {
        $flavors
      }
    """
  }
}
