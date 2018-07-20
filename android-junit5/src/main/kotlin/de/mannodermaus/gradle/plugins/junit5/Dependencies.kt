package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.ext
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Dependency
import java.util.Properties

/*
 * Model classes holding information about the transitive dependencies of the plugin,
 * exposed to consumers through the custom dependency handler.
 */

/* Types */

/**
 * Public-facing handler object, injected into the default DependencyHandler,
 * exposing the different available methods to consumers.
 */
@Suppress("MemberVisibilityCanPrivate")
class JUnit5DependencyHandler(
    private val project: Project,
    defaults: Properties) {

  private val versions: Versions by lazy {
    Versions(
        project = project,
        extension = project.android.testOptions.junitPlatform,
        defaults = defaults)
  }

  /* Public */

  /**
   * Retrieves the list of dependencies related to
   * running Unit Tests on the JUnit Platform with Android.
   */
  fun unitTests() = listOf(
      versions.others.junit4,
      versions.jupiter.api,
      versions.platform.engine,
      versions.jupiter.engine,
      versions.vintage.engine
  )

  /**
   * Retrieves the list of dependencies related to
   * writing Parameterized Tests.
   */
  fun parameterized() = listOf(
      versions.jupiter.params
  )

  /**
   * Retrieves the list of dependencies related to
   * running Instrumentation Tests on the JUnit Platform with Android.
   */
  fun instrumentationTests(): List<Dependency> {
    // Abort if JUnit 5 Instrumentation Tests aren't enabled,
    // since that would cause confusion otherwise.
    if (!project.android.testOptions.junitPlatform.instrumentationTests.enabled) {
      @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
      throw ProjectConfigurationException("""
        The JUnit 5 Instrumentation Test library can't be used
        if it is disabled via the plugin.
        Please revise the following in your build.gradle:
        android.testOptions {
          junitPlatform {
            instrumentationTests.enabled true
          }
        }
        """.trimIndent(), null)
    }

    return listOf(
        versions.others.instrumentationTest,

        // Provided to instrumentation-runner at runtime,
        // very important for the execution of JUnit 5 instrumentation tests.
        // Also refer to AndroidJUnit5Builder's documentation for
        // more info on how this is pieced together.
        versions.platform.runner
    )
  }

  /* Internal */

  internal fun configure() {
    // "dependencies.junit5" is the gateway to the sharded dependency groups
    project.dependencies.ext[DEP_HANDLER_NAME] = this
  }
}

/* Internal API */

/**
 * Internal data holder, serving as a gateway to the actual dependencies via its properties.
 */
class Versions(
    project: Project,
    extension: AndroidJUnitPlatformExtension,
    defaults: Properties
) {
  val jupiter = Jupiter(project, extension, defaults)
  val platform = Platform(project, extension, defaults)
  val vintage = Vintage(project, extension, defaults)
  val others = Other(project, extension, defaults)
}

abstract class BaseDependency(private val project: Project) {

  protected fun dependency(groupId: String, artifactId: String, version: String): Dependency =
      project.dependencies.create("$groupId:$artifactId:$version")
}

abstract class DependencyGroup(
    project: Project,
    private val groupId: String
) : BaseDependency(project) {

  protected abstract fun version(): String

  protected fun dependency(artifactId: String): Dependency =
      super.dependency(groupId, artifactId, version())
}

/**
 * Transitive Dependencies related to JUnit Jupiter
 */
class Jupiter(
    project: Project,
    private val extension: AndroidJUnitPlatformExtension,
    private val properties: Properties
) : DependencyGroup(project, "org.junit.jupiter") {

  override fun version(): String =
      extension.jupiterVersion ?: properties.getProperty(JUNIT_JUPITER_VERSION_PROP)

  val api = dependency("junit-jupiter-api")
  val engine = dependency("junit-jupiter-engine")
  val params = dependency("junit-jupiter-params")
}

/**
 * Transitive Dependencies related to the JUnit Platform
 */
class Platform(
    project: Project,
    private val extension: AndroidJUnitPlatformExtension,
    private val properties: Properties
) : DependencyGroup(project, "org.junit.platform") {

  override fun version(): String =
      extension.platformVersion ?: properties.getProperty(JUNIT_PLATFORM_VERSION_PROP)

  val launcher = dependency("junit-platform-launcher")
  val console = dependency("junit-platform-console")
  val engine = dependency("junit-platform-engine")
  val runner = dependency("junit-platform-runner")
}

/**
 * Transitive Dependencies related to JUnit Vintage
 */
class Vintage(
    project: Project,
    private val extension: AndroidJUnitPlatformExtension,
    private val properties: Properties
) : DependencyGroup(project, "org.junit.vintage") {

  override fun version(): String =
      extension.vintageVersion ?: properties.getProperty(JUNIT_VINTAGE_VERSION_PROP)

  val engine = dependency("junit-vintage-engine")
}

/**
 * Transitive Dependencies related to nothing in particular.
 * Dump for all useful dependencies that aren't closely tied
 * to the JUnit 5 namespace.
 */
class Other(
    project: Project,
    private val extension: AndroidJUnitPlatformExtension,
    properties: Properties
) : BaseDependency(project) {

  val junit4 = dependency(
      groupId = "junit",
      artifactId = "junit",
      version = properties.getProperty(JUNIT4_VERSION_PROP))

  val instrumentationTest by lazy {
    dependency(
        groupId = "de.mannodermaus.junit5",
        artifactId = "android-instrumentation-test",
        version = extension.instrumentationTests.version ?: properties.getProperty(
            INSTRUMENTATION_TEST_VERSION_PROP))
  }

  val instrumentationRunner by lazy {
    dependency(
        groupId = "de.mannodermaus.junit5",
        artifactId = "android-instrumentation-test-runner",
        version = extension.instrumentationTests.version ?: properties.getProperty(
            INSTRUMENTATION_TEST_VERSION_PROP))
  }
}
