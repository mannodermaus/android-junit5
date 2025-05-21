import Platform.Android
import Platform.Java
import org.gradle.api.Project
import java.io.File
import java.util.Properties

enum class SupportedAgp(
    val version: String,
    val gradle: String,
    val compileSdk: Int? = null
) {
    AGP_8_2("8.2.2", gradle = "8.2"),
    AGP_8_3("8.3.2", gradle = "8.4"),
    AGP_8_4("8.4.2", gradle = "8.6"),
    AGP_8_5("8.5.2", gradle = "8.7"),
    AGP_8_6("8.6.1", gradle = "8.7"),
    AGP_8_7("8.7.3", gradle = "8.9"),
    AGP_8_8("8.8.2", gradle = "8.10.2"),
    AGP_8_9("8.9.3", gradle = "8.11.1"),
    AGP_8_10("8.10.0", gradle = "8.11.1"),
    AGP_8_11("8.11.0-alpha10", gradle = "8.13"),
    AGP_8_12("8.12.0-alpha01", gradle = "8.13")
    ;

    companion object {
        val oldest = values().first()
        val newestStable = values().reversed().first { '-' !in it.version }
    }

    val shortVersion: String = run {
        // Extract first two components of the Maven dependency's version string.
        val components = version.split('.')
        if (components.size < 2) {
            throw IllegalArgumentException("Cannot derive AGP configuration name from: $this")
        }

        "${components[0]}.${components[1]}"
    }

    val configurationName = "testAgp${shortVersion.replace(".", "")}x"
}

object Android {
    const val compileSdkVersion = 35
    const val targetSdkVersion = 35
    const val sampleMinSdkVersion = 21
    val testRunnerMinSdkVersion = (Artifacts.Instrumentation.Runner.platform as Android).minSdk
    val testCoreMinSdkVersion = (Artifacts.Instrumentation.Core.platform as Android).minSdk
    val testComposeMinSdkVersion = (Artifacts.Instrumentation.Compose.platform as Android).minSdk
}

sealed class Platform(val name: String) {
    object Java : Platform("java")
    class Android(val minSdk: Int) : Platform("android")
}

/**
 * Encapsulation for "deployable" library artifacts,
 * containing all sorts of configuration related to Maven coordinates, for instance.
 */
class Deployed internal constructor(
    val platform: Platform,
    val groupId: String,
    val artifactId: String,
    val currentVersion: String,
    val latestStableVersion: String,
    val description: String,
)

object Artifacts {
    const val GITHUB_URL = "https://github.com/mannodermaus/android-junit5"
    const val GITHUB_REPO = "mannodermaus/android-junit5"
    const val LICENSE = "Apache-2.0"

    /**
     * Retrieve the artifact configuration based on a Gradle project reference.
     * Return null if none can be found
     */
    fun from(project: Project) =
        when (project.name) {
            "core" -> Instrumentation.Core
            "runner" -> Instrumentation.Runner
            "android-junit5" -> Plugin
            else -> null
        }

    /**
     * Gradle Plugin artifact
     */
    val Plugin = Deployed(
        platform = Java,
        groupId = "de.mannodermaus.gradle.plugins",
        artifactId = "android-junit5",
        currentVersion = "1.13.0.0-SNAPSHOT",
        latestStableVersion = "1.12.2.0",
        description = "Unit Testing with JUnit 5 for Android."
    )

    /**
     * Instrumentation Test artifacts
     */
    object Instrumentation {
        const val groupId = "de.mannodermaus.junit5"
        private const val currentVersion = "1.8.0-SNAPSHOT"
        private const val latestStableVersion = "1.7.0"

        val Core = Deployed(
            platform = Android(minSdk = 19),
            groupId = groupId,
            artifactId = "android-test-core",
            currentVersion = currentVersion,
            latestStableVersion = latestStableVersion,
            description = "Extensions for instrumented Android tests with JUnit 5."
        )

        val Extensions = Deployed(
            platform = Android(minSdk = 19),
            groupId = groupId,
            artifactId = "android-test-extensions",
            currentVersion = currentVersion,
            latestStableVersion = latestStableVersion,
            description = "Optional extensions for instrumented Android tests with JUnit 5."
        )

        val Runner = Deployed(
            platform = Android(minSdk = 19),
            groupId = groupId,
            artifactId = "android-test-runner",
            currentVersion = currentVersion,
            latestStableVersion = latestStableVersion,
            description = "Runner for integration of instrumented Android tests with JUnit 5."
        )

        val Compose = Deployed(
            platform = Android(minSdk = 21),
            groupId = groupId,
            artifactId = "android-test-compose",
            currentVersion = currentVersion,
            latestStableVersion = latestStableVersion,
            description = "Extensions for Jetpack Compose tests with JUnit 5."
        )
    }
}

class DeployedCredentials(private val project: Project) {

    var signingKeyId: String?
    var signingPassword: String?
    var signingKeyRingFile: String?
    var ossrhUsername: String?
    var ossrhPassword: String?
    var sonatypeStagingProfileId: String?

    init {
        // Populate deployment credentials in an environment-aware fashion.
        //
        // * Local development:
        //      Stored in local.properties file on the machine
        //      (in the root folder of the project – the one containing "plugin/" and "instrumentation/")
        // * CI Server:
        //      Stored in environment variables before launch
        val properties = Properties().apply {
            val credentialsFile = File(project.rootDir.parentFile, "local.properties")
            if (credentialsFile.exists()) {
                load(credentialsFile.inputStream())
            }
        }

        this.signingKeyId = properties.getOrEnvvar("SIGNING_KEY_ID")
        this.signingPassword = properties.getOrEnvvar("SIGNING_PASSWORD")
        this.signingKeyRingFile = properties.getOrEnvvar("SIGNING_KEY_RING_FILE")
        this.ossrhUsername = properties.getOrEnvvar("OSSRH_USERNAME")
        this.ossrhPassword = properties.getOrEnvvar("OSSRH_PASSWORD")
        this.sonatypeStagingProfileId = properties.getOrEnvvar("SONATYPE_STAGING_PROFILE_ID")
    }

    private fun Properties.getOrEnvvar(key: String): String? =
        getProperty(key, System.getenv(key))
}
