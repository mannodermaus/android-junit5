@file:Suppress("SimplifyBooleanWithConstants")

package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.util.*
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory2.TestProjectBuilder
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.tasks.testing.Test
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.api.lifecycle.CachingMode.SCOPE
import org.junit.platform.commons.util.PreconditionViolationException

/**
 * Unit Tests related to the plugin's configurability.
 * For every applicable Android plugin type, a suite
 * of tests is executed to verify the behavior and
 * customizability of the JUnit 5 plugin's tasks.
 */
class PluginSpec : Spek({
  // Access to Android SDK properties
  val environment by memoized(SCOPE) { TestEnvironment2() }
  // Factory for temporary projects
  val factory by memoized(SCOPE) { TestProjectFactory2(environment) }
  // The root project of each temporary project
  val testRoot by memoized { factory.newRootProject() }

  afterEachTest { testRoot.rootDir.deleteRecursively() }

  describe("a misconfigured project") {
    val testProjectBuilder by memoized { factory.newProject(testRoot) }

    on("not applying any supported Android plugin") {
      val expect = throws<PluginApplicationException> { testProjectBuilder.build() }

      it("throws an error") {
        assertThat(expect.cause?.message)
            .contains("One of the following plugins must be applied to this project")
      }
    }

    on("configuring unavailable DSL values") {
      val project = testProjectBuilder
          .asAndroidLibrary()
          .applyJUnit5Plugin(true)
          .build()

      project.android.testOptions.junitPlatform {
        filters("unknown", Action {
          it.includeTags("doesnt-matter")
        })
      }

      it("doesn't have a filters extension point for an unknown build type") {
        val expected = throws<ProjectConfigurationException> { project.evaluate() }
        assertAll(
            { assertThat(expected.message?.contains("Extension with name")) },
            { assertThat(expected.message?.contains("does not exist")) }
        )
      }
    }
  }

  // Perform most tests with all different Android plugins that apply for JUnit 5
  val differentPlugins = mutableMapOf<String, TestProjectBuilder.() -> TestProjectBuilder>()
  differentPlugins["application"] = { this.asAndroidApplication() }
  differentPlugins["library"] = { this.asAndroidLibrary() }
  differentPlugins["feature"] = { this.asAndroidFeature() }
  differentPlugins["dynamic-feature"] = { this.asAndroidDynamicFeature() }

  differentPlugins.forEach { (pluginName, configFunc) ->
    describe("a project using the $pluginName plugin") {
      val testProjectBuilder by memoized {
        factory.newProject(testRoot).configFunc()
      }

      on("build & evaluate") {
        val project = testProjectBuilder.buildAndEvaluate()
        val ju5 = project.android.testOptions.junitPlatform

        it("doesn't create a parent Jacoco task") {
          assertThat(project.tasks.findByName("jacocoTestReport"))
              .isNull()
        }

        it("adds an JUnit 5 extension point to the testOptions") {
          assertThat(ju5).isNotNull()
        }

        it("adds a general-purpose filter to the JUnit 5 extension point") {
          val extension = ju5.extensionByName<FiltersExtension>("filters")
          assertThat(extension).isNotNull
          assertThat(ju5.filters).isEqualTo(extension)
          assertThat(ju5.findFilters()).isEqualTo(extension)
          assertThat(ju5.findFilters(qualifier = null)).isEqualTo(extension)
        }

        listOf("debug", "release").forEach { buildType ->
          val buildTypeName = buildType.capitalize()

          it("doesn't create a Jacoco task for the $buildType build type") {
            assertThat(project.tasks.findByName("jacocoTestReport$buildTypeName"))
                .isNull()
          }

          it("adds a $buildType-specific filter to the JUnit 5 extension point") {
            val extension = ju5.extensionByName<FiltersExtension>("${buildType}Filters")
            assertThat(extension).isNotNull
            assertThat(ju5.findFilters(qualifier = buildType)).isEqualTo(extension)
          }
        }
      }

      on("applying configuration parameters") {
        val project = testProjectBuilder.build()

        it("throws an exception if the key is empty") {
          val expected = throws<PreconditionViolationException> {
            project.android.testOptions.junitPlatform {
              configurationParameter("", "some-value")
            }
          }

          assertThat(expected.message).contains("key must not be blank")
        }

        it("throws an exception if the key contains illegal characters") {
          val expected = throws<PreconditionViolationException> {
            project.android.testOptions.junitPlatform {
              configurationParameter("illegal=key", "some-value")
            }
          }

          assertThat(expected.message).contains("key must not contain '='")
        }
      }

      on("using product flavors") {
        val project = testProjectBuilder.build()

        project.android.flavorDimensions("price")
        project.android.productFlavors {
          it.create("free").dimension = "price"
          it.create("paid").dimension = "price"
        }

        project.evaluate()

        val ju5 = project.android.testOptions.extensionByName<AndroidJUnitPlatformExtension>(
            "junitPlatform")

        listOf("free", "paid").forEach { flavor ->
          it("adds a $flavor-specific filter to the JUnit 5 extension point") {
            val extension = ju5.extensionByName<FiltersExtension>("${flavor}Filters")
            assertThat(extension).isNotNull
            assertThat(ju5.findFilters(qualifier = flavor)).isEqualTo(extension)
          }

          listOf("debug", "release").forEach { buildType ->
            val variantName = "$flavor${buildType.capitalize()}"

            it("adds a $variantName-specific filter to the JUnit 5 extension point") {
              val extension = ju5.extensionByName<FiltersExtension>("${variantName}Filters")
              assertThat(extension).isNotNull
              assertThat(ju5.findFilters(qualifier = variantName)).isEqualTo(extension)
            }
          }
        }
      }

      context("jacoco integration") {
        beforeEachTest { testProjectBuilder.applyJacocoPlugin() }

        on("build & evaluate") {
          val project = testProjectBuilder.buildAndEvaluate()

          it("generates a parent task") {
            assertThat(project.tasks.findByName("jacocoTestReport"))
                .isNotNull()
          }

          listOf("debug", "release").forEach { buildType ->

            it("hooks in a child task for the $buildType build type") {
              assertThat(project.tasks.getByName("jacocoTestReport")
                  .dependsOn.map { (it as Task).name })
                  .contains("jacocoTestReport${buildType.capitalize()}")
            }

            it("includes Main-scoped source dirs for the $buildType build type") {
              // Expected items: "src/main/java" & "src/<TypeName>/java"
              val sourceDirs = project.tasks.get<AndroidJUnit5JacocoReport>(
                  "jacocoTestReport${buildType.capitalize()}")
                  .sourceDirectories!!
                  .map { it.absolutePath }

              val mainDir = sourceDirs.find { it.endsWith("src/main/java") }
              val typeDir = sourceDirs.find { it.endsWith("src/$buildType/java") }
              assertAll(
                  "Mismatch! Actual dirs: $sourceDirs",
                  { assertThat(mainDir).withFailMessage("main").isNotNull() },
                  { assertThat(typeDir).withFailMessage(buildType).isNotNull() }
              )
            }

            it("doesn't include Test-scoped source dirs for the $buildType build type") {
              // Expected omissions: "src/test/java" & "src/test<TypeName>/java"
              val sourceDirs = project.tasks.get<AndroidJUnit5JacocoReport>(
                  "jacocoTestReport${buildType.capitalize()}")
                  .sourceDirectories!!.asPath

              assertAll(
                  "Mismatch! Actual dirs: $sourceDirs",
                  { assertThat(sourceDirs).doesNotContain("src/test/java") },
                  {
                    assertThat(sourceDirs).doesNotContain("src/test${buildType.capitalize()}/java")
                  }
              )
            }

            it("doesn't include Test-scoped class dirs for the $buildType build type") {
              // Expected omissions: "classes/test"
              val classDirs = project.tasks.get<AndroidJUnit5JacocoReport>(
                  "jacocoTestReport${buildType.capitalize()}")
                  .classDirectories!!.asPath

              assertThat(classDirs).doesNotContain("classes/test")
            }
          }
        }

        on("using a custom build type") {
          val project = testProjectBuilder.build()

          project.android.buildTypes {
            it.create("staging")
          }

          project.evaluate()

          it("creates a Jacoco task for that") {
            assertThat(project.tasks.findByName("jacocoTestReportStaging"))
                .isNotNull()
          }

          it("is hooked into the main Jacoco task") {
            assertThat(project.tasks.getByName("jacocoTestReport")
                .dependsOn.map { (it as Task).name })
                .contains("jacocoTestReportStaging")
          }
        }

        on("applying custom report destination folders") {
          val project = testProjectBuilder.build()

          project.android.testOptions.junitPlatform {
            jacocoOptions {
              xml.destination(project.file("build/other-jacoco-folder/xml"))
              csv.destination(project.file("build/html-reports/jacoco"))
              html.destination(project.file("build/CSVISDABEST"))
            }
          }

          project.evaluate()

          it("applies them correctly") {
            project.tasks.withType(AndroidJUnit5JacocoReport::class.java)
                .map { it.reports }
                .forEach {
                  assertAll(
                      { assertThat(it.xml.destination.endsWith("build/other-jacoco-folder/xml")) },
                      { assertThat(it.csv.destination.endsWith("build/html-reports/jacoco")) },
                      { assertThat(it.html.destination.endsWith("build/CSVISDABEST")) }
                  )
                }
          }
        }

        listOf(true, false).forEach { enabled ->
          val operationName = if (enabled) "enabling" else "disabling"

          on("$operationName reports in the DSL") {
            val project = testProjectBuilder.build()

            project.android.testOptions.junitPlatform {
              jacocoOptions {
                xml.enabled(enabled)
                csv.enabled(enabled)
                html.enabled(enabled)
              }
            }

            project.evaluate()

            it("is $operationName reports on the generated tasks as well") {
              project.tasks.withType(AndroidJUnit5JacocoReport::class.java)
                  .map { it.reports }
                  .forEach {
                    assertAll(
                        { assertThat(it.xml.isEnabled == enabled) },
                        { assertThat(it.csv.isEnabled == enabled) },
                        { assertThat(it.html.isEnabled == enabled) }
                    )
                  }
            }
          }

          context("using custom exclusion rules") {
            val project by memoized { testProjectBuilder.build() }

            // Create some fake files to verify the Jacoco tree
            beforeEachTest {
              // Since the location of intermediate class files changed in different
              // versions of the Android Gradle Plugin,
              // create each class file in multiple directories to remain compatible with all approaches
              // TODO Clean this mess up once the Android Gradle Plugin 3.2.0 finally decides on something. :|
              listOf(
                  // AGP 3.2.0-alpha07 and above
                  "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes",
                  // AGP 3.2.0-alpha06
                  "build/intermediates/artifact_transform/compileDebugJavaWithJavac/classes",
                  // AGP 3.2.0-alpha04 and above
                  "build/intermediates/artifact_transform/javac/debug/classes",
                  // Everything below
                  "build/intermediates/classes/debug").forEach { folder ->
                project.file(folder).mkdirs()
                project.file("$folder/R.class").createNewFile()
                project.file("$folder/FirstFile.class").createNewFile()
                project.file("$folder/SecondFile.class").createNewFile()
              }

              listOf(
                  // AGP 3.2.0-alpha07 and above
                  "build/intermediates/javac/release/compileReleaseJavaWithJavac/classes",
                  // AGP 3.2.0-alpha06
                  "build/intermediates/artifact_transform/compileReleaseJavaWithJavac/classes",
                  // AGP 3.2.0-alpha04 and above
                  "build/intermediates/artifact_transform/javac/release/classes",
                  // Everything below
                  "build/intermediates/classes/release").forEach { folder ->
                project.file(folder).mkdirs()
                project.file("$folder/R.class").createNewFile()
                project.file("$folder/SecondFile.class").createNewFile()
              }

              project.file("src/main/java").mkdirs()
              project.file("src/main/java/OkFile.java").createNewFile()
              project.file("src/main/java/AnnoyingFile.java").createNewFile()
              project.file("src/release/java").mkdirs()
              project.file("src/release/java/ReleaseOnlyFile.java").createNewFile()
            }

            on("adding rules") {
              project.android.testOptions.junitPlatform {
                jacocoOptions {
                  excludedClasses.add("Second*.class")
                }
              }

              project.evaluate()

              it("honors the debug class exclusion rules") {
                // Should be included:
                //  * FirstFile.class
                // Should be excluded:
                //  * R.class (by default)
                //  * SecondFile.class (through rule)
                val fileNames = project.tasks.get<AndroidJUnit5JacocoReport>(
                    "jacocoTestReportDebug")
                    .classDirectories!!.asFileTree.files
                    .map { it.name }

                assertThat(fileNames)
                    .contains("FirstFile.class")
                    .doesNotContain(
                        "R.class",
                        "SecondFile.class")
              }

              it("honors the release class exclusion rules") {
                // Should be included:
                //  (nothing)
                // Should be excluded:
                //  * R.class (by default)
                //  * FirstFile.class (other source set)
                //  * SecondFile.class (through rule)
                val fileNames = project.tasks.get<AndroidJUnit5JacocoReport>(
                    "jacocoTestReportRelease")
                    .classDirectories!!.asFileTree.files
                    .map { it.name }

                assertThat(fileNames)
                    .doesNotContain(
                        "R.class",
                        "FirstFile.class",
                        "SecondFile.class")
              }
            }

            on("replacing class rules") {
              project.android.testOptions.junitPlatform {
                jacocoOptions {
                  excludedClasses = mutableListOf()
                }
              }

              project.evaluate()

              listOf("debug", "release").forEach { buildType ->
                it("doesn't exclude R.class anymore for the $buildType build type") {
                  val fileNames = project.tasks.get<AndroidJUnit5JacocoReport>(
                      "jacocoTestReport${buildType.capitalize()}")
                      .classDirectories!!.asFileTree.files
                      .map { it.name }

                  assertThat(fileNames).contains("R.class")
                }
              }
            }
          }

          on("using product flavors") {
            val project = testProjectBuilder.build()

            project.android.flavorDimensions("price")
            project.android.productFlavors {
              it.create("free").dimension = "price"
              it.create("paid").dimension = "price"
            }

            project.evaluate()

            (listOf("free", "paid") * listOf("debug", "release")).forEach { (flavor, buildType) ->
              val variantName = "$flavor${buildType.capitalize()}"

              it("creates Jacoco task for build variant '$variantName'") {
                assertThat(project.tasks.findByName("jacocoTestReport${variantName.capitalize()}"))
                    .isNotNull()
              }

              it("hooks '$variantName' into the main Jacoco task") {
                assertThat(project.tasks.getByName("jacocoTestReport")
                    .dependsOn.map { (it as Task).name })
                    .contains("jacocoTestReport${variantName.capitalize()}")
              }
            }
          }

          context("restricting task generation") {
            val project by memoized { testProjectBuilder.build() }

            on("disabling task generation altogether") {
              project.android.testOptions.junitPlatform {
                jacocoOptions {
                  taskGenerationEnabled = false
                }
              }

              project.evaluate()

              listOf("jacocoTestReport", "jacocoTestReportDebug", "jacocoTestReportRelease")
                  .forEach { task ->
                    it("doesn't generate task '$task'") {
                      assertThat(project.tasks.findByName(task)).isNull()
                    }
                  }
            }

            on("specifying specific variants without product flavors") {
              project.android.testOptions.junitPlatform {
                jacocoOptions {
                  onlyGenerateTasksForVariants("debug")
                }
              }

              project.evaluate()

              it("generates main task") {
                assertThat(project.tasks.findByName("jacocoTestReport")).isNotNull()
              }

              it("generates debug task") {
                assertThat(project.tasks.findByName("jacocoTestReportDebug")).isNotNull()
              }

              it("doesn't generate release task") {
                assertThat(project.tasks.findByName("jacocoTestReportRelease")).isNull()
              }
            }

            on("specifying specific variants with product flavors") {
              project.android.flavorDimensions("tier")
              project.android.productFlavors {
                it.create("paid").dimension = "tier"
                it.create("free").dimension = "tier"
              }

              project.android.testOptions.junitPlatform {
                jacocoOptions {
                  onlyGenerateTasksForVariants("paidDebug", "freeRelease")
                }
              }

              project.evaluate()

              it("generates main task") {
                assertThat(project.tasks.findByName("jacocoTestReport")).isNotNull()
              }

              listOf("paidDebug", "freeRelease")
                  .forEach { generatedTask ->
                    it("generates $generatedTask task") {
                      assertThat(
                          project.tasks.findByName("jacocoTestReport${generatedTask.capitalize()}"))
                          .isNotNull()
                    }
                  }

              listOf("paidRelease", "freeDebug")
                  .forEach { filteredTask ->
                    it("doesn't generate $filteredTask task") {
                      assertThat(
                          project.tasks.findByName("jacocoTestReport${filteredTask.capitalize()}"))
                          .isNull()
                    }
                  }
            }
          }
        }
      }

      context("filters DSL") {
        val project by memoized { testProjectBuilder.build() }

        on("using global filters") {
          project.android.testOptions.junitPlatform {
            filters {
              includeTags("global-include-tag")
              excludeTags("global-exclude-tag")
              includeEngines("global-include-engine")
              excludeEngines("global-exclude-engine")
              includePattern("com.example.package1")
              excludePattern("com.example.package2")
            }
          }

          project.evaluate()

          listOf("debug", "release").forEach { buildType ->
            it("applies configuration correctly to the $buildType task") {
              val task = project.tasks.get<Test>("test${buildType.capitalize()}UnitTest")
              assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag")
              assertThat(task.junitPlatformOptions.excludeTags).contains("global-exclude-tag")
              assertThat(task.junitPlatformOptions.includeEngines).contains("global-include-engine")
              assertThat(task.junitPlatformOptions.excludeEngines).contains("global-exclude-engine")
              assertThat(task.includes).contains("com.example.package1")
              assertThat(task.excludes).contains("com.example.package2")
            }
          }
        }

        on("using custom build types & multiple flavor dimensions") {
          project.android.apply {
            flavorDimensions("brand", "environment", "payment")
            productFlavors.apply {
              create("brandA").dimension = "brand"
              create("brandB").dimension = "brand"

              create("development").dimension = "environment"
              create("production").dimension = "environment"

              create("free").dimension = "payment"
              create("paid").dimension = "payment"
            }
            buildTypes.apply {
              create("ci").initWith(findByName("debug"))
            }
          }

          project.evaluate()

          listOf(
              "filters",

              "debugFilters",
              "releaseFilters",
              "ciFilters",

              "brandAFilters",
              "brandBFilters",

              "developmentFilters",
              "productionFilters",

              "freeFilters",
              "paidFilters",

              "brandADevelopmentPaidDebugFilters",
              "brandADevelopmentPaidReleaseFilters",
              "brandADevelopmentPaidCiFilters",
              "brandADevelopmentFreeDebugFilters",
              "brandADevelopmentFreeReleaseFilters",
              "brandADevelopmentFreeCiFilters",
              "brandAProductionPaidDebugFilters",
              "brandAProductionPaidReleaseFilters",
              "brandAProductionPaidCiFilters",
              "brandAProductionFreeDebugFilters",
              "brandAProductionFreeReleaseFilters",
              "brandAProductionFreeCiFilters",

              "brandBDevelopmentPaidDebugFilters",
              "brandBDevelopmentPaidReleaseFilters",
              "brandBDevelopmentPaidCiFilters",
              "brandBDevelopmentFreeDebugFilters",
              "brandBDevelopmentFreeReleaseFilters",
              "brandBDevelopmentFreeCiFilters",
              "brandBProductionPaidDebugFilters",
              "brandBProductionPaidReleaseFilters",
              "brandBProductionPaidCiFilters",
              "brandBProductionFreeDebugFilters",
              "brandBProductionFreeReleaseFilters",
              "brandBProductionFreeCiFilters"
          ).forEach { name ->
            it("creates an extension named '$name'") {
              val ju5 = project.android.testOptions.junitPlatform
              val extension = ju5.extensionByName<FiltersExtension>(name)
              assertThat(extension).isNotNull()
            }
          }
        }

        on("using flavor-specific filters") {
          project.android.flavorDimensions("tier")
          project.android.productFlavors.apply {
            create("free").dimension = "tier"
            create("paid").dimension = "tier"
          }

          project.android.testOptions.junitPlatform {
            filters {
              includeTags("global-include-tag")
              excludeTags("global-exclude-tag")
              includePattern("com.example.package1")
            }
            filters("paid", Action {
              it.includeEngines("paid-include-engine")
              it.includePattern("com.example.paid")
              it.excludePattern("com.example.package1")
            })
            filters("freeDebug", Action {
              it.includeTags("freeDebug-include-tag")
            })
            filters("paidRelease", Action {
              it.includeTags("paidRelease-include-tag")
              it.includeTags("global-exclude-tag")
              it.includePattern("com.example.paid.release")
            })
          }

          project.evaluate()

          it("applies freeDebug configuration correctly") {
            val task = project.tasks.get<Test>("testFreeDebugUnitTest")
            assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag",
                "freeDebug-include-tag")
            assertThat(task.junitPlatformOptions.includeTags).doesNotContain(
                "paidRelease-include-tag")
            assertThat(task.junitPlatformOptions.excludeTags).contains("global-exclude-tag")

            assertThat(task.junitPlatformOptions.includeEngines).doesNotContain(
                "paid-include-engine")

            assertThat(task.includes).contains("com.example.package1")
            assertThat(task.includes).doesNotContain("com.example.paid",
                "com.example.paid.release")
          }

          it("applies freeRelease configuration correctly") {
            val task = project.tasks.get<Test>("testFreeReleaseUnitTest")
            assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag")
            assertThat(task.junitPlatformOptions.includeTags).doesNotContain(
                "freeDebug-include-tag",
                "paidRelease-include-tag")
            assertThat(task.junitPlatformOptions.excludeTags).contains("global-exclude-tag")

            assertThat(task.junitPlatformOptions.includeEngines).doesNotContain(
                "paid-include-engine")

            assertThat(task.includes).contains("com.example.package1")
            assertThat(task.includes).doesNotContain("com.example.paid",
                "com.example.paid.release")
          }

          it("applies paidDebug configuration correctly") {
            val task = project.tasks.get<Test>("testPaidDebugUnitTest")
            assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag")
            assertThat(task.junitPlatformOptions.includeTags).doesNotContain(
                "freeDebug-include-tag",
                "paidRelease-include-tag")
            assertThat(task.junitPlatformOptions.excludeTags).contains("global-exclude-tag")

            assertThat(task.junitPlatformOptions.includeEngines).contains("paid-include-engine")

            assertThat(task.includes).contains("com.example.paid")
            assertThat(task.excludes).contains("com.example.package1")
            assertThat(task.includes).doesNotContain("com.example.package1",
                "com.example.paid.release")
          }

          it("applies paidRelease configuration correctly") {
            val task = project.tasks.get<Test>("testPaidReleaseUnitTest")
            assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag",
                "global-exclude-tag",
                "paidRelease-include-tag")
            assertThat(task.junitPlatformOptions.includeTags).doesNotContain(
                "freeDebug-include-tag")
            assertThat(task.junitPlatformOptions.excludeTags).doesNotContain("global-exclude-tag")

            assertThat(task.junitPlatformOptions.includeEngines).contains("paid-include-engine")

            assertThat(task.includes).contains("com.example.paid",
                "com.example.paid.release")
            assertThat(task.includes).doesNotContain("com.example.package1")
            assertThat(task.excludes).contains("com.example.package1")
          }
        }

        on("using build-type-specific filters") {
          project.android.testOptions.junitPlatform {
            filters {
              includeTags("global-include-tag")
              includeEngines("global-include-engine")
              includePattern("pattern123")
            }
            filters("debug", Action {
              it.excludeTags("debug-exclude-tag")
              it.excludeEngines("debug-exclude-engine")
              it.excludePattern("pattern123")
              it.excludePattern("debug-pattern")
            })
            filters("release", Action {
              it.includeTags("rel-include-tag")
              it.includeEngines("rel-include-engine")
              it.excludeEngines("global-include-engine")
              it.includePattern("release-pattern")
            })
          }

          project.evaluate()

          it("applies configuration correctly to the debug task") {
            val task = project.tasks.get<Test>("testDebugUnitTest")
            assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag")
            assertThat(task.junitPlatformOptions.includeTags).doesNotContain("rel-include-tag")
            assertThat(task.junitPlatformOptions.excludeTags).contains("debug-exclude-tag")

            assertThat(task.junitPlatformOptions.includeEngines).contains("global-include-engine")
            assertThat(task.junitPlatformOptions.includeEngines).doesNotContain(
                "rel-include-engine")
            assertThat(task.junitPlatformOptions.excludeEngines).contains("debug-exclude-engine")

            assertThat(task.includes).doesNotContain("pattern123")
            assertThat(task.excludes).contains("pattern123", "debug-pattern")
          }

          it("applies configuration correctly to the release task") {
            val task = project.tasks.get<Test>("testReleaseUnitTest")
            assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag",
                "rel-include-tag")
            assertThat(task.junitPlatformOptions.excludeTags).doesNotContain("debug-exclude-tag")

            assertThat(task.junitPlatformOptions.includeEngines).contains("rel-include-engine")
            assertThat(task.junitPlatformOptions.includeEngines).doesNotContain(
                "global-include-engine")
            assertThat(task.junitPlatformOptions.excludeEngines).contains("global-include-engine")
            assertThat(task.junitPlatformOptions.excludeEngines).doesNotContain(
                "debug-exclude-engine")

            assertThat(task.includes).contains("pattern123", "release-pattern")
            assertThat(task.excludes).doesNotContain("pattern123")
          }
        }
      }
    }
  }
})
