package de.mannodermaus.gradle.plugins.android_junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.jacoco.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.android_junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.android_junit5.util.TestProjectFactory
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/*
 * Base class for plugin-related unit testing
 * across different versions of the Android Gradle Plugin.
 * It is extended inside the respective sub-configurations for
 * Android Gradle Plugin 2 & 3, which then drive test execution
 * with their respective version.
 *
 * This class also provides hooks into the lifecycle of the JUnit 5 plugin,
 * as well as an Environment & Factory for the creation
 * of "mock projects" to test with.
 */

abstract class BasePluginSpec extends Specification {

  protected static final environment = new TestEnvironment()
  protected static final factory = new TestProjectFactory(environment)

  // Root Project containing required Android SDK reference in local.properties;
  // recreated for each executed test case
  private Project testRoot

  /* Before Each **/

  def setup() {
    testRoot = factory.newRootProject()
  }

  def cleanup() {
    FileUtils.deleteDirectory(testRoot.rootDir)
  }

  protected final Project rootProject() {
    return testRoot
  }

  /*
   * ================================================================================
   * Common Test Cases
   * ================================================================================
   */

  def "Requires Android Plugin"() {
    when:
    factory.newProject(rootProject())
        .applyJunit5Plugin()
        .build()

    then:
    def expect = thrown(PluginApplicationException)
    expect.cause.message == "An Android plugin must be applied to this project"
  }

  def "Requires Gradle 2.5 or later"() {
    // Below Gradle 2.8, TestKit's pluginClasspath() API
    // doesn't work with GradleRunner. Therefore, we have
    // to inject it ourselves inside the build.gradle script.
    //
    // Because of that, this test doesn't apply the default plugins
    // directly, and instead writes out the build file manually.
    //
    // More info:
    // https://docs.gradle.org/current/userguide/test_kit.html#sec:working_with_gradle_versions_prior_to_28
    when:
    def project = factory.newProject(rootProject())
        .asAndroidLibrary()
        .applyJunit5Plugin(false)
        .build()

    project.file("build.gradle").withWriter {
      it.write("""
          buildscript {
              dependencies {
                  classpath files($environment.pluginClasspathString)
              }
          }

          apply plugin: "de.mannodermaus.android-junit5"
  """)
    }

    def result = GradleRunner.create()
        .withGradleVersion("2.4")
        .withProjectDir(project.projectDir)
        .buildAndFail()

    then:
    result.output.contains("android-junit5 plugin requires Gradle version 2.5 or higher")
  }

  def "Dependency Handler Creation"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .buildAndEvaluate()

    then:
    def junit5 = project.dependencies.junit5
    def junit5Params = project.dependencies.junit5Params
    def junit5EmbeddedRuntime = project.dependencies.junit5EmbeddedRuntime

