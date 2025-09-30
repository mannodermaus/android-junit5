import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  kotlin("android")
  id("jacoco")
  id("de.mannodermaus.android-junit5").version(Artifacts.Plugin.latestStableVersion)
}

val javaVersion = JavaVersion.VERSION_17

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
  }
}

android {
  namespace = "de.mannodermaus.junit5.sample"
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    applicationId = "de.mannodermaus.junit5.sample"
    minSdk = Android.sampleMinSdkVersion
    targetSdk = Android.targetSdkVersion
    versionCode = 1
    versionName = "1.0"

    // Make sure to use the AndroidJUnitRunner (or a sub-class) in order to hook in the JUnit 5 Test Builder
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // These two lines are not needed for a normal integration;
    // this sample project disables the automatic integration, so it must be done manually
    testInstrumentationRunnerArguments["runnerBuilder"] = "de.mannodermaus.junit5.AndroidJUnit5Builder"
    testInstrumentationRunnerArguments["configurationParameters"] = "junit.jupiter.execution.parallel.enabled=true,junit.jupiter.execution.parallel.mode.default=concurrent"

    buildFeatures {
      buildConfig = true
    }

    buildConfigField("boolean", "MY_VALUE", "true")

    testOptions {
      animationsDisabled = true
    }
  }

  // Add Kotlin source directory to all source sets
  sourceSets.forEach {
    it.java.srcDir("src/${it.name}/kotlin")
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }
}

junitPlatform {
  // Configure JUnit 5 tests here
  filters("debug") {
    excludeTags("slow")
  }

  // Using local dependency instead of Maven coordinates
  instrumentationTests.enabled = false
}

tasks.withType<Test> {
  testLogging.events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
}

dependencies {
  implementation(libs.kotlinStdLib)

  testImplementation(libs.junitJupiterApi)
  testImplementation(libs.junitJupiterParams)
  testRuntimeOnly(libs.junitJupiterEngine)

  androidTestImplementation(libs.junit4)
  androidTestImplementation(libs.androidXTestRunner)

  // Android Instrumentation Tests wth JUnit 5
  androidTestImplementation(libs.junitJupiterApi)
  androidTestImplementation(libs.junitJupiterParams)
  androidTestImplementation(libs.espressoCore)
  androidTestImplementation(project(":core"))
  androidTestRuntimeOnly(project(":runner"))
}
