package de.mannodermaus.gradle.plugins.junit5.util

import java.io.File
import java.util.Properties

private const val ANDROID_SDK_FILE_NAME = "local.properties"
private const val ANDROID_SDK_PROP_NAME = "sdk.dir"
private const val ANDROID_HOME_ENVVAR_NAME = "ANDROID_HOME"

private const val ENVIRONMENT_RESOURCE_NAME =
    "/de/mannodermaus/gradle/plugins/junit5/testenv.properties"
private const val COMPILE_SDK_PROP_NAME = "COMPILE_SDK_VERSION"
private const val MIN_SDK_PROP_NAME = "MIN_SDK_VERSION"
private const val TARGET_SDK_PROP_NAME = "TARGET_SDK_VERSION"
private const val KOTLIN_VERSION_PROP_NAME = "KOTLIN_VERSION"
private const val JUNIT5_ANDROID_PROP_NAME = "JUNIT5_ANDROID_LIBS_VERSION"
private const val AGP_VERSIONS_PROP_NAME = "AGP_VERSIONS"
private const val JUNIT_VERSIONS_PROP_NAME = "JUNIT_VERSIONS"

private const val USER_DIR_PROP_NAME = "user.dir"
private const val BUILD_SRC_FOLDER_NAME = "buildSrc"

/**
 * Encapsulates environment properties related to running Unit Tests that interface with an Android
 * SDK installation.
 *
 * Typically, test cases don't need to instantiate this themselves, as its creation is hooked into
 * the lifecycle of the enclosing test class.
 */
class TestEnvironment {
    val androidSdkFolder = loadAndroidSdkFolder()
    val envProps: Properties = loadAndroidEnvironment()

    val compileSdkVersion: Int
    val minSdkVersion: Int
    val targetSdkVersion: Int

    val kotlinVersion: String
    val junit5AndroidLibsVersion: String

    val supportedAgpVersions: List<TestedAgp>
    val supportedJUnitVersions: List<TestedJUnit>

    init {
        compileSdkVersion = envProps.getProperty(COMPILE_SDK_PROP_NAME).toInt()
        minSdkVersion = envProps.getProperty(MIN_SDK_PROP_NAME).toInt()
        targetSdkVersion = envProps.getProperty(TARGET_SDK_PROP_NAME).toInt()
        kotlinVersion = envProps.getProperty(KOTLIN_VERSION_PROP_NAME)
        junit5AndroidLibsVersion = envProps.getProperty(JUNIT5_ANDROID_PROP_NAME)

        // Each entry in this string is separated by semicolon.
        // Within each entry, the pipe ("|") divides it into four properties
        val agpVersionsString = envProps.getProperty(AGP_VERSIONS_PROP_NAME)
        supportedAgpVersions =
            agpVersionsString
                .split(";")
                .map { entry -> entry.split("|") }
                .map { values ->
                    TestedAgp(
                        shortVersion = values[0],
                        version = values[1],
                        requiresGradle = values[2],
                        requiresCompileSdk = values[3].toIntOrNull(),
                    )
                }

        // Each entry in this string is separated by semicolon.
        val junitVersionsString = envProps.getProperty(JUNIT_VERSIONS_PROP_NAME)
        supportedJUnitVersions =
            junitVersionsString
                .split(";")
                .map { entry -> entry.split("|") }
                .map { values ->
                    TestedJUnit(majorVersion = values[0].toInt(), fullVersion = values[1])
                }
    }
}

/* Private functions */

private fun loadAndroidSdkFolder(): File {
    // Try local project first, fall back to Environment Variable, throw if nothing works
    return loadAndroidSdkFromProject()
        ?: loadAndroidSdkFromEnvVar()
        ?: throw AssertionError(
            "Android SDK couldn't be found. Either local.properties file in project root is missing, " +
                "it doesn't include the required 'sdk.dir' statement, " +
                "or there is no ANDROID_HOME environment variable!"
        )
}

private fun loadAndroidSdkFromProject(): File? {
    // Walk up the directory tree until the root is reached
    // (root is reached == "buildSrc" folder present).
    // If no local.properties is found until then, don't bother
    val userDir = System.getProperty(USER_DIR_PROP_NAME)

    var file: File? = File(userDir)
    while (file != null) {
        val localPropsFile = File(file, ANDROID_SDK_FILE_NAME)
        if (localPropsFile.exists()) {
            val sdkFolderProp =
                localPropsFile
                    .readLines()
                    .find { it.startsWith(ANDROID_SDK_PROP_NAME) }
                    ?.run { this.substring(this.indexOf('=') + 1).trim() }

            if (sdkFolderProp != null) {
                // Found match; abort
                return File(sdkFolderProp)
            }
        }

        // No match. Go up unless root directory has been reached
        val buildSrcFolder = File(file, BUILD_SRC_FOLDER_NAME)
        if (buildSrcFolder.exists() && buildSrcFolder.isDirectory) {
            return null
        }

        // Otherwise walk up to the parent file
        file = file.parentFile
    }

    return null
}

private fun loadAndroidSdkFromEnvVar() = System.getenv(ANDROID_HOME_ENVVAR_NAME)?.run { File(this) }

private fun loadAndroidEnvironment() =
    Properties().apply {
        TestEnvironment::class.java.getResourceAsStream(ENVIRONMENT_RESOURCE_NAME)!!.reader().use {
            this.load(it)
        }
    }
