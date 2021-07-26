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

  lintOptions {
    // JUnit 4 refers to java.lang.management APIs, which are absent on Android.
    warning("InvalidPackage")
  }

  packagingOptions {
    exclude("META-INF/LICENSE.md")
    exclude("META-INF/LICENSE-notice.md")
  }

  testOptions {
    junitPlatform {
      // Using local dependency instead of Maven coordinates
      instrumentationTests.integrityCheckEnabled = false
    }

    unitTests.apply {
      isReturnDefaultValues = true
    }
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

dependencies {
  implementation(libs.kotlinStdLib)
  implementation(libs.junitJupiterApi)
  api(libs.androidXTestCore)
  // This is required by the "instrumentation-runner" companion library,
  // since it can't provide any JUnit 5 runtime libraries itself
  // due to fear of prematurely incrementing the minSdkVersion requirement.
  runtimeOnly(libs.junitPlatformRunner)
  runtimeOnly(libs.junitJupiterEngine)

  androidTestImplementation(libs.junitJupiterApi)
  androidTestImplementation(libs.junitJupiterParams)
  androidTestImplementation(libs.espressoCore)
  androidTestRuntimeOnly(project(":runner"))
  androidTestRuntimeOnly(libs.junitJupiterEngine)

  testImplementation(libs.junitJupiterApi)
  testImplementation(libs.mockitoCore)
  testImplementation(libs.mockitoKotlin)
  testImplementation(libs.truth)
  testImplementation(libs.truthJava8Extensions)
}

project.configureDeployment(Artifacts.Instrumentation.Core)
