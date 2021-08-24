import de.mannodermaus.gradle.plugins.junit5.junitPlatform
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

val javaVersion = JavaVersion.VERSION_1_8

android {
  compileSdkVersion(Android.compileSdkVersion)

  dexOptions {
    javaMaxHeapSize = Android.javaMaxHeapSize
  }

  defaultConfig {
    minSdkVersion(Android.testCoreMinSdkVersion)
    targetSdkVersion(Android.targetSdkVersion)
    multiDexEnabled = true

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArgument("runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder")
  }

  sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
    getByName("test").java.srcDir("src/test/kotlin")
    getByName("androidTest").java.srcDir("src/androidTest/kotlin")
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

junitPlatform {
  // Using local dependency instead of Maven coordinates
  instrumentationTests.integrityCheckEnabled = false
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

dependencies {
}

project.configureDeployment(Artifacts.Instrumentation.Compose)
