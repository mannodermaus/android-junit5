package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.gradle.api.Task
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.api.lifecycle.CachingMode.SCOPE

class NewPluginSpec : Spek({
  val environment by memoized(SCOPE) { TestEnvironment() }
  val factory by memoized(SCOPE) { TestProjectFactory(environment) }
  val testRoot by memoized { factory.newRootProject() }

  afterEachTest { testRoot.rootDir.deleteRecursively() }

  describe("a project") {
    val testProjectBuilder by memoized { factory.newProject(testRoot) }

    on("missing any Android plugin") {
      val expect = throws<PluginApplicationException> { testProjectBuilder.build() }

      it("should throw an raise about the requirement") {
        assertThat(expect.cause?.message)
            .isEqualTo("An Android plugin must be applied to this project")
      }
    }

    on("running with an old Gradle version") {
      val project = testProjectBuilder
          .asAndroidLibrary()
          .applyJunit5Plugin(false)
          .build()

      // Write out the build file manually, so we can properly test
      // if the plugin rejects outdated versions of Gradle.
      project.file("build.gradle").writeText("""
            buildscript {
                dependencies {
                    classpath files(${environment.pluginClasspathString})
                }
            }

            apply plugin: "de.mannodermaus.android-junit5"
        """.trimIndent())

      it("should throw an error about the minimum required version") {
        val result = GradleRunner.create()
            .withGradleVersion("4.2")
            .withProjectDir(project.projectDir)
            .withArguments("--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("android-junit5 plugin requires Gradle 4.3 or later")
      }
    }

    context("using the Android Application plugin") {
      beforeEachTest { testProjectBuilder.asAndroidApplication() }

      on("building & evaluating") {
        val project = testProjectBuilder.buildAndEvaluate()

        it("should create a JUnit 5 dependency handler") {
          assertThat(project.dependencies.junit5).isNotNull()
        }
      }

      on("overriding default dependency versions") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          platformVersion = "1.3.3.7"
          jupiterVersion = "0.8.15"
          vintageVersion = "1.2.3"

          instrumentationTests {
            enabled(true)
            version = "4.8.15"
          }
        }

        project.evaluate()

        it("uses the overridden unitTests dependencies") {
          val deps = project.dependencies.junit5.unitTests()
              .map { it.group to it.version }

          assertThat(deps).contains(
              "org.junit.platform" to "1.3.3.7",
              "org.junit.jupiter" to "0.8.15",
              "org.junit.vintage" to "1.2.3")
        }

        it("uses the overridden parameterized dependencies") {
          val deps = project.dependencies.junit5.parameterized()
              .map { it.group to it.version }

          assertThat(deps).contains(
              "org.junit.jupiter" to "0.8.15"
          )
        }

        it("uses the overridden instrumentationTests dependencies") {
          val deps = project.dependencies.junit5.instrumentationTests()
              .map { it.group to it.version }

          assertThat(deps).contains(
              "de.mannodermaus.junit5" to "4.8.15"
          )
        }
      }

      on("applying jvmArgs") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          unitTests.all {
            if (name.contains("Debug")) {
              jvmArgs("-noverify")
            }
          }
        }

        project.evaluate()

        it("uses specified jvmArgs in the debug task") {
          val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTestDebug")
          assertThat(task.jvmArgs).contains("-noverify")
        }

        it("doesn't use specified jvmArgs in the release task") {
          val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTestRelease")
          assertThat(task.jvmArgs).doesNotContain("-noverify")
        }
      }

      on("applying system properties") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          unitTests.all {
            if (name.contains("Debug")) {
              systemProperty("some.prop", "0815")
            }
          }
        }

        project.evaluate()

        it("uses specified property in the debug task") {
          val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTestDebug")
          assertThat(task.systemProperties).contains(entry("some.prop", "0815"))
        }

        it("doesn't use specified property in the release task") {
          val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTestRelease")
          assertThat(task.systemProperties).doesNotContain(entry("some.prop", "0815"))
        }
      }

      on("applying environment variables") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          unitTests.all {
            if (name.contains("Debug")) {
              environment("MY_ENV_VAR", "MegaShark.bin")
            }
          }
        }

        project.evaluate()

        it("uses specified envvar in the debug task") {
          val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTestDebug")
          assertThat(task.environment).contains(entry("MY_ENV_VAR", "MegaShark.bin"))
        }

        it("doesn't use specified envvar in the release task") {
          val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTestRelease")
          assertThat(task.environment).doesNotContain(entry("MY_ENV_VAR", "MegaShark.bin"))
        }
      }

      on("describing task dependencies") {
        val project = testProjectBuilder.build()
        val defaultTaskDep = project.task("onlyDefaultTask")
        val anotherTaskDep = project.task("someOtherTask")

        project.android.testOptions.junitPlatform {
          unitTests.all {
            dependsOn(anotherTaskDep)

            if (name == "junitPlatformTest") {
              dependsOn(defaultTaskDep)
            }
          }
        }

        project.evaluate()

        it("honors dependsOn for main test task") {
          val task = project.tasks.get<Task>("junitPlatformTest")
          assertThat(task.dependsOn).contains(defaultTaskDep, anotherTaskDep)
        }

        it("honors dependsOn for debug test task") {
          val task = project.tasks.get<Task>("junitPlatformTestDebug")
          assertThat(task.dependsOn)
              .doesNotContain(defaultTaskDep)
              .contains(anotherTaskDep)
        }

        it("honors dependsOn for release test task") {
          val task = project.tasks.get<Task>("junitPlatformTestRelease")
          assertThat(task.dependsOn)
              .doesNotContain(defaultTaskDep)
              .contains(anotherTaskDep)
        }
      }
    }
  }
})
