import de.mannodermaus.gradle.plugins.junit5.Artifact
import de.mannodermaus.gradle.plugins.junit5.Artifacts
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
val compileGroovy = tasks.getByName("compileGroovy") as AbstractCompile
val compileKotlin = tasks.getByName("compileKotlin") as AbstractCompile
val classesTask = tasks.getByName("classes")

compileGroovy.dependsOn.remove("compileJava")
compileKotlin.dependsOn.add(compileGroovy)
compileKotlin.classpath += project.files(compileGroovy.destinationDir)
classesTask.dependsOn.add(compileKotlin)

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
// Dependency Definitions
// ------------------------------------------------------------------------------------------------

dependencies {
  compileOnly(kotlin("gradle-plugin", extra["versions.kotlin"] as String))
  implementation(kotlin("stdlib-jdk8", extra["versions.kotlin"] as String))
  implementation(gradleApi())
  implementation(extra["libs.javaSemver"] as String)
  implementation(extra["libs.annimonStream"] as String)
  implementation(extra["libs.junitPlatformCommons"] as String)
  implementation(extra["plugins.android"] as String)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Artifact> { Artifacts.Plugin }
apply(from = "$rootDir/gradle/deployment.gradle.kts")
