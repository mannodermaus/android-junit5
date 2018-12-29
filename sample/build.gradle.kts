import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
  rootProject.apply { from(rootProject.file("gradle/dependencies.gradle.kts")) }

  repositories {
    google()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }

  dependencies {
    val latest = extra["android-junit5.plugin.latestVersion"]
    classpath("de.mannodermaus.gradle.plugins:android-junit5:$latest")
  }
}

plugins {
  id("com.android.application")
  kotlin("android")
  id("jacoco")
}

apply {
  plugin("de.mannodermaus.android-junit5")
}

android {
  compileSdkVersion(extra["android.compileSdkVersion"] as String)

  dexOptions {
    javaMaxHeapSize = extra["android.javaMaxHeapSize"] as String
  }

  defaultConfig {
    applicationId = "de.mannodermaus.junit5.sample"
    minSdkVersion(extra["android.sampleMinSdkVersion"] as Int)
    targetSdkVersion(extra["android.targetSdkVersion"] as Int)
    versionCode = 1
    versionName = "1.0"

    // Make sure to use the AndroidJUnitRunner (or a sub-class) in order to hook in the JUnit 5 Test Builder
    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArgument("runnerBuilder",
        "de.mannodermaus.junit5.AndroidJUnit5Builder")
  }

  // Since the minSdkVersion requirement for JUnit 5 Instrumentation Tests is quite high,
  // we introduce a product flavor that uses an elevated version other than the application's default.
  // With this, we are able to try JUnit 5 tests without sacrificing the minSdkVersion completely.
  flavorDimensions("kind")
  productFlavors {
    create("experimental") {
      dimension = "kind"
      minSdkVersion(26)
    }

    create("normal") {
      dimension = "kind"
    }
  }

  // Add Kotlin source directory to all source sets
  sourceSets.forEach {
    it.java.srcDir("src/${it.name}/kotlin")
  }

  compileOptions {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
  }

  testOptions {
    junitPlatform {
      // Configure JUnit 5 tests here
    }
  }

  packagingOptions {
    exclude("META-INF/LICENSE.md")
    exclude("META-INF/LICENSE-notice.md")
  }
}

tasks.withType<Test> {
  testLogging.events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
}

dependencies {
  implementation(kotlin("stdlib", extra["versions.kotlin"] as String))

  testImplementation(extra["libs.junitJupiterApi"] as String)
  testImplementation(extra["libs.junitJupiterParams"] as String)
  testRuntimeOnly(extra["libs.junitJupiterEngine"] as String)

  androidTestImplementation(extra["libs.junit4"] as String)
  androidTestImplementation(extra["libs.androidTestRunner"] as String)

  // Add the Android Instrumentation Test dependencies to the product flavor only
  // (with this, only the "experimental" flavor must have minSdkVersion 26)
  val latestVersion = extra["android-junit5.instrumentation.latestVersion"]
  val androidTestExperimentalImplementation by configurations
  androidTestExperimentalImplementation(extra["libs.junitJupiterApi"] as String)
  androidTestExperimentalImplementation(
      "de.mannodermaus.junit5:android-instrumentation-test:$latestVersion")

  // Runtime dependencies for Android Instrumentation Tests
  val androidTestExperimentalRuntimeOnly by configurations
  androidTestExperimentalRuntimeOnly(extra["libs.junitJupiterEngine"] as String)
  androidTestExperimentalRuntimeOnly(extra["libs.junitPlatformRunner"] as String)
  androidTestExperimentalRuntimeOnly(
      "de.mannodermaus.junit5:android-instrumentation-test-runner:$latestVersion")
}
