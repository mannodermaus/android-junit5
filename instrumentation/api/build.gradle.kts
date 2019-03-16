import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdkVersion(Android.compileSdkVersion)

  dexOptions {
    javaMaxHeapSize = Android.javaMaxHeapSize
  }

  defaultConfig {
    minSdkVersion(Android.instrumentationMinSdkVersion)
    targetSdkVersion(Android.targetSdkVersion)
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
  implementation(Libs.kotlin_stdlib)
  implementation(Libs.com_android_support_test_runner)
  implementation(Libs.junit_jupiter_api)

  // This is required by the "instrumentation-runner" companion library,
  // since it can't provide any JUnit 5 runtime libraries itself
  // due to fear of prematurely incrementing the minSdkVersion requirement.
  runtimeOnly(Libs.junit_platform_runner)

  commonTestImplementation(Libs.assertj_core)
  commonTestImplementation(Libs.mockito_core)
  commonTestImplementation(Libs.junit_jupiter_api)
  commonTestImplementation(Libs.junit_jupiter_engine)

  androidTestImplementation(Libs.assertj_android)
  androidTestImplementation(Libs.espresso_core)

  androidTestRuntimeOnly(project(":runner"))

  // Obviously, these dependencies should be mostly "runtimeOnly",
  // but we have to override bundled APIs from the IDE as much as possible for Android Studio.
  testImplementation(Libs.junit_platform_engine)
  testImplementation(Libs.junit_platform_launcher)
  testImplementation(Libs.junit_jupiter_api)
  testImplementation(Libs.junit_jupiter_engine)
  testImplementation(Libs.junit_vintage_engine)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
//
// Releases are pushed to jcenter via Bintray, while snapshots are pushed to Sonatype OSS.
// This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Deployed> { Artifacts.Instrumentation.Library }
apply(from = "$rootDir/gradle/deployment.gradle")
