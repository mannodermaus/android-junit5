package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.util.Properties

/*
 * Model classes holding information about the transitive dependencies of the plugin,
 * exposed to consumers through its custom dependency handlers.
 */

/**
 * Data holder, serving as a gateway to the actual dependencies via its properties.
 */
class Versions(
    project: Project,
    extension: AndroidJUnitPlatformExtension,
    defaults: Properties
) {

  val jupiter = Jupiter(project, extension, defaults)
  val platform = Platform(project, extension, defaults)
  val vintage = Vintage(project, extension, defaults)
  val others = Other(project, defaults)
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
      extension.jupiterVersion ?: properties.getProperty(Constants.JUNIT_JUPITER_VERSION_PROP)

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
      extension.platformVersion ?: properties.getProperty(Constants.JUNIT_PLATFORM_VERSION_PROP)

  val launcher = dependency("junit-platform-launcher")
  val console = dependency("junit-platform-console")
  val engine = dependency("junit-platform-engine")
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
      extension.vintageVersion ?: properties.getProperty(Constants.JUNIT_VINTAGE_VERSION_PROP)

  val engine = dependency("junit-vintage-engine")
}

/**
 * Transitive Dependencies related to
 */
class Other(
    project: Project,
    properties: Properties
) : BaseDependency(project) {

  val embeddedRuntime = dependency(
      groupId = "de.mannodermaus.gradle.plugins",
      artifactId = "android-junit5-embedded-runtime",
      version = properties.getProperty(Constants.ANDROID_JUNIT5_VERSION_PROP))

  val junit4 = dependency(
      groupId = "junit",
      artifactId = "junit",
      version = properties.getProperty(Constants.JUNIT4_VERSION_PROP))
}
