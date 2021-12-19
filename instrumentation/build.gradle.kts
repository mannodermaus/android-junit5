plugins {
  id("io.github.gradle-nexus.publish-plugin").version("1.1.0")
  id("com.github.ben-manes.versions").version("0.39.0")
  id("org.jetbrains.kotlinx.binary-compatibility-validator").version("0.6.0")
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
    classpath(libs.plugins.dokka)

    // Use a different version of the Android Gradle Plugin
    // depending on the presence of Compose in the project
    if (project.isComposeIncluded) {
      classpath(libs.plugins.android(SupportedAgp.AGP_7_0))
    } else {
      classpath(libs.plugins.android(SupportedAgp.AGP_4_2))
    }
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
    sonatypeSnapshots()
  }
}

apiValidation {
  ignoredPackages.add("de.mannodermaus.junit5.internal")
  ignoredPackages.add("de.mannodermaus.junit5.compose.internal")
  ignoredProjects.add("sample")
}
