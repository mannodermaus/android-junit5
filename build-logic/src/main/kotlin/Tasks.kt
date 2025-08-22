import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.ZonedDateTime
import java.util.Locale

private const val minimumGradleVersion = "8.2"

@Suppress("DEPRECATION")
fun Project.configureTestResources() {
    // Create a test resource task which will power the instrumented tests
    // for different versions of the Android Gradle Plugin
    tasks.named("processTestResources", Copy::class.java).configure {
        val tokens = mapOf(
            "COMPILE_SDK_VERSION" to Android.compileSdkVersion.toString(),
            "MIN_SDK_VERSION" to Android.sampleMinSdkVersion.toString(),
            "TARGET_SDK_VERSION" to Android.targetSdkVersion.toString(),

            "KOTLIN_VERSION" to libs.versions.kotlin,
            "JUNIT_JUPITER_VERSION" to libs.versions.junitJupiter,
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

                val agpDependency = libs.plugins.android(plugin).substringBeforeLast(":")
                project.dependencies.add(this.name, "${agpDependency}:${plugin.version}")

                // For Android Gradle Plugins before 9.x, add the Kotlin Gradle Plugin explicitly,
                // acknowledging the different plugin variants introduced in Kotlin 1.7.
                // Acknowledging the minimum required Gradle version, request the correct variant for KGP
                // (see https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html#plugin-with-variants)
                if (plugin < SupportedAgp.AGP_9_0) {
                    project.dependencies.add(
                        this.name,
                        "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin}"
                    ).apply {
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
    }

    // Create slim plugin classpath for functional tests, using multiple flavors
    tasks.named("pluginUnderTestMetadata").configure {
        val defaultDirectory = outputs.files.singleFile

        configurations.filter { it.name.startsWith("testAgp") }.forEach { configuration ->
            val strippedName = configuration.name.substring(4).toLowerCase(Locale.ROOT)
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
}

fun findInstrumentationVersion(
    pluginVersion: String = Artifacts.Plugin.currentVersion,
    currentInstrumentationVersion: String = Artifacts.Instrumentation.Core.currentVersion,
    stableInstrumentationVersion: String = Artifacts.Instrumentation.Core.latestStableVersion
): String {
    return when {
        pluginVersion.endsWith("-SNAPSHOT") -> currentInstrumentationVersion
        currentInstrumentationVersion.endsWith("-SNAPSHOT") -> stableInstrumentationVersion
        else -> currentInstrumentationVersion
    }
}

fun Copy.configureCreateVersionClassTask(
    instrumentationVersion: String = findInstrumentationVersion(),
    fromPath: String = "src/main/templates/Libraries.kt",
    intoPath: String = "build/generated/sources/plugin/de/mannodermaus",
) {
    from(fromPath)
    into(intoPath)
    filter(
        mapOf(
            "tokens" to mapOf(
                "INSTRUMENTATION_GROUP" to Artifacts.Instrumentation.groupId,
                "INSTRUMENTATION_COMPOSE" to Artifacts.Instrumentation.Compose.artifactId,
                "INSTRUMENTATION_CORE" to Artifacts.Instrumentation.Core.artifactId,
                "INSTRUMENTATION_EXTENSIONS" to Artifacts.Instrumentation.Extensions.artifactId,
                "INSTRUMENTATION_RUNNER" to Artifacts.Instrumentation.Runner.artifactId,

                // Find an appropriate version of the instrumentation library,
                // depending on the version of how the plugin is configured
                "INSTRUMENTATION_VERSION" to instrumentationVersion,

                // JUnit 5.12+ requires the platform launcher on the runtime classpath;
                // to prevent issues with version mismatching, the plugin applies this for users
                "JUNIT_PLATFORM_LAUNCHER" to libs.junitPlatformLauncher
            )
        ), ReplaceTokens::class.java
    )
    outputs.upToDateWhen { false }
}

/**
 * Helper Task class for generating an up-to-date version of the project's README.md.
 * Using a template file, the plugin's version constants & other dependency versions
 * are automatically injected into the README.
 */
abstract class GenerateReadme : DefaultTask() {
    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\\$\\{(.+)}")
        private val EXTERNAL_DEP_REGEX = Regex("libs\\.(.+)")
        private val CONSTANT_REGEX = Regex("constants\\.(.+)")

        private const val PLUGIN_VERSION = "pluginVersion"
        private const val INSTRUMENTATION_VERSION = "instrumentationVersion"

        private const val CONSTANTS_FILE =
            "android-junit5/src/main/kotlin/de/mannodermaus/gradle/plugins/junit5/internal/config/Constants.kt"
        private val CONSTANTS_FILE_REGEX1 = Regex("""val (.*)\s*=\s*.+"(.*)".+""")
        private val CONSTANTS_FILE_REGEX2 = Regex("""val (.*)\s*=\s*AndroidPluginVersion\((\d+)\s*,\s*(\d+)\)""")
        private val CONSTANT_MAPPINGS = mapOf(
            "minimumRequiredGradleVersion" to "MIN_REQUIRED_GRADLE_VERSION",
            "minimumRequiredAgpVersion" to "MIN_REQUIRED_AGP_VERSION",
            "currentYear" to "CURRENT_YEAR",
        )

        private val GENERATED_HEADER_COMMENT = """
      <!--
        This file was automatically generated by Gradle. Do not modify.
        To update the content of this README, please apply modifications
        to `README.md.template` instead, and run the `generateReadme` task from Gradle.
      -->
      
    """.trimIndent()
    }

    @get:InputFile
    abstract val inputTemplateFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun doWork() {
        val templateText = inputTemplateFile.asFile.get().readText()
        val constants = parseConstantsFile()
        val replacedText = replacePlaceholdersInTemplate(templateText, constants)

        outputFile.asFile.get().writeText(replacedText)
    }

    /* Private */

    private fun replacePlaceholdersInTemplate(templateText: String, constants: Map<String, String>): String {
        // Apply placeholders in the template with data from Versions.kt & Environment.kt:
        // ${pluginVersion}             Artifacts.Plugin.currentVersion
        // ${instrumentationVersion}    Artifacts.Instrumentation.Core.currentVersion
        // ${Libs.<xxx>}                (A constant value taken from Dependencies.kt)
        val allPlaceholders = mutableMapOf<String, String>()

        PLACEHOLDER_REGEX.findAll(templateText).forEach { match ->
            val placeholder = match.groups.last()?.value
                ?: throw InvalidPlaceholder(match)

            // Local versions (plugin, instrumentation)
            val replacement = when (placeholder) {
                PLUGIN_VERSION -> Artifacts.Plugin.anyStableVersion
                INSTRUMENTATION_VERSION -> Artifacts.Instrumentation.Core.anyStableVersion
                else -> {
                    val match2 = CONSTANT_REGEX.find(placeholder)
                    if (match2 != null) {
                        val key = match2.groups.last()?.value
                        val constantKey1 = CONSTANT_MAPPINGS[key]
                        println("Constant key. placeholder=$placeholder, match2=$match2, constantKey1=$constantKey1, constants=$constants")
                        val constantKey = CONSTANT_MAPPINGS[key] ?: throw InvalidPlaceholder(match2)
                        constants[constantKey] ?: throw InvalidPlaceholder(match2)

                    } else {
                        val match3 = EXTERNAL_DEP_REGEX.find(placeholder)
                            ?: throw InvalidPlaceholder(match)
                        val externalDependency = match3.groups.last()?.value
                            ?: throw InvalidPlaceholder(match3)

                        val field = libs.javaClass.getField(externalDependency)
                        field.get(null) as String
                    }
                }
            }

            // Save placeholder
            allPlaceholders["\${$placeholder}"] = replacement
        }

        var replacedText = GENERATED_HEADER_COMMENT + templateText
        allPlaceholders.forEach { (key, value) ->
            replacedText = replacedText.replace(key, value)
        }
        return replacedText
    }

    private fun parseConstantsFile(): Map<String, String> {
        val constants = mutableMapOf<String, String>()
        val text = File(CONSTANTS_FILE).readText()

        // Add hardcoded constants
        constants["CURRENT_YEAR"] = ZonedDateTime.now().year.toString()

        CONSTANTS_FILE_REGEX1.findAll(text).forEach { match ->
            constants[match[1]] = match[2]
        }

        // Special case for AGP version
        CONSTANTS_FILE_REGEX2.findAll(text).forEach { match ->
            constants[match[1]] = match.groupValues
                .drop(2)
                .joinToString(separator = ".", transform = String::trim)
        }

        return constants
    }

    private operator fun MatchResult.get(index: Int): String {
        return this.groups[index]!!.value.trim()
    }
}

private class InvalidPlaceholder(matchResult: MatchResult) :
    Exception("Invalid match result: '${matchResult.groupValues}'")

private val Deployed.anyStableVersion: String
    get() = if (currentVersion.endsWith("-SNAPSHOT")) {
        latestStableVersion
    } else {
        currentVersion
    }
