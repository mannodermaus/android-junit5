import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("groovy")
  id("kotlin")
  id("java-gradle-plugin")
  id("jacoco")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  jvmTarget = "1.8"
}

// ------------------------------------------------------------------------------------------------
// Compilation Tweaks
//
// The plugin currently consists of a codebase wherein Groovy & Kotlin coexist.
// Therefore, the compilation chain has to be well-defined to allow Kotlin
// to call into Groovy code.
//
// The other way around ("call Kotlin from Groovy") is prohibited explicitly.
// ------------------------------------------------------------------------------------------------
project.fixCompileTaskChain()

// ------------------------------------------------------------------------------------------------
// Plugin Resource Setup
//
// This block generates the required resource files
// containing the identifiers with which the plugin can be applied to consumer projects.
// ------------------------------------------------------------------------------------------------

val pluginClassName = "de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformPlugin"

gradlePlugin {
  isAutomatedPublishing = false
  plugins {
    register("shortIdentifier") {
      id = "android-junit5"
      implementationClass = pluginClassName
    }
    register("longIdentifier") {
      id = "de.mannodermaus.android-junit5"
      implementationClass = pluginClassName
    }
  }
}

// ------------------------------------------------------------------------------------------------
// Task Setup
// ------------------------------------------------------------------------------------------------

// Use JUnit 5
tasks.withType<Test> {
  useJUnitPlatform()
  failFast = true
  testLogging {
    events = setOf(TestLogEvent.STARTED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

// Setup environment & versions for test projects
tasks.named("processTestResources", Copy::class.java).configure {
  val tokens = mapOf(
      "COMPILE_SDK_VERSION" to Android.compileSdkVersion,
      "MIN_SDK_VERSION" to Android.sampleMinSdkVersion.toString(),
      "TARGET_SDK_VERSION" to Android.targetSdkVersion.toString(),

      "KOTLIN_VERSION" to Plugins.kotlin.version,
      "JUNIT_JUPITER_VERSION" to Libs.junitJupiterApi.version,
      "JUNIT5_ANDROID_LIBS_VERSION" to Artifacts.Instrumentation.latestStableVersion,

      // Collect all supported AGP versions into a single string.
      // This string is delimited with semicolons, and each of the separated values itself is a 3-tuple.
      //
      // Example:
      // AGP_VERSIONS = 3.5|3.5.3|;3.6|3.6.3|6.4
      //
      // Can be parsed into this list of values:
      // |___> Short: "3.5"
      //       Full: "3.5.3"
      //       Gradle Requirement: null
      //
      // |___> Short: "3.6"
      //       Full: "3.6.3"
      //       Gradle Requirement: "6.4"
      "AGP_VERSIONS" to Plugins.supportedAndroidPlugins.joinToString(separator=";") { plugin ->
        "${plugin.shortVersion}|${plugin.version}|${plugin.requiresGradle ?: ""}"
      }
  )

  inputs.properties(tokens)

  // Apply test environment to a resource file
  from(sourceSets["test"].resources.srcDirs) {
    include("**/testenv.properties")
    filter(ReplaceTokens::class, mapOf("tokens" to tokens))
  }
}

configurations {
  // Create a custom configuration for each version
  Plugins.supportedAndroidPlugins.forEach { plugin ->
    create(plugin.configurationName) {
      description = "Local dependencies used for compiling & running " +
          "tests source code in Gradle functional tests against AGP ${plugin.version}"
      extendsFrom(configurations.getByName("implementation"))
      dependencies {
        this@create(plugin.dependency)
      }
    }
  }
}

// Create slim plugin classpath for functional tests,
// using multiple flavors
tasks.named("pluginUnderTestMetadata").configure {
  val defaultDirectory = outputs.files.singleFile

  configurations.filter { it.name.startsWith("testAgp") }.forEach { configuration ->
    val strippedName = configuration.name.substring(4).toLowerCase()
    val prunedFile = File(defaultDirectory, "pruned-plugin-metadata-$strippedName.properties")
    outputs.file(prunedFile)

    doLast {
      prunedFile.writer().use { writer ->
        // 1) Use output classes from the plugin itself
        // 2) Use resources from the plugin (i.e. plugin IDs etc.)
        // 3) Use AGP-specific dependencies
        val classesDirs = file("$buildDir/classes").listFiles()
            ?.filter { it.isDirectory }
            ?.map { File(it, "main") }
            ?.filter { it.exists() && it.isDirectory && it.list()?.isEmpty() == false }
            ?: emptyList()
        val resourcesDirs = file("$buildDir/resources").listFiles()
            ?.filter { it.isDirectory }
            ?: emptyList()

        writer.write("implementation-classpath=")
        writer.write((classesDirs + resourcesDirs + configuration)
            .joinToString(separator = "\\:"))
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// Dependency Definitions
// ------------------------------------------------------------------------------------------------

dependencies {
  compileOnly(Plugins.android.dependency)
  implementation(gradleApi())
  implementation(Plugins.kotlin)
  implementation(Libs.kotlinStdLib)
  implementation(Libs.javaSemver)
  implementation(Libs.annimonStream)
  implementation(Libs.junitPlatformCommons)

  testImplementation(gradleTestKit())
  testImplementation(Plugins.android.dependency)
  testImplementation(Libs.korte)
  testImplementation(Libs.commonsIO)
  testImplementation(Libs.commonsLang)
  testImplementation(Libs.konfToml)
  testImplementation(Libs.mockitoCore)
  testImplementation(Libs.truth) {
    // Incompatibility with AGP pulling in older version
    exclude(group = "com.google.guava", module = "guava")
  }
  testImplementation(Libs.junitJupiterApi)
  testImplementation(Libs.junitJupiterParams)
  testRuntimeOnly(Libs.junitJupiterEngine)

  testImplementation(Libs.spekApi)
  testRuntimeOnly(Libs.spekEngine)
}

apply(from = "${rootDir.parentFile}/deployment.gradle")
