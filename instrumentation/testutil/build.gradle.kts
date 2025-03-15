import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
}

val javaVersion = JavaVersion.VERSION_11

android {
  namespace = "de.mannodermaus.junit5.testutil"
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = 19
    multiDexEnabled = true
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

dependencies {
  implementation(project(":testutil-reflect"))
  implementation(libs.androidXMultidex)

  api(libs.androidXTestMonitor)
  api(libs.truth)
  api(libs.truthJava8Extensions)
  api(libs.mockitoCore)
  api(libs.mockitoKotlin)
  api(libs.junitJupiterApi)
  api(libs.junitJupiterParams)
  api(libs.junitPlatformRunner)
}
