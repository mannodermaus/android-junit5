import extensions.getWithVersion
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE
import java.util.Locale

plugins {
    id("groovy")
    id("kotlin")
    id("java-gradle-plugin")
    id("jacoco")
//  id("com.github.johnrengelman.shadow")
}

val minimumGradleVersion = "8.2"

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

kotlin {
    explicitApi()
}

// ------------------------------------------------------------------------------------------------
// Plugin Resource Setup
//
// This block generates the required resource files
// containing the identifiers with which the plugin can be applied to consumer projects.
// ------------------------------------------------------------------------------------------------
gradlePlugin {
    plugins {
        register("plugin") {
            id = "de.mannodermaus.android-junit5"
            implementationClass = "de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformPlugin"
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Dependency Definitions
// ------------------------------------------------------------------------------------------------

dependencies {
    // Compile our plugin with the oldest supported AGP version
    // and its tools library, whose version is derived from it
    val agpVersion = SupportedAgp.oldest.version
    val toolsVersion = agpVersion.split('.').run {
        "${23 + first().toInt()}." + drop(1).joinToString(".")
    }

    compileOnly(libs.agp.getWithVersion(agpVersion))
    compileOnly(libs.android.tools.getWithVersion(toolsVersion))
    compileOnly(libs.kgp)
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.stdlib)

    testImplementation(gradleTestKit())
    testImplementation(libs.agp.getWithVersion(agpVersion))
    testImplementation(libs.korte)
    testImplementation(libs.konftoml)
    testImplementation(libs.truth.core)
    testImplementation(platform(libs.junit.framework.bom6))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
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

// Generate a file with the latest versions of the plugin & instrumentation
val genFolder = "build/generated/sources/plugin"
val versionClassTask = tasks.register<Copy>(
    "createVersionClass",
    Copy::configureCreateVersionClassTask,
)

sourceSets {
    main {
        kotlin.srcDir(genFolder)
    }
}

// Setup environment & versions for test projects

// Create a test resource task which will power the instrumented tests
// for different versions of the Android Gradle Plugin
tasks.named<Copy>("processTestResources").configure {
    val tokens = mapOf(
        "COMPILE_SDK_VERSION" to Android.compileSdkVersion.toString(),
        "MIN_SDK_VERSION" to Android.sampleMinSdkVersion.toString(),
        "TARGET_SDK_VERSION" to Android.targetSdkVersion.toString(),

        "KOTLIN_VERSION" to libs.versions.kotlin.get(),
        "JUNIT5_ANDROID_LIBS_VERSION" to Artifacts.Instrumentation.Core.latestStableVersion,

        // Collect all supported AGP versions into a single string.
        // This string is delimited with semicolons, and each of the separated values itself is a 4-tuple.
        //
        // Example:
        // AGP_VERSIONS = 3.5|3.5.3|;3.6|3.6.3|6.4;3.7|3.7.0|8.0|33
        //
        // Can be parsed into this list of values:
        // |___> Short: "3.5"
        //       Full: "3.5.3"
        //       Gradle Requirement: ""
        //       Compile SDK: null
        //
        // |___> Short: "3.6"
        //       Full: "3.6.3"
        //       Gradle Requirement: "6.4"
        //       Compile SDK: null
        //
        // |___> Short: "3.7"
        //       Full: "3.7.0"
        //       Gradle Requirement: "8.0"
        //       Compile SDK: 33
        "AGP_VERSIONS" to SupportedAgp.values().joinToString(separator = ";") { plugin ->
            "${plugin.shortVersion}|${plugin.version}|${plugin.gradle}|${plugin.compileSdk ?: ""}"
        },

        // Collect all supported JUnit versions into a single string.
        // This string is delimited with semicolons, and each of the separated values is a 2-tuple.
        // Example:
        // JUNIT_VERSIONS = 5|5.14.1;6|6.0.1
        "JUNIT_VERSIONS" to SupportedJUnit.values().joinToString(separator = ";") { junit ->
            val fullVersion = when (junit) {
                SupportedJUnit.JUnit5 -> libs.versions.junit5
                SupportedJUnit.JUnit6 -> libs.versions.junit6
            }
            "${junit.majorVersion}|${fullVersion.get()}"
        }
    )

    inputs.properties(tokens)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    // Apply test environment to a resource file
    val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
    from(sourceSets.getByName("test").resources.srcDirs) {
        include("**/testenv.properties")
        filter(mapOf("tokens" to tokens), ReplaceTokens::class.java)
    }
}

// Also, create a custom configuration for each of the supported Android Gradle Plugin versions
project.configurations.apply {
    SupportedAgp.values().forEach { plugin ->
        create(plugin.configurationName) {
            description = "Local dependencies used for compiling & running " +
                    "tests source code in Gradle functional tests against AGP ${plugin.version}"
            extendsFrom(configurations.getByName("implementation"))
            project.dependencies.add(
                /* configurationName = */ this.name,
                /* dependencyNotation = */libs.agp.getWithVersion(plugin.version)
            )


            // For Android Gradle Plugins before 9.x, add the Kotlin Gradle Plugin explicitly,
            // acknowledging the different plugin variants introduced in Kotlin 1.7.
            // Acknowledging the minimum required Gradle version, request the correct variant for KGP
            // (see https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html#plugin-with-variants)
            if (plugin < SupportedAgp.AGP_9_0) {
                project.dependencies.add(this.name, libs.kgp.get()).apply {
                    with(this as ExternalModuleDependency) {
                        attributes {
                            attribute(
                                TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                                objects.named(TargetJvmEnvironment::class.java, STANDARD_JVM)
                            )
                            attribute(
                                USAGE_ATTRIBUTE,
                                objects.named(Usage::class.java, JAVA_RUNTIME)
                            )
                            attribute(
                                GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                                objects.named(GradlePluginApiVersion::class.java, minimumGradleVersion)
                            )
                        }
                    }
                }
            }
        }
    }

    // Attach Gradle Plugin API attribute to the created plugin artifacts
    listOf("runtimeElements", "apiElements").forEach { configurationName ->
        this.named(configurationName).configure {
            attributes {
                attribute(GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(minimumGradleVersion))
            }
        }
    }
}

// Create slim plugin classpath for functional tests, using multiple flavors
tasks.named("pluginUnderTestMetadata").configure {
    val defaultDirectory = outputs.files.singleFile

    configurations.filter { it.name.startsWith("testAgp") }.forEach { configuration ->
        val strippedName = configuration.name.substring(4).lowercase(Locale.ROOT)
        val prunedFile = File(defaultDirectory, "pruned-plugin-metadata-$strippedName.properties")
        outputs.file(prunedFile)

        doLast {
            prunedFile.writer().use { writer ->
                // 1) Use output classes from the plugin itself
                // 2) Use resources from the plugin (i.e. plugin IDs etc.)
                // 3) Use AGP-specific dependencies
                val classesDirs = layout.buildDirectory.dir("classes").get().asFile.listFiles()
                    ?.filter { it.isDirectory }
                    ?.map { File(it, "main") }
                    ?.filter { it.exists() && it.isDirectory && it.list()?.isEmpty() == false }
                    ?: emptyList()
                val resourcesDirs = layout.buildDirectory.dir("resources").get().asFile.listFiles()
                    ?.filter { it.isDirectory }
                    ?: emptyList()

                writer.write("implementation-classpath=")
                writer.write(
                    (classesDirs + resourcesDirs + configuration)
                        .joinToString(separator = "\\:")
                )
            }
        }
    }
}

project.configureDeployment(Artifacts.Plugin)

// Register source-processing tasks as dependants of the custom source generation task
listOf("compileKotlin", "sourcesJar", "dokkaGeneratePublicationHtml").forEach { taskName ->
    tasks.named(taskName).configure {
        dependsOn(versionClassTask)
    }
}
