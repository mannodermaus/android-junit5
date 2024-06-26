import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  id("explicit-api-mode")
  id("de.mannodermaus.android-junit5").version(Artifacts.Plugin.latestStableVersion)
}

val javaVersion = JavaVersion.VERSION_11

android {
  namespace = "de.mannodermaus.junit5"
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = Android.testCoreMinSdkVersion
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

junitPlatform {
  filters {
    // See TaggedTests.kt for usage of this tag
    excludeTags("nope")
  }
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

// Use local project dependencies on android-test instrumentation libraries
// instead of relying on their Maven coordinates for this module
val instrumentationLibraryRegex = Regex("de\\.mannodermaus\\.junit5:android-test-(.+):")

configurations.all {
  if ("debugAndroidTestRuntimeClasspath" in name) {
    resolutionStrategy.dependencySubstitution.all {
      instrumentationLibraryRegex.find(requested.toString())?.let { result ->
        useTarget(project(":${result.groupValues[1]}"))
      }
    }
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

  // This transitive dependency of JUnit 5 is required to be on the runtime classpath,
  // since otherwise ART will print noisy logs to console when trying to resolve any
  // of the annotations of JUnit 5 (see #291 for more info)
  runtimeOnly(libs.apiguardianApi)

  androidTestImplementation(libs.junitJupiterApi)
  androidTestImplementation(libs.junitJupiterParams)
  androidTestImplementation(libs.espressoCore)
  androidTestRuntimeOnly(project(":runner"))

  testImplementation(project(":testutil"))
}

project.configureDeployment(Artifacts.Instrumentation.Core)
