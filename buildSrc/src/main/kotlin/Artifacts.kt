import Platform.Android
import Platform.Java
import org.gradle.api.Project
import java.io.File
import java.util.*

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
   * Gradle Plugin artifact
   */
  val Plugin = Deployed(
      platform = Java,
      groupId = "de.mannodermaus.gradle.plugins",
      artifactId = "android-junit5",
      currentVersion = "1.4.1.0-SNAPSHOT",
      latestStableVersion = "1.4.0.0",
      license = license,
      description = "Unit Testing with JUnit 5 for Android."
  )

  /**
   * Instrumentation Test artifacts
   */
  object Instrumentation {
    private val groupId = "de.mannodermaus.junit5"
    private val currentVersion = "1.0.0-SNAPSHOT"
    val latestStableVersion = "0.2.2"

    val Library = Deployed(
        platform = Android(minSdk = 26),
        groupId = groupId,
        artifactId = "android-instrumentation-test",
        currentVersion = "0.3.0-SNAPSHOT",
        latestStableVersion = "0.2.2",
        license = license,
        description = "(DEPRECATED) Extensions for instrumented Android tests with JUnit 5."
    )

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

class DeployCredentials(private val project: Project) {

  val bintrayUser: String?
  val bintrayKey: String?
  val sonatypeUser: String?
  val sonatypePass: String?

  init {
    // Populate deployment credentials in an environment-aware fashion.
    //
    // * Local development:
    //      Stored in local.properties file on the machine
    // * CI Server:
    //      Stored in environment variables before launch
    val properties = Properties().apply {
      val credentialsFile = File(project.rootDir, "local.properties")
      if (credentialsFile.exists()) {
        load(credentialsFile.inputStream())
      }
    }

    this.bintrayUser = properties.getProperty("BINTRAY_USER", System.getenv("bintrayUser"))
    this.bintrayKey = properties.getProperty("BINTRAY_KEY", System.getenv("bintrayKey"))
    this.sonatypeUser = properties.getProperty("SONATYPE_USER", System.getenv("sonatypeUser"))
    this.sonatypePass = properties.getProperty("SONATYPE_PASS", System.getenv("sonatypePass"))
  }
}
