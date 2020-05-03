import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  id("groovy")
  id("kotlin")
  id("java-gradle-plugin")
  id("jacoco")
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
// Test Setup
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

// Setup environment & versions in test projects
tasks.named("processTestResources", Copy::class.java).configure {
  val tokens = mutableMapOf(
      "COMPILE_SDK_VERSION" to Android.compileSdkVersion,
      "BUILD_TOOLS_VERSION" to Android.buildToolsVersion,
      "MIN_SDK_VERSION" to Android.sampleMinSdkVersion.toString(),
      "TARGET_SDK_VERSION" to Android.targetSdkVersion.toString(),

      "KOTLIN" to Plugins.kotlin.version,

      "KOTLIN_STD_LIB" to Libs.kotlinStdLib,
      "JUPITER_API" to Libs.junitJupiterApi,
      "JUPITER_ENGINE" to Libs.junitJupiterEngine
  ).also { map ->
    // Add an entry for each of the supported Android Gradle Plugin values
    // (e.g. "3.5.3" -> "AGP_35X")
    Plugins.supportedAndroidPlugins.forEach { plugin ->
      map["AGP_${plugin.shortVersion}X"] = plugin.version
    }
  }

  inputs.properties(tokens)

  // Write a gradle.properties file into each test project
  file("src/test/projects")
      .listFiles()
      .filter { it.isDirectory }
      .forEach {
        val propertiesFile = File(it, "gradle.properties")
        if (propertiesFile.exists()) {
          propertiesFile.delete()
        }
        propertiesFile.writer().use { writer ->
          writer.write(tokens
              .map { t -> "${t.key}=${t.value}" }
              .joinToString(separator = System.lineSeparator()))
        }
      }

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
        this@create(plugin)
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
            .filter { it.isDirectory }
            .map { File(it, "main") }
            .filter { it.exists() && it.isDirectory && it.list().isNotEmpty() }
        val resourcesDirs = file("$buildDir/resources").listFiles()
            .filter { it.isDirectory }

        writer.write("implementation-classpath=")
        writer.write((classesDirs + resourcesDirs + configuration)
            .joinToString(separator = "\\:"))
      }
    }
  }
}

// Resource Writers
tasks.create("writePluginClasspath", WriteClasspathResource::class) {
  inputFiles = sourceSets["test"].runtimeClasspath
  outputDir = File("$buildDir/resources/test")
  resourceFileName = "plugin-classpath.txt"
}

val testTask = tasks.getByName("test")
val processTestResources = tasks.getByName("processTestResources")
tasks.withType<WriteClasspathResource> {
  processTestResources.finalizedBy(this)
  testTask.mustRunAfter(this)
}

// ------------------------------------------------------------------------------------------------
// Dependency Definitions
// ------------------------------------------------------------------------------------------------

dependencies {
  compileOnly(Plugins.android)
  implementation(gradleApi())
  implementation(Plugins.kotlin)
  implementation(Libs.kotlinStdLib)
  implementation(Libs.javaSemver)
  implementation(Libs.annimonStream)
  implementation(Libs.junitPlatformCommons)

  testImplementation(gradleTestKit())
  testImplementation(Plugins.android)
  testImplementation(Libs.commonsIO)
  testImplementation(Libs.commonsLang)
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

apply(from = "$rootDir/gradle/deployment.gradle")
