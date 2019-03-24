package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment2
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory2
import kotlin.io.FilesKt
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.junit.After
import org.junit.Before

import static com.google.common.truth.Truth.assertThat

class DslGroovyTests {

  private TestProjectFactory2 factory
  private Project testRoot

  @Before
  void beforeEach() {
    def environment = new TestEnvironment2()

    this.factory = new TestProjectFactory2(environment)
    this.testRoot = factory.newRootProject()
  }

  @After
  void afterEach() {
    FilesKt.deleteRecursively(this.testRoot.rootDir)
  }

  @org.junit.Test
  void dynamicFiltersMethodsCanBeCalledOnExistingBuildTypes() {
    def project = factory.newProject(testRoot, null)
        .asAndroidApplication()
        .build()

    project.android.testOptions.junitPlatform {
      filters {
        includeTags "some-tag"
        excludeTags "other-tag"
      }
      debugFilters {
        includeTags "debug-tag"
      }
    }

    project.evaluate()

    def debugTask = project.tasks.getByName("testDebugUnitTest") as Test
    assertThat(debugTask.testFramework.options.includeTags).containsOnly("some-tag", "debug-tag")
    assertThat(debugTask.testFramework.options.excludeTags).containsOnly("other-tag")

    def releaseTask = project.tasks.getByName("testReleaseUnitTest") as Test
    assertThat(releaseTask.testFramework.options.includeTags).containsOnly("some-tag")
    assertThat(releaseTask.testFramework.options.excludeTags).containsOnly("other-tag")
  }

  @org.junit.Test
  void dynamicFiltersMethodsCanBeCalledOnExistingProductFlavors() {
    def project = factory.newProject(testRoot, null)
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
          includeTags "some-tag"
          excludeTags "other-tag"
        }
        freeDebugFilters {
          includeTags "free-debug-tag"
        }
        paidFilters {
          includeTags "paid-tag"
        }
        releaseFilters {
          includeTags "release-tag"
        }
      }
    }

    project.evaluate()

    def freeDebugTask = project.tasks.getByName("testFreeDebugUnitTest") as Test
    assertThat(freeDebugTask.testFramework.options.includeTags).
        containsOnly("some-tag", "free-debug-tag")
    assertThat(freeDebugTask.testFramework.options.excludeTags).containsOnly("other-tag")

    def freeReleaseTask = project.tasks.getByName("testFreeReleaseUnitTest") as Test
    assertThat(freeReleaseTask.testFramework.options.includeTags).
        containsOnly("some-tag", "release-tag")
    assertThat(freeReleaseTask.testFramework.options.excludeTags).containsOnly("other-tag")

    def paidDebugTask = project.tasks.getByName("testPaidDebugUnitTest") as Test
    assertThat(paidDebugTask.testFramework.options.includeTags).containsOnly("some-tag", "paid-tag")
    assertThat(paidDebugTask.testFramework.options.excludeTags).containsOnly("other-tag")

    def paidReleaseTask = project.tasks.getByName("testPaidReleaseUnitTest") as Test
    assertThat(paidReleaseTask.testFramework.options.includeTags).
        containsOnly("some-tag", "paid-tag", "release-tag")
    assertThat(paidReleaseTask.testFramework.options.excludeTags).containsOnly("other-tag")
  }

  @org.junit.Test
  void complexExampleWithMultipleFlavorDimensionsAndBuildTypes() {
    def project = factory.newProject(testRoot, null)
        .asAndroidApplication()
        .build()

    project.android {
      flavorDimensions "brand", "environment", "payment"
      productFlavors {
        brandA {
          dimension "brand"
        }
        brandB {
          dimension "brand"
        }

        development {
          dimension "environment"
        }
        production {
          dimension "environment"
        }

        free {
          dimension "payment"
        }
        paid {
          dimension "payment"
        }
      }

      buildTypes {
        ci {
          initWith debug
        }
      }

      testOptions.junitPlatform {
        filters {
          includeTags "global-tag"
        }

        brandAFilters {
          includeTags "brandA-tag"
          includeTags "some-other-tag"
        }

        developmentFilters {
          includeTags "development-tag"
        }

        paidFilters {
          includeTags "paid-tag"
          excludeTags "some-other-tag"
        }
      }
    }

    project.evaluate()

    def brandADevelopmentPaidDebugTask = project.tasks
        .getByName("testBrandADevelopmentPaidDebugUnitTest") as Test

    assertThat(brandADevelopmentPaidDebugTask.testFramework.options.includeTags).
        containsOnly("global-tag", "brandA-tag", "development-tag", "paid-tag")
    assertThat(brandADevelopmentPaidDebugTask.testFramework.options.excludeTags).
        containsOnly("some-other-tag")
  }
}
