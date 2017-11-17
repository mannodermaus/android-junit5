package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/*
 * Model classes holding information about the transitive dependencies of the plugin,
 * exposed to consumers through its custom dependency handlers.
 */

class Versions(
    project: Project,
    jupiterVersion: String,
    platformVersion: String,
    vintageVersion: String,
    junit4Version: String,
    androidJunit5Version: String
) {

  val jupiter = Jupiter(project, jupiterVersion)
  val platform = Platform(project, platformVersion)
  val vintage = Vintage(project, vintageVersion)
  val android = Android(project, androidJunit5Version)
  val junit4: Dependency = project.dependencies.create("junit:junit:$junit4Version")
}

sealed class Base(
    private val project: Project,
    private val groupId: String,
    protected val version: String
) {
  protected fun dependency(artifactId: String): Dependency =
      project.dependencies.create("$groupId:$artifactId:$version")
}

class Jupiter(project: Project, version: String) : Base(project, "org.junit.jupiter", version) {
  val api = dependency("junit-jupiter-api")
  val engine = dependency("junit-jupiter-engine")
  val params = dependency("junit-jupiter-params")
}

class Platform(project: Project, version: String) : Base(project, "org.junit.jupiter", version) {
  val launcher = dependency("junit-platform-launcher")
  val console = dependency("junit-platform-console")
  val engine = dependency("junit-platform-engine")
}

class Vintage(project: Project, version: String) : Base(project, "org.junit.vintage", version) {
  val engine = dependency("junit-vintage-engine")
}

class Android(project: Project, version: String) :
    Base(project, "de.mannodermaus.gradle.plugins", version) {
  val embeddedRuntime = dependency("android-junit5-embedded-runtime")
}
