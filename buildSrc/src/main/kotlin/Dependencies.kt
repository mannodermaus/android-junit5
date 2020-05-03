import kotlin.String

// Shared versions

private const val kotlinVersion = "1.3.61"

/**
 * Gradle plugins used throughout the repository.
 * Also, the list of supported AGP versions is maintained here.
 */
object Plugins {
  // Maintenance & Build Environment
  const val versions: Lib = "com.github.ben-manes:gradle-versions-plugin:0.20.0"
  const val kotlin: Lib = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

  // Android Gradle Plugin
  const val android35x: Agp = "com.android.tools.build:gradle:3.5.3"
  const val android36x: Agp = "com.android.tools.build:gradle:3.6.3"
  const val android40x: Agp = "com.android.tools.build:gradle:4.0.0-beta05"
  const val android41x: Agp = "com.android.tools.build:gradle:4.1.0-alpha08"
  const val android: Agp = android35x

  val supportedAndroidPlugins = listOf(
      android35x,
      android36x,
      android40x,
      android41x
  )

  // Documentation
  const val dokkaCore: Lib = "org.jetbrains.dokka:dokka-gradle-plugin:0.9.18"
  const val dokkaAndroid: Lib = "org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.18"

  // Publishing
  const val androidMavenGradle: Lib = "com.github.dcendents:android-maven-gradle-plugin:2.1"
  const val androidMavenPublish: Lib = "digital.wup:android-maven-publish:3.6.2"
  const val bintray: Lib = "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4"
}

/**
 * Third-party dependencies used by the modules in this repository.
 */
object Libs {
  // Environment & Helpers
  const val kotlinStdLib: Lib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
  const val javaSemver: Lib = "com.github.zafarkhaja:java-semver:0.9.0"
  const val annimonStream: Lib = "com.annimon:stream:1.2.1"
  const val commonsIO: Lib = "commons-io:commons-io:2.6"
  const val commonsLang: Lib = "commons-lang:commons-lang:2.6"

  // JUnit 5
  private const val junitJupiterVersion = "5.6.2"
  private const val junitPlatformVersion = "1.6.2"
  private const val junitVintageVersion = "5.6.2"
  const val junitJupiterApi: Lib = "org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion"
  const val junitJupiterEngine: Lib = "org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion"
  const val junitJupiterParams: Lib = "org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion"
  const val junitPlatformCommons: Lib = "org.junit.platform:junit-platform-commons:$junitPlatformVersion"
  const val junitPlatformEngine: Lib = "org.junit.platform:junit-platform-engine:$junitPlatformVersion"
  const val junitPlatformLauncher: Lib = "org.junit.platform:junit-platform-launcher:$junitPlatformVersion"
  const val junitPlatformRunner: Lib = "org.junit.platform:junit-platform-runner:$junitPlatformVersion"
  const val junitVintageEngine: Lib = "org.junit.vintage:junit-vintage-engine:$junitVintageVersion"
  
  // Assertions & Testing
  private const val truthVersion = "0.43"
  const val truth: Lib = "com.google.truth:truth:$truthVersion"
  const val truthJava8Extensions: Lib = "com.google.truth.extensions:truth-java8-extension:$truthVersion"
  const val truthAndroidExtensions: Lib = "androidx.test.ext:truth:1.1.0"

  const val mockitoCore: Lib = "org.mockito:mockito-core:2.19.0"
  const val mockitoKotlin: Lib = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"

  const val espressoCore: Lib = "androidx.test.espresso:espresso-core:3.2.0"
  const val androidxTestCore: Lib = "androidx.test:core:1.2.0"
  const val androidxTestMonitor: Lib = "androidx.test:monitor:1.2.0"
  const val androidxTestRunner: Lib = "androidx.test:runner:1.2.0"

  private const val spekVersion = "1.2.1"
  const val spekApi: Lib = "org.jetbrains.spek:spek-api:$spekVersion"
  const val spekEngine: Lib = "org.jetbrains.spek:spek-junit-platform-engine:$spekVersion"



//  const val aapt2: Lib = "com.android.tools.build:aapt2:" + Versions.aapt2
//
//  const val lint_gradle: Lib = "com.android.tools.lint:lint-gradle:" + Versions.lint_gradle
//
//
//  const val android_junit5: Lib = "de.mannodermaus.gradle.plugins:android-junit5:" + Versions.android_junit5
//
//  const val org_jacoco_agent: Lib = "org.jacoco:org.jacoco.agent:" + Versions.org_jacoco_agent
//  const val org_jacoco_ant: Lib = "org.jacoco:org.jacoco.ant:" + Versions.org_jacoco_ant
//
//  const val android_instrumentation_test_runner: Lib =
//      "de.mannodermaus.junit5:android-instrumentation-test-runner:" + Versions.de_mannodermaus_junit5
//
//  const val android_instrumentation_test: Lib =
//      "de.mannodermaus.junit5:android-instrumentation-test:" + Versions.de_mannodermaus_junit5
//
//  const val android_maven_publish: Lib = "digital.wup:android-maven-publish:" +
//      Versions.android_maven_publish
//
//  const val junit: Lib = "junit:junit:" + Versions.junit
//  const val assertj_core: Lib = "org.assertj:assertj-core:" + Versions.assertj_core
//  const val kotlin_compiler_embeddable: Lib =
//      "org.jetbrains.kotlin:kotlin-compiler-embeddable:" + Versions.org_jetbrains_kotlin
//  const val kotlin_gradle_plugin: Lib = "org.jetbrains.kotlin:kotlin-gradle-plugin:" +
//      Versions.org_jetbrains_kotlin
//  const val kotlin_reflect: Lib = "org.jetbrains.kotlin:kotlin-reflect:" +
//      Versions.org_jetbrains_kotlin
//  const val kotlin_scripting_compiler_embeddable: Lib =
//      "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:" +
//          Versions.org_jetbrains_kotlin
//  const val kotlin_stdlib_jdk8: Lib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:" +
//      Versions.org_jetbrains_kotlin
//  const val kotlin_stdlib: Lib = "org.jetbrains.kotlin:kotlin-stdlib:" +
//      Versions.org_jetbrains_kotlin
//
//  const val junit_pioneer: Lib = "org.junit-pioneer:junit-pioneer:" + Versions.junit_pioneer
}

/* Helpers & Extensions */

typealias Lib = String
val Lib.version get() = substringAfterLast(":")

typealias Agp = Lib
val Agp.shortVersion: String get() {
  // Extract first two components of the Maven dependency's version string.
  val components = substringAfterLast(":").split('.')
  if (components.size < 2) {
    throw IllegalArgumentException("Cannot derive AGP configuration name from: $this")
  }

  return "${components[0]}${components[1]}"
}
val Agp.configurationName: String get() {
  // Derive the Gradle configuration name from that
  // (Example: version = "3.2.0" --> configurationName = "testAgp32x")
  return "testAgp${shortVersion}x"
}
