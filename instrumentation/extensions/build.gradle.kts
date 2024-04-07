import libs.plugins.android
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    google()
    mavenCentral()
    sonatypeSnapshots()
  }

  dependencies {
    val latest = Artifacts.Plugin.latestStableVersion
    classpath("de.mannodermaus.gradle.plugins:android-junit5:$latest")
  }
}

plugins {
  id("com.android.library")
  kotlin("android")
  id("explicit-api-mode")
}

apply {
  plugin("de.mannodermaus.android-junit5")
}

val javaVersion = JavaVersion.VERSION_11

android {
  namespace = "de.mannodermaus.junit5.extensions"
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = Android.testRunnerMinSdkVersion
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  buildFeatures {
    buildConfig = false
    resValues = false
  }

  lint {
    // JUnit 4 refers to java.lang.management APIs, which are absent on Android.
    warning.add("InvalidPackage")
    targetSdk = Android.targetSdkVersion
  }

  packaging {
    resources.excludes.add("META-INF/LICENSE.md")
    resources.excludes.add("META-INF/LICENSE-notice.md")
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    targetSdk = Android.targetSdkVersion
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = javaVersion.toString()
}

tasks.withType<Test> {
  failFast = true
  testLogging {
    events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

configurations.all {
  // The Instrumentation Test Runner uses the plugin,
  // which in turn provides the Instrumentation Test Runner again -
  // that's kind of deep.
  // To avoid conflicts, prefer using the local classes
  // and exclude the dependency from being pulled in externally.
  exclude(module = Artifacts.Instrumentation.Extensions.artifactId)
}

dependencies {
  implementation(libs.androidXTestRunner)
  implementation(libs.junitJupiterApi)

  testImplementation(project(":testutil"))
  testRuntimeOnly(libs.junitJupiterEngine)
}

project.configureDeployment(Artifacts.Instrumentation.Extensions)
