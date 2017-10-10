package de.mannodermaus.gradle.plugins.android_junit5

class AndroidJUnitPlatformAgp3xSpec extends AndroidJUnitPlatformSpec {

  @Override
  protected String testCompileDependencyName() {
    return "testImplementation"
  }

  @Override
  protected String testRuntimeDependencyName() {
    return "testRuntimeOnly"
  }
}
