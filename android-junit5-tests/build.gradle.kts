import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  id("groovy")
  id("java-library")
  id("idea")
  id("jacoco")
  id("kotlin")
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
val compileTestGroovy = tasks.getByName("compileTestGroovy") as AbstractCompile
val compileTestKotlin = tasks.getByName("compileTestKotlin") as AbstractCompile
val testClassesTask = tasks.getByName("testClasses")

compileTestKotlin.dependsOn.remove("compileTestJava")
compileTestGroovy.dependsOn.add(compileTestKotlin)
compileTestGroovy.classpath += project.files(compileTestKotlin.destinationDir)

// Add custom dependency configurations
configurations {
  create("functionalTest") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests"
  }

  create("functionalTestAgp32X") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.2.X"
  }

  create("functionalTestAgp33X") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.3.X"
  }

  create("functionalTestAgp34X") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests against AGP 3.4.X"
  }
}

val processTestResources = tasks.getByName("processTestResources") as Copy
processTestResources.apply {
  val tokens = mapOf(
      "COMPILE_SDK_VERSION" to Android.compileSdkVersion,
      "BUILD_TOOLS_VERSION" to Android.buildToolsVersion,
      "MIN_SDK_VERSION" to Android.sampleMinSdkVersion.toString(),
      "TARGET_SDK_VERSION" to Android.targetSdkVersion.toString()
  )

  inputs.properties(tokens)

  from(sourceSets["test"].resources.srcDirs) {
    include("**/testenv.properties")
    filter(ReplaceTokens::class, mapOf("tokens" to tokens))
  }
}

tasks.withType<Test> {
  failFast = true
  useJUnitPlatform()
  testLogging {
    events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

dependencies {
  testImplementation(project(":android-junit5"))
  testImplementation(gradleTestKit())
  testImplementation(Libs.kotlin_gradle_plugin)
  testImplementation(Libs.com_android_tools_build_gradle)
  testImplementation(Libs.commons_io)
  testImplementation(Libs.commons_lang)
  testImplementation(Libs.junit)
  testImplementation(Libs.junit_jupiter_api)
  testImplementation(Libs.junit_jupiter_params)
  testImplementation(Libs.spek_api)
  testImplementation(Libs.junit_pioneer)
  testImplementation(Libs.assertj_core)
  testImplementation(Libs.mockito_core)

  testRuntimeOnly(Libs.junit_jupiter_engine)
  testRuntimeOnly(Libs.junit_vintage_engine)
  testRuntimeOnly(Libs.spek_junit_platform_engine)

  // Compilation of local classpath for functional tests
  val functionalTest by configurations
  functionalTest(Libs.kotlin_compiler_embeddable)
  functionalTest(Libs.junit)
  functionalTest(Libs.junit_jupiter_api)
  functionalTest(Libs.junit_jupiter_engine)

  val functionalTestAgp32X by configurations
  functionalTestAgp32X("com.android.tools.build:gradle:3.2.1")

  val functionalTestAgp33X by configurations
  functionalTestAgp33X("com.android.tools.build:gradle:3.3.0-rc03")

  val functionalTestAgp34X by configurations
  functionalTestAgp34X("com.android.tools.build:gradle:3.4.0-alpha09")
}

// Resource Writers
tasks.create("writePluginClasspath", WriteClasspathResource::class) {
  inputFiles = sourceSets["test"].runtimeClasspath
  outputDir = File("$buildDir/resources/test")
  resourceFileName = "plugin-classpath.txt"
}

// Create a classpath-generating task for all functional test configurations
listOf("functionalTest", "functionalTestAgp32X", "functionalTestAgp33X", "functionalTestAgp34X").forEach { config ->
  tasks.create("write${config.capitalize()}CompileClasspath", WriteClasspathResource::class) {
    inputFiles = configurations[config]
    outputDir = File("$buildDir/resources/test")
    resourceFileName = "$config-compile-classpath.txt"
  }
}

val testTask = tasks.getByName("test")
tasks.withType<WriteClasspathResource> {
  processTestResources.finalizedBy(this)
  testTask.mustRunAfter(this)
}
