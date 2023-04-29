import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  id("explicit-api-mode")
  id("de.mannodermaus.android-junit5").version(Artifacts.Plugin.latestStableVersion)
}

val javaVersion = JavaVersion.VERSION_1_8

android {
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = Android.testCoreMinSdkVersion
    targetSdk = Android.targetSdkVersion
    multiDexEnabled = true

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["runnerBuilder"] = "de.mannodermaus.junit5.AndroidJUnit5Builder"
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
    warning("InvalidPackage")
  }

  packagingOptions {
    resources.excludes.add("META-INF/LICENSE.md")
    resources.excludes.add("META-INF/LICENSE-notice.md")
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

junitPlatform {
  // Using local dependency instead of Maven coordinates
  instrumentationTests.enabled = false
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = javaVersion.toString()
  kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
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

  testImplementation(project(":testutil"))
}

project.configureDeployment(Artifacts.Instrumentation.Core)
