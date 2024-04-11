import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("groovy")
    id("kotlin")
    id("java-gradle-plugin")
    id("jacoco")
//  id("com.github.johnrengelman.shadow")
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
val javaVersion = JavaVersion.VERSION_11.toString()
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion
}
tasks.withType<JavaCompile> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

project.fixCompileTaskChain()

kotlin {
    explicitApi()
}

// ------------------------------------------------------------------------------------------------
// Plugin Resource Setup
//
// This block generates the required resource files
// containing the identifiers with which the plugin can be applied to consumer projects.
// ------------------------------------------------------------------------------------------------
val pluginClassName = "de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformPlugin"

gradlePlugin {
    plugins {
        create("plugin") {
            id = "de.mannodermaus.android-junit5"
            implementationClass = pluginClassName
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Task Setup
// ------------------------------------------------------------------------------------------------

// Allow building fat JARs if necessary
//tasks.withType<ShadowJar> {
//  isZip64 = true
//  enabled = project.hasProperty("enableFatJar")
//  archiveAppendix.set("fat")
//}

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
project.configureTestResources()

// Generate a file with the latest versions of the plugin & instrumentation
val genFolder = "build/generated/sources/plugin"
val versionClassTask = tasks.register<Copy>("createVersionClass") {
    from("src/main/templates/Libraries.kt")
    into("$genFolder/de/mannodermaus")
    filter(
        mapOf(
            "tokens" to mapOf(
                "INSTRUMENTATION_GROUP" to Artifacts.Instrumentation.groupId,
                "INSTRUMENTATION_CORE" to Artifacts.Instrumentation.Core.artifactId,
                "INSTRUMENTATION_EXTENSIONS" to Artifacts.Instrumentation.Extensions.artifactId,
                "INSTRUMENTATION_RUNNER" to Artifacts.Instrumentation.Runner.artifactId,
                "INSTRUMENTATION_VERSION" to Artifacts.Instrumentation.latestStableVersion,
            )
        ), ReplaceTokens::class.java
    )
    outputs.upToDateWhen { false }
}
sourceSets {
    main {
        kotlin.srcDir(genFolder)
    }
}
tasks.withType<DokkaTask> {
    // Connect additional source folder to Dokka generation task
    dependsOn(versionClassTask)
}

// ------------------------------------------------------------------------------------------------
// Dependency Definitions
// ------------------------------------------------------------------------------------------------

dependencies {
    compileOnly(libs.plugins.android(SupportedAgp.oldest))
    compileOnly(libs.androidTools)
    compileOnly(libs.plugins.kotlin)

    implementation(gradleApi())
    implementation(libs.kotlinStdLib)
    implementation(libs.javaSemver)
    implementation(libs.junitPlatformCommons)

    testImplementation(gradleTestKit())
    testImplementation(libs.plugins.android(SupportedAgp.oldest))
    testImplementation(libs.korte)
    testImplementation(libs.konfToml)
    testImplementation(libs.truth)
    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterParams)
    testRuntimeOnly(libs.junitJupiterEngine)
}

project.configureDeployment(Artifacts.Plugin)

// Register source-processing tasks as dependants of the custom source generation task
listOf("compileKotlin", "androidSourcesJar", "dokkaJavadoc").forEach { taskName ->
    tasks.named(taskName).configure {
        dependsOn(versionClassTask)
    }
}
