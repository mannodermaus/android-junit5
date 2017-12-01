package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import de.mannodermaus.gradle.plugins.junit5.util.TaskUtils
import org.gradle.api.Project

/*
 * Unit testing the integration of JUnit 5
 * with the Android Gradle Plugin version 2.
 */

class AGP2PluginSpec extends BasePluginSpec {

  def "Application: Custom Product Flavors"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      productFlavors {
        free {}
        paid {}
      }
    }

    project.evaluate()

    then:
    // These statements automatically assert the existence of the tasks,
    // and raise an Exception if absent
    def expectedVariants = ["freeDebug", "paidDebug", "freeRelease", "paidRelease"]

    // Assert that dependency chain is valid
    def expectedVariantTasks = expectedVariants
        .collect { project.tasks.getByName("junitPlatformTest${it.capitalize()}") }
        .collect { it as AndroidJUnit5UnitTest }
    def runAllTask = project.tasks.getByName("junitPlatformTest")
    expectedVariantTasks.each { assert runAllTask.getDependsOn().contains(it) }

    // Assert that report directories are correctly to individual folders
    def uniqueReportDirs = expectedVariantTasks
        .collect { TaskUtils.argument(it, "--reports-dir") }
        .unique()

    assert expectedVariantTasks.size() == uniqueReportDirs.size()
  }

  def "Application: Jacoco Integration with Product Flavors"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyJacocoPlugin()
        .build()

    project.android {
      productFlavors {
        free {}
        paid {}
      }
    }

    project.evaluate()

    then:
    // These statements automatically assert the existence of the tasks,
    // and raise an Exception if absent
    def expectedVariants = ["freeDebug", "paidDebug", "freeRelease", "paidRelease"]

    // Assert that dependency chain is valid
    def expectedVariantTasks = expectedVariants
        .collect { project.tasks.getByName("jacocoTestReport${it.capitalize()}") }
        .collect { it as AndroidJUnit5JacocoReport }
    def runAllTask = project.tasks.getByName("jacocoTestReport")
    expectedVariantTasks.each { assert runAllTask.getDependsOn().contains(it) }
  }
}
