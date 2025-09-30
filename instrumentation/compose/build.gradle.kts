import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.library")
  kotlin("android")
  id("explicit-api-mode")
  id("de.mannodermaus.android-junit5").version(Artifacts.Plugin.latestStableVersion)
  id("org.jetbrains.kotlin.plugin.compose")
}

val javaVersion = JavaVersion.VERSION_17

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
  }
}

android {
  namespace = "de.mannodermaus.junit5.compose"
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = Android.testComposeMinSdkVersion

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

  testOptions {
    unitTests.isReturnDefaultValues = true
    targetSdk = Android.targetSdkVersion
  }

  lint {
    targetSdk = Android.targetSdkVersion
  }

  packaging {
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
  implementation(project(":core"))
  implementation(libs.kotlinStdLib)
  implementation(libs.kotlinCoroutinesCore)

  implementation(libs.junitJupiterApi)
  implementation(libs.junit4)
  implementation(libs.espressoCore)

  implementation(platform(libs.composeBom))
  implementation(libs.composeActivity)
  implementation(libs.composeUi)
  implementation(libs.composeUiTooling)
  implementation(libs.composeFoundation)
  implementation(libs.composeMaterial)
  api(libs.composeUiTest)
  api(libs.composeUiTestJUnit4)
  implementation(libs.composeUiTestManifest)

  testImplementation(libs.junitJupiterApi)
  testImplementation(libs.junitJupiterParams)
  testRuntimeOnly(libs.junitJupiterEngine)

  androidTestImplementation(libs.junitJupiterApi)
  androidTestImplementation(libs.junitJupiterParams)
  androidTestImplementation(libs.espressoCore)

  androidTestRuntimeOnly(project(":runner"))
  androidTestRuntimeOnly(libs.androidXTestRunner)
}

project.configureDeployment(Artifacts.Instrumentation.Compose)
