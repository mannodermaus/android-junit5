import kotlin.String

// Shared versions

private const val kotlinVersion = "1.3.72"

/**
 * Gradle plugins used throughout the repository.
 * Also, the list of supported AGP versions is maintained here.
 */
object Plugins {
  // Maintenance & Build Environment
  const val versions: Lib = "com.github.ben-manes:gradle-versions-plugin:0.20.0"
  const val kotlin: Lib = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

  // Android Gradle Plugin
  val android35x: Agp = Agp("com.android.tools.build:gradle:3.5.4")
  val android36x: Agp = Agp("com.android.tools.build:gradle:3.6.4")
  val android40x: Agp = Agp("com.android.tools.build:gradle:4.0.1")
  val android41x: Agp = Agp("com.android.tools.build:gradle:4.1.0-beta04", requiresGradle = "6.5")
  val android42x: Agp = Agp("com.android.tools.build:gradle:4.2.0-alpha09", requiresGradle = "6.5")
  val android: Agp = android35x

  val supportedAndroidPlugins = listOf(
      android35x,
      android36x,
      android40x,
      android41x,
      android42x
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
  const val kotlinReflect: Lib = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
  const val javaSemver: Lib = "com.github.zafarkhaja:java-semver:0.9.0"
  const val annimonStream: Lib = "com.annimon:stream:1.2.1"
  const val commonsIO: Lib = "commons-io:commons-io:2.6"
  const val commonsLang: Lib = "commons-lang:commons-lang:2.6"
  const val konfToml: Lib = "com.uchuhimo:konf-toml:0.22.1"

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

  const val junit4: Lib = "junit:junit:4.13"
}

/* Helpers & Extensions */

typealias Lib = String

val Lib.version get() = substringAfterLast(":")

class Agp(val dependency: Lib, val requiresGradle: String? = null) {

  val version = dependency.version

  val shortVersion: String = run {
    // Extract first two components of the Maven dependency's version string.
    val components = dependency.substringAfterLast(":").split('.')
    if (components.size < 2) {
      throw IllegalArgumentException("Cannot derive AGP configuration name from: $this")
    }

    "${components[0]}.${components[1]}"
  }

  // Derive the Gradle configuration name from that
  // (Example: version = "3.2.0" --> configurationName = "testAgp32x")
  val configurationName = "testAgp${shortVersion.replace(".", "")}x"

  override fun toString() = dependency
}
