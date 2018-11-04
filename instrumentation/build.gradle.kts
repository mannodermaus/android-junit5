import de.mannodermaus.gradle.plugins.junit5.Artifact
import de.mannodermaus.gradle.plugins.junit5.Artifacts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdkVersion(extra["android.compileSdkVersion"] as String)

  dexOptions {
    javaMaxHeapSize = extra["android.javaMaxHeapSize"] as String
  }

  defaultConfig {
    minSdkVersion(extra["android.instrumentationMinSdkVersion"] as Int)
    targetSdkVersion(extra["android.targetSdkVersion"] as Int)
    versionCode = 1
    versionName = "1.0"
    multiDexEnabled = true

    // Usually, this is automatically applied through the Gradle Plugin
    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArgument("runnerBuilder",
        "de.mannodermaus.junit5.AndroidJUnit5Builder")
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

val commonTestImplementation = configurations.create("commonTestImplementation")
configurations {
  getByName("androidTestImplementation").extendsFrom(commonTestImplementation)
  getByName("testImplementation").extendsFrom(commonTestImplementation)
}

dependencies {
  implementation(kotlin("stdlib", extra["versions.kotlin"] as String))
  implementation(extra["libs.androidTestRunner"] as String)
  implementation(extra["libs.junitJupiterApi"] as String)

  // This is required by the "instrumentation-runner" companion library,
  // since it can't provide any JUnit 5 runtime libraries itself
  // due to fear of prematurely incrementing the minSdkVersion requirement.
  runtimeOnly(extra["libs.junitPlatformRunner"] as String)

  commonTestImplementation(extra["libs.assertjCore"] as String)
  commonTestImplementation(extra["libs.mockito"] as String)
  commonTestImplementation(extra["libs.junitJupiterApi"] as String)
  commonTestImplementation(extra["libs.junitJupiterEngine"] as String)

  androidTestImplementation(extra["libs.assertjAndroid"] as String)
  androidTestImplementation(extra["libs.espresso"] as String)

  androidTestRuntimeOnly(project(":instrumentation-runner"))

  // Obviously, these dependencies should be mostly "runtimeOnly",
  // but we have to override bundled APIs from the IDE as much as possible for Android Studio.
  testImplementation(extra["libs.junitPlatformEngine"] as String)
  testImplementation(extra["libs.junitPlatformLauncher"] as String)
  testImplementation(extra["libs.junitJupiterApi"] as String)
  testImplementation(extra["libs.junitJupiterEngine"] as String)
  testImplementation(extra["libs.junitVintageEngine"] as String)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
//
// Releases are pushed to jcenter via Bintray, while snapshots are pushed to Sonatype OSS.
// This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Artifact> { Artifacts.Instrumentation.Library }
apply(from = "$rootDir/gradle/deployment.gradle")
