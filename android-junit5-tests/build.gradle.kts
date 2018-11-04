import de.mannodermaus.gradle.plugins.junit5.WriteClasspathResource
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  id("groovy")
  id("java-gradle-plugin")
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

compileTestGroovy.dependsOn.remove("compileTestJava")
compileTestKotlin.dependsOn.add(compileTestGroovy)
compileTestKotlin.classpath += project.files(compileTestGroovy.destinationDir)
testClassesTask.dependsOn.add(compileTestKotlin)

// Add custom dependency configurations
configurations {
  create("functionalTest") {
    description = "Local dependencies used for compiling & running " +
        "tests source code in Gradle functional tests"
  }
}

val processTestResources = tasks.getByName("processTestResources") as Copy
processTestResources.apply {
  val tokens = mapOf(
      "COMPILE_SDK_VERSION" to project.extra["android.compileSdkVersion"] as String,
      "BUILD_TOOLS_VERSION" to project.extra["android.buildToolsVersion"] as String,
      "MIN_SDK_VERSION" to (project.extra["android.sampleMinSdkVersion"] as Int).toString(),
      "TARGET_SDK_VERSION" to (project.extra["android.targetSdkVersion"] as Int).toString()
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

  // Enable this line to run disable running Functional Tests on the local device
//  environment("CI", "true")
}

dependencies {
  testImplementation(project(":android-junit5"))
  testImplementation(kotlin("gradle-plugin", extra["versions.kotlin"] as String))
  testImplementation(extra["plugins.android"] as String)
  testImplementation(extra["libs.commonsIO"] as String)
  testImplementation(extra["libs.commonsLang"] as String)
  testImplementation(extra["libs.junit4"] as String)
  testImplementation(extra["libs.junitJupiterApi"] as String)
  testImplementation(extra["libs.junitJupiterParams"] as String)
  testImplementation(extra["libs.spekApi"] as String)
  testImplementation(extra["libs.junitPioneer"] as String)
  testImplementation(extra["libs.assertjCore"] as String)
  testImplementation(extra["libs.mockito"] as String)

  testRuntimeOnly(extra["libs.junitJupiterEngine"] as String)
  testRuntimeOnly(extra["libs.spekEngine"] as String)

  // Compilation of local classpath for functional tests
  val functionalTest by configurations
  functionalTest(kotlin("compiler-embeddable", extra["versions.kotlin"] as String))
  functionalTest(extra["libs.junit4"] as String)
  functionalTest(extra["plugins.android"] as String)
  functionalTest(extra["libs.junitJupiterApi"] as String)
  functionalTest(extra["libs.junitJupiterEngine"] as String)
}

// Resource Writers
tasks.create("writePluginClasspath", WriteClasspathResource::class) {
  inputFiles = sourceSets["test"].runtimeClasspath
  outputDir = File("$buildDir/resources/test")
  resourceFileName = "plugin-classpath.txt"
}

tasks.create("writeFunctionalTestCompileClasspath", WriteClasspathResource::class) {
  inputFiles = configurations["functionalTest"]
  outputDir = File("$buildDir/resources/test")
  resourceFileName = "functional-test-compile-classpath.txt"
}

val testTask = tasks.getByName("test")
tasks.withType<WriteClasspathResource> {
  processTestResources.finalizedBy(this)
  testTask.mustRunAfter(this)
}
