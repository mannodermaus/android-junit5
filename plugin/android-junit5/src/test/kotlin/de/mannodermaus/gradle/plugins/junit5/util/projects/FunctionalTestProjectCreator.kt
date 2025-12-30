package de.mannodermaus.gradle.plugins.junit5.util.projects

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.toml
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestedAgp
import de.mannodermaus.gradle.plugins.junit5.util.TestedJUnit
import java.io.File
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.opentest4j.TestAbortedException

private const val TEST_PROJECTS_RESOURCE = "/test-projects"
private const val BUILD_GRADLE_TEMPLATE_NAME = "build.gradle.kts.template"
private const val SETTINGS_GRADLE_TEMPLATE_NAME = "settings.gradle.kts.template"
private const val OUTPUT_BUILD_GRADLE_NAME = "build.gradle.kts"
private const val OUTPUT_SETTINGS_GRADLE_NAME = "settings.gradle.kts"
private const val PROJECT_CONFIG_FILE_NAME = "config.toml"
private const val SRC_FOLDER_NAME = "src"

class FunctionalTestProjectCreator(
    private val outputFolder: File,
    private val environment: TestEnvironment,
) {

    private val projectRootFolder: File
    val allSpecs: List<Spec>

    init {
        // Obtain access to the root folder for all test projects
        val rootUrl = FunctionalTestProjectCreator::class.java.getResource(TEST_PROJECTS_RESOURCE)
        projectRootFolder = File(rootUrl.toURI())

        // Collect all eligible test folders
        allSpecs =
            projectRootFolder
                .listFiles()
                ?.filter { it.isDirectory }
                ?.mapNotNull { folder -> Spec.tryCreate(folder) } ?: emptyList()
    }

    fun specNamed(name: String): Spec =
        allSpecs.firstOrNull { it.name == name }
            ?: throw IllegalAccessException(
                "No test project named '$name' found in src/test/resources/test-projects"
            )

    @Throws(TestAbortedException::class)
    fun createProject(spec: Spec, agp: TestedAgp, junit: TestedJUnit): File {
        // Validate the spec requirement against the executing AGP version first
        validateSpec(spec, agp)

        // Construct the project folder, cleaning it if necessary.
        // If any Gradle or build caches already exist, we keep those around.
        // That's the reason for not doing "projectFolder.deleteRecursively()"
        // and nuking everything at once.
        val projectName = "${spec.name}_agp${agp.shortVersion}_junit${junit.majorVersion}"
        val projectFolder = File(outputFolder, projectName)
        if (projectFolder.exists()) {
            File(projectFolder, SRC_FOLDER_NAME).deleteRecursively()
            File(projectFolder, OUTPUT_BUILD_GRADLE_NAME).delete()
            File(projectFolder, OUTPUT_SETTINGS_GRADLE_NAME).delete()
        }
        projectFolder.mkdirs()

        // Set up static files
        File(projectFolder, "local.properties").bufferedWriter().use { file ->
            file.write("sdk.dir = ${environment.androidSdkFolder.absolutePath}")
        }
        File(projectFolder, "gradle.properties").bufferedWriter().use { file ->
            file.appendLine("android.useAndroidX = true")

            // From AGP 9, test components are only generated for the debug build type; disable this
            // behavior
            file.appendLine("android.onlyEnableUnitTestForTheTestedBuildType = false")
        }

        // Copy over the source folders
        val targetSrcFolder = File(projectFolder, "src").also { it.mkdir() }
        spec.srcFolder.copyRecursively(targetSrcFolder)

        // Construct the build script from its raw template, using the environment properties as
        // placeholders
        val replacements = environment.envProps.mapKeys { it.key.toString() }.toMutableMap()
        replacements["AGP_VERSION"] = agp.version
        replacements["USE_KOTLIN"] = spec.useKotlin
        replacements["USE_FLAVORS"] = spec.useFlavors
        replacements["USE_JACOCO"] = spec.useJacoco
        replacements["USE_CUSTOM_BUILD_TYPE"] = spec.useCustomBuildType
        replacements["RETURN_DEFAULT_VALUES"] = spec.returnDefaultValues
        replacements["INCLUDE_ANDROID_RESOURCES"] = spec.includeAndroidResources
        replacements["DISABLE_TESTS_FOR_BUILD_TYPES"] = spec.disableTestsForBuildTypes
        replacements["JUNIT_VERSION"] = junit.fullVersion

        agp.requiresCompileSdk?.let { replacements["OVERRIDE_SDK_VERSION"] = it }

        val processor =
            BuildScriptTemplateProcessor(
                folder = projectRootFolder,
                replacements = replacements,
                agpVersion = agp.version,
                gradleVersion = agp.requiresGradle,
                junitVersion = junit.fullVersion,
            )

        processor.process(BUILD_GRADLE_TEMPLATE_NAME).also { result ->
            File(projectFolder, OUTPUT_BUILD_GRADLE_NAME).writeText(result)
        }

        processor.process(SETTINGS_GRADLE_TEMPLATE_NAME).also { result ->
            File(projectFolder, OUTPUT_SETTINGS_GRADLE_NAME).writeText(result)
        }

        return projectFolder
    }

    private fun validateSpec(spec: Spec, agp: TestedAgp) {
        if (spec.minAgpVersion != null) {
            // If the spec dictates a minimum version of the AGP,
            // disable the test for plugin versions below that minimum requirement
            assumeTrue(
                SemanticVersion(agp.version) >= SemanticVersion(spec.minAgpVersion),
                "This project requires AGP ${spec.minAgpVersion} and was disabled on this version.",
            )
        }
    }

    /* Types */

    class Spec private constructor(val name: String, val srcFolder: File, config: Config) {
        val task = config[TomlSpec.Settings.task]
        val minAgpVersion = config[TomlSpec.Settings.minAgpVersion]
        val useKotlin = config[TomlSpec.Settings.useKotlin]
        val useJacoco = config[TomlSpec.Settings.useJacoco]
        val useFlavors = config[TomlSpec.Settings.useFlavors]
        val useCustomBuildType = config[TomlSpec.Settings.useCustomBuildType]
        val returnDefaultValues = config[TomlSpec.Settings.returnDefaultValues]
        val includeAndroidResources = config[TomlSpec.Settings.includeAndroidResources]
        val allowSkipped = config[TomlSpec.Settings.allowSkipped]
        val expectedTests = config[TomlSpec.expectations]

        val disableTestsForBuildTypes =
            config[TomlSpec.Settings.disableTestsForBuildTypes]?.split(",")?.map(String::trim)
                ?: emptyList()

        companion object {
            fun tryCreate(folder: File): Spec? {
                if (folder.isFile) {
                    return null
                }

                val srcFolder = File(folder, SRC_FOLDER_NAME)
                if (!srcFolder.exists()) {
                    return null
                }

                val configFile = File(folder, PROJECT_CONFIG_FILE_NAME)
                if (!configFile.exists()) {
                    return null
                }

                val config = Config { addSpec(TomlSpec) }.from.toml.file(configFile)
                return Spec(folder.name, srcFolder, config)
            }
        }
    }

    // Structure of the virtual project config file, used only internally
    private object TomlSpec : ConfigSpec(prefix = "") {
        val expectations by optional<List<ExpectedTests>>(default = emptyList())

        object Settings : ConfigSpec() {
            val task by optional<String?>(default = null)
            val minAgpVersion by optional<String?>(default = null)
            val useFlavors by optional(default = false)
            val useKotlin by optional(default = false)
            val useJacoco by optional(default = false)
            val allowSkipped by optional<Boolean>(default = false)
            val useCustomBuildType by optional<String?>(default = null)
            val returnDefaultValues by optional(default = false)
            val includeAndroidResources by optional(default = false)
            val disableTestsForBuildTypes by optional<String?>(default = null)
        }
    }

    /** Data holder for one set of expected tests within the virtual project */
    data class ExpectedTests(
        val buildType: String,
        val productFlavor: String?,
        private val tests: String,
    ) {
        val testsList = tests.split(",").map(String::trim)
    }
}
