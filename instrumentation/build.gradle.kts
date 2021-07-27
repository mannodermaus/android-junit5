plugins {
  id("io.github.gradle-nexus.publish-plugin").version("1.1.0")
  id("com.github.ben-manes.versions").version("0.39.0")
}

buildscript {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    jitpack()
  }

  dependencies {
    classpath(libs.plugins.kotlin)
    classpath(libs.plugins.android)
    classpath(libs.plugins.dokka)
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
    sonatypeSnapshots()
  }
}
