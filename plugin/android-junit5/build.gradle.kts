import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.google.common.io.Files
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
  testLogging {
    events = setOf(TestLogEvent.STARTED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

// Setup environment & versions in test projects
tasks.named("processTestResources", Copy::class.java).configure {
  val tokens = mapOf(
      "COMPILE_SDK_VERSION" to Android.compileSdkVersion,
      "BUILD_TOOLS_VERSION" to Android.buildToolsVersion,
      "MIN_SDK_VERSION" to Android.sampleMinSdkVersion.toString(),
      "TARGET_SDK_VERSION" to Android.targetSdkVersion.toString(),

      "AGP_32X" to Versions.com_android_tools_build_gradle_32x,
      "AGP_33X" to Versions.com_android_tools_build_gradle_33x,
      "AGP_34X" to Versions.com_android_tools_build_gradle_34x,
      "AGP_35X" to Versions.com_android_tools_build_gradle_35x,
      "KOTLIN" to Versions.org_jetbrains_kotlin,

      "JUPITER_API" to Libs.junit_jupiter_api,
      "JUPITER_ENGINE" to Libs.junit_jupiter_engine
  )

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

// Add custom dependency configurations
configurations {
  create("testAgp32x") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.2.X"
    extendsFrom(configurations.getByName("implementation"))
    resolutionStrategy.force(Libs.com_android_tools_build_gradle_32x)
  }
  create("testAgp33x") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.3.X"
    extendsFrom(configurations.getByName("implementation"))
    resolutionStrategy.force(Libs.com_android_tools_build_gradle_33x)
  }
  create("testAgp34x") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.4.X"
    extendsFrom(configurations.getByName("implementation"))
    resolutionStrategy.force(Libs.com_android_tools_build_gradle_34x)
  }
  create("testAgp35x") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.5.X"
    extendsFrom(configurations.getByName("implementation"))
    resolutionStrategy.force(Libs.com_android_tools_build_gradle_35x)
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
  implementation(gradleApi())
  implementation(Libs.com_android_tools_build_gradle)
  implementation(Libs.kotlin_gradle_plugin)
  implementation(Libs.kotlin_stdlib_jdk8)
  implementation(Libs.java_semver)
  implementation(Libs.stream)
  implementation(Libs.junit_platform_commons)

  testImplementation(gradleTestKit())
  testImplementation(Libs.commons_io)
  testImplementation(Libs.commons_lang)
  testImplementation(Libs.mockito_core)
  testImplementation(Libs.truth) {
    // Incompatibility with AGP pulling in older version
    exclude(group = "com.google.guava", module = "guava")
  }

  testImplementation(Libs.spek_api)
  testRuntimeOnly(Libs.spek_junit_platform_engine)

  testImplementation(Libs.junit_jupiter_api)
  testImplementation(Libs.junit_jupiter_params)
  testRuntimeOnly(Libs.junit_jupiter_engine)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Deployed> { Artifacts.Plugin }
apply(from = "$rootDir/gradle/deployment.gradle")
