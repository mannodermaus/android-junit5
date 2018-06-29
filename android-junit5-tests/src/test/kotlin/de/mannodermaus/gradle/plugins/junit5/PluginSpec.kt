@file:Suppress("SimplifyBooleanWithConstants")

package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationKind.ANDROID_TEST
import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationScope.RUNTIME_ONLY
import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import de.mannodermaus.gradle.plugins.junit5.internal.find
import de.mannodermaus.gradle.plugins.junit5.providers.JavaDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.KotlinDirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import de.mannodermaus.gradle.plugins.junit5.util.TaskUtils.argument
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory.TestProjectBuilder
import de.mannodermaus.gradle.plugins.junit5.util.assertAll
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import de.mannodermaus.gradle.plugins.junit5.util.get
import de.mannodermaus.gradle.plugins.junit5.util.getArgument
import de.mannodermaus.gradle.plugins.junit5.util.throws
import de.mannodermaus.gradle.plugins.junit5.util.times
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testkit.runner.GradleRunner
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
  val environment by memoized(SCOPE) { TestEnvironment() }
  // Factory for temporary projects
  val factory by memoized(SCOPE) { TestProjectFactory(environment) }
  // The root project of each temporary project
  val testRoot by memoized { factory.newRootProject() }

  afterEachTest { testRoot.rootDir.deleteRecursively() }

  describe("a misconfigured project") {
    val testProjectBuilder by memoized { factory.newProject(testRoot) }

    on("not applying any Android plugin") {
      val expect = throws<PluginApplicationException> { testProjectBuilder.build() }

      it("throws an error") {
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

      it("throws an error") {
        val result = GradleRunner.create()
            .withGradleVersion("4.6")
            .withProjectDir(project.projectDir)
            .withArguments("--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("android-junit5 plugin requires Gradle $MIN_REQUIRED_GRADLE_VERSION or later")
      }
    }

    on("using instrumentation-test library without enabling that feature") {
      val project = testProjectBuilder
          .asAndroidApplication()
          .applyJunit5Plugin()
          .build()

      project.android.testOptions.junitPlatform {
        instrumentationTests.enabled(false)
      }

      val expect = throws<ProjectConfigurationException> {
        project.dependencies.add(
            "androidTestImplementation",
            project.dependencies.junit5.instrumentationTests())

        project.evaluate()
      }

      it("throws an error") {
        assertThat(expect.message)
            .contains("instrumentationTests.enabled true")
      }
    }

    on("accessing unavailable DSL values") {
      val project = testProjectBuilder
          .asAndroidLibrary()
          .applyJunit5Plugin(true)
          .buildAndEvaluate()

      it("doesn't have a filters extension point for an unknown build type") {
        val ju5 = project.android.testOptions
            .extensionByName<AndroidJUnitPlatformExtension>("junitPlatform")
        val expected = throws<UnknownDomainObjectException> { ju5.filters("unknown") }
        assertAll(
            { assertThat(expected.message?.contains("Extension with name")) },
            { assertThat(expected.message?.contains("does not exist")) }
        )
      }
    }

    context("JUnit 5 RunnerBuilder") {
      beforeEachTest {
        testProjectBuilder
            .asAndroidApplication()
            .applyJunit5Plugin()
      }

      on("build & evaluate") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          instrumentationTests.enabled(true)
        }

        project.evaluate()

        it("attaches the RunnerBuilder by default") {
          assertThat(project.android.defaultConfig.testInstrumentationRunnerArguments)
              .containsKey("runnerBuilder")
              .containsEntry("runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder")
        }
      }

      on("using another one already") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          instrumentationTests.enabled(true)
        }

        project.android.defaultConfig
            .testInstrumentationRunnerArgument(
                "runnerBuilder",
                "com.something.else.OtherRunnerBuilder")

        val expect = throws<ProjectConfigurationException> { project.evaluate() }

        it("throws an error") {
          assertThat(expect.cause?.message).isEqualTo(
              "Custom runnerBuilder is overwriting JUnit 5 integration! Change your declaration to 'com.something.else.OtherRunnerBuilder,de.mannodermaus.junit5.AndroidJUnit5Builder'.")
        }
      }

      on("including our own alongside another one") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          instrumentationTests.enabled(true)
        }

        project.android.defaultConfig
            .testInstrumentationRunnerArgument(
                "runnerBuilder",
                "com.something.else.OtherRunnerBuilder,de.mannodermaus.junit5.AndroidJUnit5Builder")

        project.evaluate()

        it("contains both") {
          assertThat(
              project.android.defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"])
              .contains("com.something.else.OtherRunnerBuilder")
              .contains("de.mannodermaus.junit5.AndroidJUnit5Builder")
        }
      }
    }
  }

  // Perform most tests with all different Android plugins that apply for JUnit 5
  val differentPlugins = mutableMapOf<String, TestProjectBuilder.() -> TestProjectBuilder>()
  differentPlugins["application"] = { this.asAndroidApplication() }
  differentPlugins["library"] = { this.asAndroidLibrary() }
  differentPlugins["feature"] = { this.asAndroidFeature() }

  differentPlugins.forEach { (pluginName, configFunc) ->
    describe("a project using the $pluginName plugin") {
      val testProjectBuilder by memoized {
        factory.newProject(testRoot).configFunc()
      }

      on("build & evaluate") {
        val project = testProjectBuilder.buildAndEvaluate()
        val ju5 = project.android.testOptions.extensionByName<AndroidJUnitPlatformExtension>(
            "junitPlatform")

        it("creates a JUnit 5 dependency handler") {
          assertThat(project.dependencies.junit5).isNotNull()
        }

        it("creates a parent junitPlatform task") {
          assertThat(project.tasks.findByName("junitPlatformTest"))
              .isNotNull()
        }

        it("doesn't create a parent Jacoco task") {
          assertThat(project.tasks.findByName("jacocoTestReport"))
              .isNull()
        }

        it("adds an JUnit 5 extension point to the testOptions") {
          assertThat(ju5).isNotNull()
        }

        it("adds a general-purpose filter to the JUnit 5 extension point") {
          val extension = ju5.extensionByName<FiltersExtension>("filters")
          assertThat(extension).isNotNull()
          assertThat(ju5.filters).isEqualTo(extension)
          assertThat(ju5.filters()).isEqualTo(extension)
          assertThat(ju5.filters(variant = null)).isEqualTo(extension)
        }

        listOf("debug", "release").forEach { buildType ->
          val buildTypeName = buildType.capitalize()

          it("creates a junitPlatform task for the $buildType build type") {
            assertThat(project.tasks.findByName("junitPlatformTest$buildTypeName"))
                .isNotNull()
          }

          it("doesn't create a Jacoco task for the $buildType build type") {
            assertThat(project.tasks.findByName("jacocoTestReport$buildTypeName"))
                .isNull()
          }

          it("adds a $buildType-specific filter to the JUnit 5 extension point") {
            val extension = ju5.extensionByName<FiltersExtension>("${buildType}Filters")
            assertThat(extension).isNotNull()
            assertThat(ju5.filters(variant = buildType)).isEqualTo(extension)
          }
        }
      }

      on("overriding default dependency versions") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          platformVersion = "1.3.3.7"
          jupiterVersion = "0.8.15"
          vintageVersion = "1.2.3"

          instrumentationTests {
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

        it("automatically includes instrumentation-test-runner at runtime") {
          val androidTestRuntimeOnly = project.configurations.find(
              kind = ANDROID_TEST, scope = RUNTIME_ONLY)

          assertThat(androidTestRuntimeOnly.dependencies
              .map { "${it.group}:${it.name}:${it.version}" })
              .contains("de.mannodermaus.junit5:android-instrumentation-test-runner:4.8.15")
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

      on("describing task dependencies") {
        val project = testProjectBuilder.build()
        val defaultTaskDep = project.task("onlyDefaultTask")
        val anotherTaskDep = project.task("someOtherTask")

        project.android.testOptions.junitPlatform {
          unitTests.all {
            dependsOn(anotherTaskDep)

            if (isRunAllTask) {
              dependsOn(defaultTaskDep)
            }
          }
        }

        project.evaluate()

        it("honors dependsOn for main test task") {
          val task = project.tasks.get<Task>("junitPlatformTest")
          assertThat(task.dependsOn).contains(defaultTaskDep, anotherTaskDep)
        }

        listOf("debug", "release").forEach { buildType ->
          val buildTypeName = buildType.capitalize()

          it("honors dependsOn for $buildType test task") {
            val task = project.tasks.get<Task>("junitPlatformTest$buildTypeName")
            assertThat(task.dependsOn)
                .doesNotContain(defaultTaskDep)
                .contains(anotherTaskDep)
          }
        }
      }

      on("using a custom reportsDir") {
        val project = testProjectBuilder.build()

        project.android.testOptions.junitPlatform {
          reportsDir(project.file("${project.buildDir.absolutePath}/other-path/test-reports"))
        }

        project.evaluate()

        listOf("debug", "release").forEach { buildType ->
          val buildTypeName = buildType.capitalize()

          it("uses that directory for $buildType test task") {
            val task = project.tasks.get<AndroidJUnit5UnitTest>("junitPlatformTest$buildTypeName")
            val argument = task.getArgument("--reports-dir")
            assertThat(argument)
                .endsWith("/other-path/test-reports/$buildType")
          }
        }
      }

      on("using a custom build type") {
        val project = testProjectBuilder.build()

        project.android.buildTypes {
          it.create("staging")
        }

        project.evaluate()

        it("creates a junitPlatform task for that build type") {
          assertThat(project.tasks.findByName("junitPlatformTestStaging"))
              .isNotNull()
        }

        it("is hooked into the main test task") {
          assertThat(project.tasks.getByName("junitPlatformTest")
              .dependsOn.map { (it as Task).name })
              .contains("junitPlatformTestStaging")
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
            val extension =ju5.extensionByName<FiltersExtension>("${flavor}Filters")
            assertThat(extension).isNotNull()
            assertThat(ju5.filters(variant = flavor)).isEqualTo(extension)
          }

          listOf("debug", "release").forEach { buildType ->
            val variantName = "$flavor${buildType.capitalize()}"

            it("creates task for build variant '$variantName'") {
              assertThat(project.tasks.findByName("junitPlatformTest${variantName.capitalize()}"))
                  .isNotNull()
            }

            it("hooks '$variantName' into the main task") {
              assertThat(project.tasks.getByName("junitPlatformTest")
                  .dependsOn.map { (it as Task).name })
                  .contains("junitPlatformTest${variantName.capitalize()}")
            }

            it("adds a $variantName-specific filter to the JUnit 5 extension point") {
              val extension = ju5.extensionByName<FiltersExtension>("${variantName}Filters")
              assertThat(extension).isNotNull()
              assertThat(ju5.filters(variant = variantName)).isEqualTo(extension)
            }
          }
        }

        it("uses unique report directories for all variants") {
          val tasks = project.tasks.withType(AndroidJUnit5UnitTest::class.java)
          val reportDirsCount = tasks
              .map { it.getArgument("--reports-dir") }
              .distinct()
              .count()

          assertThat(tasks.size).isEqualTo(reportDirsCount)
        }
      }

      context("unitTests.returnDefaultValues") {
        val project by memoized { testProjectBuilder.build() }

        listOf(true, false).forEach { state ->
          on("set to $state") {
            project.android.testOptions.junitPlatform.unitTests {
              returnDefaultValues = state
            }

            project.evaluate()

            it("configures the AGP setting correctly") {
              assertThat(project.android.testOptions.unitTests.isReturnDefaultValues)
                  .isEqualTo(state)
            }
          }
        }
      }

      context("unitTests.includeAndroidResources") {
        val project by memoized { testProjectBuilder.build() }

        listOf(true, false).forEach { state ->
          on("set to $state") {
            project.android.testOptions.junitPlatform.unitTests {
              includeAndroidResources = state
            }

            project.evaluate()

            it("configures the AGP setting correctly") {
              assertThat(project.android.testOptions.unitTests.isIncludeAndroidResources)
                  .isEqualTo(state)
            }
          }
        }
      }

      context("test folder detection") {
        // The order of applying the Kotlin plugin shouldn't interfere
        // with the detection of its source directories
        // (https://github.com/mannodermaus/android-junit5/issues/72)
        beforeEachTest { testProjectBuilder.applyKotlinPlugin() }

        listOf("debug", "release").forEach { buildType ->

          on("assembling the $buildType task") {
            val project = testProjectBuilder.buildAndEvaluate()
            val projectConfig = ProjectConfig(project)
            val task = project.tasks.get<AndroidJUnit5UnitTest>(
                "junitPlatformTest${buildType.capitalize()}")
            val folders = argument(task, "--scan-class-path")?.split(":") ?: listOf()

            val variant = projectConfig.unitTestVariants.find { it.name == buildType }
            require(variant != null)

            listOf(
                JavaDirectoryProvider(variant!!),
                KotlinDirectoryProvider(project, variant)).forEach { provider ->

              it("contains all class folders of the ${provider.javaClass.simpleName}") {
                assertThat(folders).containsAll(provider.classDirectories().map { it.absolutePath })
              }
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
                  .sourceDirectories
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
                  .sourceDirectories.asPath

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
                  .classDirectories.asPath

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
                    .classDirectories.asFileTree.files
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
                    .classDirectories.asFileTree.files
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
                      .classDirectories.asFileTree.files
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
    }
  }
})