    assert junit5 != null
    assert junit5Params != null
    assert junit5EmbeddedRuntime != null
  }

  def "Overwrite Dependency Versions"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.junitPlatform {
      platformVersion = "1.3.3.7"
      jupiterVersion = "0.8.15"
      vintageVersion = "1.2.3"
    }

    project.evaluate()

    then:
    def ju5Deps = project.dependencies.junit5() as List<Dependency>
    assert ju5Deps.find { it.group == "org.junit.platform" && it.version == "1.3.3.7" } != null
    assert ju5Deps.find { it.group == "org.junit.jupiter" && it.version == "0.8.15" } != null
    assert ju5Deps.find { it.group == "org.junit.vintage" && it.version == "1.2.3" } != null

    def ju5ParamsDeps = project.dependencies.junit5Params() as List<Dependency>
    assert ju5ParamsDeps.find { it.group == "org.junit.jupiter" && it.version == "0.8.15" } != null
  }

  def "Application: Basic Integration"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      buildTypes {
        staging {}
      }
    }

    project.evaluate()

    then:
    // These statements automatically assert the existence of the tasks,
    // and raise an Exception if absent
    def runDebug = project.tasks.getByName("junitPlatformTestDebug")
    def runRelease = project.tasks.getByName("junitPlatformTestRelease")
    def runStaging = project.tasks.getByName("junitPlatformTestStaging")
    def runAll = project.tasks.getByName("junitPlatformTest")

    // Assert that dependency chain is valid
    assert runAll.getDependsOn().containsAll([runDebug, runRelease, runStaging])
  }

  @SuppressWarnings("GroovyPointlessBoolean")
  def "Application: Jacoco Integration"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyJacocoPlugin()
        .build()

    project.android {
      buildTypes {
        staging {}
      }

      project.junitPlatform {
        jacoco {
          xmlReport false
          htmlReport false
          csvReport true
        }
      }
    }

    project.evaluate()

    then:
    // These statements automatically assert the existence of the tasks,
    // and raise an Exception if absent
    def runDebug = project.tasks.getByName("jacocoTestReportDebug") as AndroidJUnit5JacocoReport
    def runRelease = project.tasks.getByName("jacocoTestReportRelease")
    def runStaging = project.tasks.getByName("jacocoTestReportStaging")
    def runAll = project.tasks.getByName("jacocoTestReport")

    // Assert that dependency chain is valid
    assert runAll.getDependsOn().containsAll([runDebug, runRelease, runStaging])

    // Assert report configuration parameters
    assert runDebug.reports.xml.enabled == false
    assert runDebug.reports.html.enabled == false
    assert runDebug.reports.csv.enabled == true
  }

  def "Application: Jacoco Tasks not added if plugin absent"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyJacocoPlugin(false)
        .buildAndEvaluate()

    then:
    project.tasks.findByName("jacocoTestReport") == null
    project.tasks.findByName("jacocoTestReportDebug") == null
    project.tasks.findByName("jacocoTestReportRelease") == null
  }

  def "Library: Basic Integration"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidLibrary()
        .applyJunit5Plugin()
        .build()

    project.android {
      buildTypes {
        staging {}
      }
    }

    project.evaluate()

    then:
    // These statements automatically assert the existence of the tasks,
    // and raise an Exception if absent
    def runDebug = project.tasks.getByName("junitPlatformTestDebug")
    def runRelease = project.tasks.getByName("junitPlatformTestRelease")
    def runStaging = project.tasks.getByName("junitPlatformTestStaging")
    def runAll = project.tasks.getByName("junitPlatformTest")

    // Assert that dependency chain is valid
    assert runAll.getDependsOn().containsAll([runDebug, runRelease, runStaging])
  }

  def "Library: Jacoco Integration"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidLibrary()
        .applyJunit5Plugin()
        .applyJacocoPlugin()
        .buildAndEvaluate()

    then:
    // These statements automatically assert the existence of the tasks,
    // and raise an Exception if absent
    def runDebug = project.tasks.getByName("jacocoTestReportDebug")
    def runRelease = project.tasks.getByName("jacocoTestReportRelease")
    def runAll = project.tasks.getByName("jacocoTestReport")

    // Assert that dependency chain is valid
    assert runAll.getDependsOn().containsAll([runDebug, runRelease])
  }

  def "Library: Jacoco Tasks not added if plugin absent"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidLibrary()
        .applyJunit5Plugin()
        .applyJacocoPlugin(false)
        .buildAndEvaluate()

    then:
    project.tasks.findByName("jacocoTestReport") == null
    project.tasks.findByName("jacocoTestReportDebug") == null
    project.tasks.findByName("jacocoTestReportRelease") == null
  }

  def "Application: Kotlin Integration"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyKotlinPlugin()
        .buildAndEvaluate()

    then:
    project.tasks.getByName("copyKotlinUnitTestClassesDebug")
    project.tasks.getByName("copyKotlinUnitTestClassesRelease")
  }

  def "Library: Kotlin Integration"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidLibrary()
        .applyJunit5Plugin()
        .applyKotlinPlugin()
        .buildAndEvaluate()

    then:
    project.tasks.getByName("copyKotlinUnitTestClassesDebug")
    project.tasks.getByName("copyKotlinUnitTestClassesRelease")
  }

  def "Application: Kotlin Tasks not added if plugin absent"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyKotlinPlugin(false)
        .buildAndEvaluate()

    then:
    project.tasks.findByName("copyKotlinUnitTestClassesDebug") == null
    project.tasks.findByName("copyKotlinUnitTestClassesRelease") == null
  }

  def "Library: Kotlin Tasks not added if plugin absent"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidLibrary()
        .applyJunit5Plugin()
        .applyKotlinPlugin(false)
        .buildAndEvaluate()

    then:
    project.tasks.findByName("copyKotlinUnitTestClassesDebug") == null
    project.tasks.findByName("copyKotlinUnitTestClassesRelease") == null
  }
}
