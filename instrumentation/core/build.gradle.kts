import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    google()
    jcenter()
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
  kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
  failFast = true
  testLogging {
    events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

dependencies {
  implementation(Libs.kotlinStdLib)
  implementation(Libs.junitJupiterApi)
  api(Libs.androidxTestCore)
  // This is required by the "instrumentation-runner" companion library,
  // since it can't provide any JUnit 5 runtime libraries itself
  // due to fear of prematurely incrementing the minSdkVersion requirement.
  runtimeOnly(Libs.junitPlatformRunner)
  runtimeOnly(Libs.junitJupiterEngine)

  androidTestImplementation(Libs.junitJupiterApi)
  androidTestImplementation(Libs.junitJupiterParams)
  androidTestImplementation(Libs.espressoCore)
  androidTestRuntimeOnly(project(":runner"))
  androidTestRuntimeOnly(Libs.junitJupiterEngine)

  testImplementation(Libs.junitJupiterApi)
  testImplementation(Libs.mockitoCore)
  testImplementation(Libs.mockitoKotlin)
  testImplementation(Libs.truth)
  testImplementation(Libs.truthJava8Extensions)
}

apply(from = "${rootDir.parentFile}/deployment.gradle")
