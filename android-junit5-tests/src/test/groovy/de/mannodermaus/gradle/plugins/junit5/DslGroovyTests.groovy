package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.JUnit5Task
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory
import kotlin.io.FilesKt
import org.gradle.api.Project
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class DslGroovyTests {

  private TestProjectFactory factory
  private Project testRoot

  @BeforeEach
  void beforeEach() {
    def environment = new TestEnvironment()

    this.factory = new TestProjectFactory(environment)
    this.testRoot = factory.newRootProject()
  }

  @AfterEach
  void afterEach() {
    FilesKt.deleteRecursively(this.testRoot.rootDir)
  }

  @Test
  @DisplayName("dynamic filters methods can be called on existing build types")
  void dynamicFiltersMethodsCanBeCalledOnExistingBuildTypes() {
    def project = factory.newProject(testRoot)
        .asAndroidApplication()
        .build()

    project.android.testOptions.junitPlatform {
      filters {
        tags {
          include "some-tag"
          exclude "other-tag"
        }
      }
      debugFilters {
        tags {
          include "debug-tag"
        }
      }
    }

    project.evaluate()

    def debugTask = project.tasks.getByName("junitPlatformTestDebug") as JUnit5Task
    assertThat(debugTask.tagIncludes).containsOnly("some-tag", "debug-tag")
    assertThat(debugTask.tagExcludes).containsOnly("other-tag")

    def releaseTask = project.tasks.getByName("junitPlatformTestRelease") as JUnit5Task
    assertThat(releaseTask.tagIncludes).containsOnly("some-tag")
    assertThat(releaseTask.tagExcludes).containsOnly("other-tag")
  }

  @Test
  @DisplayName("dynamic filters methods can be called on existing product flavors")
  void dynamicFiltersMethodsCanBeCalledOnExistingProductFlavors() {
    def project = factory.newProject(testRoot)
        .asAndroidApplication()
        .build()

    project.android {
      flavorDimensions "tier"
      productFlavors {
        free {
          dimension "tier"
        }
        paid {
          dimension "tier"
        }
      }

      testOptions.junitPlatform {
        filters {
          tags {
            include "some-tag"
            exclude "other-tag"
          }
        }
        freeDebugFilters {
          tags {
            include "free-debug-tag"
          }
        }
        paidFilters {
          tags {
            include "paid-tag"
          }
        }
        releaseFilters {
          tags {
            include "release-tag"
          }
        }
      }
    }

    project.evaluate()

    def freeDebugTask = project.tasks.getByName("junitPlatformTestFreeDebug") as JUnit5Task
    assertThat(freeDebugTask.tagIncludes).containsOnly("some-tag", "free-debug-tag")
    assertThat(freeDebugTask.tagExcludes).containsOnly("other-tag")

    def freeReleaseTask = project.tasks.getByName("junitPlatformTestFreeRelease") as JUnit5Task
    assertThat(freeReleaseTask.tagIncludes).containsOnly("some-tag", "release-tag")
    assertThat(freeReleaseTask.tagExcludes).containsOnly("other-tag")

    def paidDebugTask = project.tasks.getByName("junitPlatformTestPaidDebug") as JUnit5Task
    assertThat(paidDebugTask.tagIncludes).containsOnly("some-tag", "paid-tag")
    assertThat(paidDebugTask.tagExcludes).containsOnly("other-tag")

    def paidReleaseTask = project.tasks.getByName("junitPlatformTestPaidRelease") as JUnit5Task
    assertThat(paidReleaseTask.tagIncludes).containsOnly("some-tag", "paid-tag", "release-tag")
    assertThat(paidReleaseTask.tagExcludes).containsOnly("other-tag")
  }
}