import Platform.Android
import Platform.Java
import org.gradle.api.Project
import java.io.File
import java.util.*

enum class SupportedAgp(
        val version: String,
        val gradle: String? = null
) {
    AGP_4_0("4.0.2", gradle = "6.1.1"),
    AGP_4_1("4.1.2", gradle = "6.5"),
    AGP_4_2("4.2.2", gradle = "6.7.1"),
    AGP_7_0("7.0.2", gradle = "7.0.2"),
    AGP_7_1("7.1.0-alpha12", gradle = "7.2");

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
    const val compileSdkVersion = "android-28"
    const val javaMaxHeapSize = "3g"

    const val targetSdkVersion = 28
    const val sampleMinSdkVersion = 14
    val testRunnerMinSdkVersion = (Artifacts.Instrumentation.Runner.platform as Platform.Android).minSdk
    val testCoreMinSdkVersion = (Artifacts.Instrumentation.Core.platform as Platform.Android).minSdk
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
        val license: String
)

object Artifacts {
    val githubUrl = "https://github.com/mannodermaus/android-junit5"
    val githubRepo = "mannodermaus/android-junit5"
    val license = "Apache-2.0"

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
            currentVersion = "1.8.0.0-SNAPSHOT",
            latestStableVersion = "1.7.1.1",
            license = license,
            description = "Unit Testing with JUnit 5 for Android."
    )

    /**
     * Instrumentation Test artifacts
     */
    object Instrumentation {
        private val groupId = "de.mannodermaus.junit5"
        private val currentVersion = "1.3.0-SNAPSHOT"
        val latestStableVersion = "1.2.2"

        val Core = Deployed(
                platform = Android(minSdk = 14),
                groupId = groupId,
                artifactId = "android-test-core",
                currentVersion = currentVersion,
                latestStableVersion = latestStableVersion,
                license = license,
                description = "Extensions for instrumented Android tests with JUnit 5."
        )

        val Runner = Deployed(
                platform = Android(minSdk = 14),
                groupId = groupId,
                artifactId = "android-test-runner",
                currentVersion = currentVersion,
                latestStableVersion = latestStableVersion,
                license = license,
                description = "Runner for integration of instrumented Android tests with JUnit 5."
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
