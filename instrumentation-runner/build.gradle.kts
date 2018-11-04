import de.mannodermaus.gradle.plugins.junit5.Artifact
import de.mannodermaus.gradle.plugins.junit5.Artifacts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
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
  id("com.android.library")
  kotlin("android")
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
    minSdkVersion(extra["android.runnerMinSdkVersion"] as Int)
    targetSdkVersion(extra["android.targetSdkVersion"] as Int)
    versionCode = 1
    versionName = "1.0"
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

configurations.all {
  // The Instrumentation Test Runner uses the plugin,
  // which in turn provides the Instrumentation Test Runner again -
  // that's kind of deep.
  // To avoid conflicts, prefer using the local classes
  // and exclude the dependency from being pulled in externally.
  exclude(module = extra["android-junit5.instrumentation.runner.artifactId"] as String)
}

dependencies {
  implementation(kotlin("stdlib", extra["versions.kotlin"] as String))
  implementation(kotlin("reflect", extra["versions.kotlin"] as String))
  implementation(extra["libs.junit4"] as String)

  // This module's JUnit 5 dependencies cannot be present on the runtime classpath,
  // since that would prematurely raise the minSdkVersion requirement for target applications,
  // even though not all product flavors might want to use JUnit 5.
  // Therefore, only compile against those APIs, and have them provided at runtime
  // by the "instrumentation" companion library instead.
  compileOnly(extra["libs.junitJupiterApi"] as String)
  compileOnly(extra["libs.junitJupiterParams"] as String)
  compileOnly(extra["libs.junitPlatformRunner"] as String)

  testImplementation(extra["libs.assertjCore"] as String)
  testImplementation(extra["libs.mockito"] as String)
  testImplementation(extra["libs.junitJupiterApi"] as String)
  testImplementation(extra["libs.junitJupiterParams"] as String)
  testImplementation(extra["libs.junitPlatformRunner"] as String)

  testRuntimeOnly(extra["libs.junitJupiterEngine"] as String)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
//
// Releases are pushed to jcenter via Bintray, while snapshots are pushed to Sonatype OSS.
// This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Artifact> { Artifacts.Instrumentation.Runner }
apply(from = "$rootDir/gradle/deployment.gradle")
