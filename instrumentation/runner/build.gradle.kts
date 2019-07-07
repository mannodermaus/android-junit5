import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    google()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
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

android {
  compileSdkVersion(Android.compileSdkVersion)

  dexOptions {
    javaMaxHeapSize = Android.javaMaxHeapSize
  }

  defaultConfig {
    minSdkVersion(Android.testRunnerMinSdkVersion)
    targetSdkVersion(Android.targetSdkVersion)
  }

  sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
    getByName("test").java.srcDir("src/test/kotlin")
    getByName("androidTest").java.srcDir("src/androidTest/kotlin")
  }

  compileOptions {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
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
    unitTests.apply {
      isReturnDefaultValues = true
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
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
  exclude(module = Artifacts.Instrumentation.Runner.artifactId)
}

dependencies {
  implementation(Libs.androidx_test_monitor)
  implementation(Libs.kotlin_stdlib)
  implementation(Libs.kotlin_reflect)
  implementation(Libs.junit)

  // This module's JUnit 5 dependencies cannot be present on the runtime classpath,
  // since that would prematurely raise the minSdkVersion requirement for target applications,
  // even though not all product flavors might want to use JUnit 5.
  // Therefore, only compile against those APIs, and have them provided at runtime
  // by the "instrumentation" companion library instead.
  compileOnly(Libs.junit_jupiter_api)
  compileOnly(Libs.junit_jupiter_params)
  compileOnly(Libs.junit_platform_runner)

  testImplementation(Libs.truth)
  testImplementation(Libs.mockito_core)
  testImplementation(Libs.junit_jupiter_api)
  testImplementation(Libs.junit_jupiter_params)
  testImplementation(Libs.junit_platform_runner)

  testRuntimeOnly(Libs.junit_jupiter_engine)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
//
// Releases are pushed to jcenter via Bintray, while snapshots are pushed to Sonatype OSS.
// This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Deployed> { Artifacts.Instrumentation.Runner }
apply(from = "$rootDir/gradle/deployment.gradle")
