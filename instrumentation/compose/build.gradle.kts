import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  id("com.android.library")
  kotlin("android")
  id("explicit-api-mode")
  id("de.mannodermaus.android-junit5").version(Artifacts.Plugin.latestStableVersion)
}

val javaVersion = JavaVersion.VERSION_11

android {
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = Android.testComposeMinSdkVersion
    targetSdk = Android.targetSdkVersion

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["runnerBuilder"] = "de.mannodermaus.junit5.AndroidJUnit5Builder"
  }

  buildFeatures {
    compose = true
    buildConfig = false
    resValues = false
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  kotlinOptions {
    jvmTarget = javaVersion.toString()
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.compose
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }

  packagingOptions {
    resources.excludes.add("META-INF/AL2.0")
    resources.excludes.add("META-INF/LGPL2.1")
  }
}

junitPlatform {
  // Using local dependency instead of Maven coordinates
  instrumentationTests.enabled = false
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
  implementation(libs.kotlinCoroutinesCore)

  implementation(libs.junitJupiterApi)
  implementation(libs.junit4)
  implementation(libs.espressoCore)

  implementation(libs.composeUi)
  implementation(libs.composeUiTooling)
  implementation(libs.composeFoundation)
  implementation(libs.composeMaterial)
  api(libs.composeUiTest)
  api(libs.composeUiTestJUnit4)
  implementation(libs.composeUiTestManifest)

  androidTestImplementation(libs.junitJupiterApi)
  androidTestImplementation(libs.junitJupiterParams)
  androidTestImplementation(libs.espressoCore)

  androidTestImplementation(project(":core"))
  androidTestRuntimeOnly(project(":runner"))
  androidTestRuntimeOnly(libs.androidXTestRunner)
}

project.configureDeployment(Artifacts.Instrumentation.Compose)
