package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5JacocoReport
import de.mannodermaus.gradle.plugins.junit5.tasks.AndroidJUnit5UnitTest
import de.mannodermaus.gradle.plugins.junit5.util.TaskUtils
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.TestProjectFactory
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/*
 * Plugin-related unit testing for Android Gradle Plugin 3.
 *
 * This class also provides hooks into the lifecycle of the JUnit 5 plugin,
 * as well as an Environment & Factory for the creation
 * of "mock projects" to test with.
 */

class PluginSpec extends Specification {

  protected static final environment = new TestEnvironment()
  protected static final factory = new TestProjectFactory(environment)

  // Root Project containing required Android SDK reference in local.properties;
  // recreated for each executed test case
  private Project testRoot

  /* Before Each */

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

  def "Requires Gradle 4.3 or later"() {
    when:
    // Write out the build file manually, so we can properly test
    // if the plugin rejects outdated versions of Gradle.
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
        .withGradleVersion("4.2")
        .withProjectDir(project.projectDir)
        .buildAndFail()

    then:
    result.output.contains("android-junit5 plugin requires Gradle 4.3 or later")
  }

  def "Dependency Handler Creation"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .buildAndEvaluate()

    then:
    assert project.dependencies.junit5 != null
  }

  // FIXME When the deprecation is removed in a future major update, delete this test as well
  def "Deprecated Dependency Handlers still work"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .buildAndEvaluate()

    then:
    // @formatter:off
    assert project.dependencies.junit5.unitTests() == project.dependencies.junit5()
    assert project.dependencies.junit5.parameterized() == project.dependencies.junit5Params()
    assert project.dependencies.junit5.unitTestsRuntime() == project.dependencies.junit5EmbeddedRuntime()
    // @formatter:on
  }

  def "Overwrite Dependency Versions"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions.junitPlatform {
        platformVersion = "1.3.3.7"
        jupiterVersion = "0.8.15"
        vintageVersion = "1.2.3"

        instrumentationTests {
          enabled true
          version = "4.8.15"
        }
      }
    }

    project.evaluate()

    then:
    def ju5Deps = project.dependencies.junit5.unitTests() as List<Dependency>
    assert ju5Deps.find { it.group == "org.junit.platform" && it.version == "1.3.3.7" } != null
    assert ju5Deps.find { it.group == "org.junit.jupiter" && it.version == "0.8.15" } != null
    assert ju5Deps.find { it.group == "org.junit.vintage" && it.version == "1.2.3" } != null

    def ju5ParamsDeps = project.dependencies.junit5.parameterized() as List<Dependency>
    assert ju5ParamsDeps.find { it.group == "org.junit.jupiter" && it.version == "0.8.15" } != null

    def ju5InstrumentationDeps = project.dependencies.junit5.instrumentationTests() as List<Dependency>
    assert ju5InstrumentationDeps.find {
      it.group == "de.mannodermaus.junit5" && it.version == "4.8.15"
    } != null
  }

  // FIXME When the deprecation is removed in a future major update, delete this test as well
  def "Using the old DSL to configure JUnit 5 properly delegates"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.junitPlatform {
      platformVersion = "1.3.3.7"
      jupiterVersion = "0.8.15"
      vintageVersion = "1.2.3"

      instrumentationTests {
        enabled = true
        version = "4.8.15"
      }
    }

    project.evaluate()

    then:
    def ju5Deps = project.dependencies.junit5.unitTests() as List<Dependency>
    assert ju5Deps.find { it.group == "org.junit.platform" && it.version == "1.3.3.7" } != null
    assert ju5Deps.find { it.group == "org.junit.jupiter" && it.version == "0.8.15" } != null
    assert ju5Deps.find { it.group == "org.junit.vintage" && it.version == "1.2.3" } != null

    def ju5ParamsDeps = project.dependencies.junit5.parameterized() as List<Dependency>
    assert ju5ParamsDeps.find { it.group == "org.junit.jupiter" && it.version == "0.8.15" } != null

    def ju5InstrumentationDeps = project.dependencies.junit5.instrumentationTests() as List<Dependency>
    assert ju5InstrumentationDeps.find {
      it.group == "de.mannodermaus.junit5" && it.version == "4.8.15"
    } != null
  }

  def "jvmArgs are properly applied"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions {
        junitPlatform.unitTests.all {
          if (it.name.contains("Debug")) {
            jvmArgs "-noverify"
          }
        }
      }
    }

    project.evaluate()

    then:
    def runDebug = project.tasks.getByName("junitPlatformTestDebug") as AndroidJUnit5UnitTest
    def runRelease = project.tasks.getByName("junitPlatformTestRelease") as AndroidJUnit5UnitTest

    assert runDebug.jvmArgs.contains("-noverify")
    assert !runRelease.jvmArgs.contains("-noverify")
  }

  def "System properties are properly applied"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions {
        junitPlatform.unitTests.all {
          if (it.name.contains("Debug")) {
            systemProperty "some.prop", "0815"
          }
        }
      }
    }

    project.evaluate()

    then:
    def runDebug = project.tasks.getByName("junitPlatformTestDebug") as AndroidJUnit5UnitTest
    def runRelease = project.tasks.getByName("junitPlatformTestRelease") as AndroidJUnit5UnitTest

    assert runDebug.systemProperties.containsKey("some.prop")
    assert !runRelease.systemProperties.containsKey("some.prop")
  }

  def "Environment variables are properly applied"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions {
        junitPlatform.unitTests.all {
          if (it.name.contains("Debug")) {
            environment "MY_ENV_VAR", "MegaShark.bin"
          }
        }
      }
    }

    project.evaluate()

    then:
    def runDebug = project.tasks.getByName("junitPlatformTestDebug") as AndroidJUnit5UnitTest
    def runRelease = project.tasks.getByName("junitPlatformTestRelease") as AndroidJUnit5UnitTest

    assert runDebug.environment.containsKey("MY_ENV_VAR")
    assert !runRelease.environment.containsKey("MY_ENV_VAR")
  }

  def "Configuration Closure works properly"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    def onlyDefaultTask = project.task("onlyDefaultTask")
    def otherTask = project.task("someOtherTask")

    project.android {
      testOptions {
        junitPlatform.unitTests.all {
          jvmArgs "-noverify"
          systemProperty "some.prop", "0815"
          environment "MY_ENV_VAR", "MegaShark.bin"
          dependsOn otherTask

          if (it.name == "junitPlatformTest") {
            it.dependsOn onlyDefaultTask
          }
        }
      }
    }

    project.evaluate()

    then:
    def runAll = project.tasks.getByName("junitPlatformTest")
    def runDebug = project.tasks.getByName("junitPlatformTestDebug") as AndroidJUnit5UnitTest
    def runRelease = project.tasks.getByName("junitPlatformTestRelease") as AndroidJUnit5UnitTest

    assert runAll.getDependsOn().contains(otherTask)
    assert runAll.getDependsOn().contains(onlyDefaultTask)
    assert runDebug.jvmArgs.contains("-noverify")
    assert runDebug.systemProperties.containsKey("some.prop")
    assert runDebug.environment.containsKey("MY_ENV_VAR")
    assert runDebug.getDependsOn().contains(otherTask)
    assert !runDebug.getDependsOn().contains(onlyDefaultTask)
    assert runRelease.jvmArgs.contains("-noverify")
    assert runRelease.systemProperties.containsKey("some.prop")
    assert runRelease.environment.containsKey("MY_ENV_VAR")
    assert runRelease.getDependsOn().contains(otherTask)
    assert !runRelease.getDependsOn().contains(onlyDefaultTask)
  }

  def "android.testOptions: Can be disabled for JUnit 5 tasks via the extension"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions {
        unitTests.all {
          jvmArgs "-noverify"
          systemProperty "some.prop", "0815"
          environment "MY_ENV_VAR", "MegaShark.bin"
        }
      }
    }

    project.android {
      testOptions.junitPlatform {
        applyDefaultTestOptions false
      }
    }

    project.evaluate()

    then:
    def runDebug = project.tasks.getByName("junitPlatformTestDebug") as AndroidJUnit5UnitTest
    def runRelease = project.tasks.getByName("junitPlatformTestRelease") as AndroidJUnit5UnitTest

    assert !runDebug.jvmArgs.contains("-noverify")
    assert !runDebug.systemProperties.containsKey("some.prop")
    assert !runDebug.environment.containsKey("MY_ENV_VAR")
    assert !runRelease.jvmArgs.contains("-noverify")
    assert !runRelease.systemProperties.containsKey("some.prop")
    assert !runRelease.environment.containsKey("MY_ENV_VAR")
  }

  // FIXME When the deprecated property is removed, delete this test as well
  def "android.testOptions: Can be disabled for JUnit 5 tasks via the extension (Old DSL)"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions {
        unitTests.all {
          jvmArgs "-noverify"
          systemProperty "some.prop", "0815"
          environment "MY_ENV_VAR", "MegaShark.bin"
        }
      }
    }

    project.android {
      testOptions.junitPlatform {
        applyDefaultTestOptions false
      }
    }

    project.evaluate()

    then:
    def runDebug = project.tasks.getByName("junitPlatformTestDebug") as AndroidJUnit5UnitTest
    def runRelease = project.tasks.getByName("junitPlatformTestRelease") as AndroidJUnit5UnitTest

    assert !runDebug.jvmArgs.contains("-noverify")
    assert !runDebug.systemProperties.containsKey("some.prop")
    assert !runDebug.environment.containsKey("MY_ENV_VAR")
    assert !runRelease.jvmArgs.contains("-noverify")
    assert !runRelease.systemProperties.containsKey("some.prop")
    assert !runRelease.environment.containsKey("MY_ENV_VAR")
  }

  def "Custom reportsDir still creates unique path per variant"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      testOptions.junitPlatform {
        reportsDir project.file("${project.buildDir.absolutePath}/other-path/test-reports")
      }
    }

    project.evaluate()

    then:
    def expectedVariants = ["debug", "release"]
    def expectedReportDirs = expectedVariants
        .collect { project.tasks.getByName("junitPlatformTest${it.capitalize()}") }
        .collect { it as AndroidJUnit5UnitTest }
        .collect { TaskUtils.argument(it, "--reports-dir") }
        .unique()

    assert expectedReportDirs.size() == expectedVariants.size()
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
    }

    project.junitPlatform {
      jacoco {
        xml {
          enabled false
          destination project.file("build/other-jacoco-folder/xml")
        }
        html {
          enabled false
          destination project.file("build/html-reports/jacoco")
        }
        csv {
          enabled true
          destination project.file("build/CSVISDABEST")
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
    assert runDebug.reports.xml.destination.path.endsWith("build/other-jacoco-folder/xml")
    assert runDebug.reports.html.enabled == false
    assert runDebug.reports.html.destination.path.endsWith("build/html-reports/jacoco")
    assert runDebug.reports.csv.enabled == true
    assert runDebug.reports.csv.destination.path.endsWith("build/CSVISDABEST")
  }

  // FIXME Deprecated. Remove test once APIs are deleted
  @SuppressWarnings("GroovyPointlessBoolean")
  def "Application: Jacoco Integration Using Old Configuration Parameters"() {
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
    }

    project.junitPlatform {
      jacoco {
        xmlReport false
        htmlReport false
        csvReport true
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

  def "Application: Jacoco doesn't include Test-Scoped Sources or Classes"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyJacocoPlugin()
        .buildAndEvaluate()

    then:
    def runDebug = project.tasks.getByName("jacocoTestReportDebug") as AndroidJUnit5JacocoReport
    def runRelease = project.tasks.getByName("jacocoTestReportRelease") as AndroidJUnit5JacocoReport

    assert !runDebug.sourceDirectories.asPath.contains("src/test/java")
    assert !runDebug.sourceDirectories.asPath.contains("src/testDebug/java")
    assert !runRelease.sourceDirectories.asPath.contains("src/test/java")
    assert !runRelease.sourceDirectories.asPath.contains("src/testRelease/java")

    assert !runDebug.classDirectories.asPath.contains("classes/test/")
    assert !runRelease.classDirectories.asPath.contains("classes/test/")
  }

  def "Jacoco Exclude Rules: Addition"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyJacocoPlugin()
        .build()

    // Create some fake class files to verify the Jacoco task's tree
    project.file("build/intermediates/classes/debug").mkdirs()
    project.file("build/intermediates/classes/debug/R.class").createNewFile()
    project.file("build/intermediates/classes/debug/FirstFile.class").createNewFile()
    project.file("build/intermediates/classes/debug/SecondFile.class").createNewFile()
    project.file("src/main/java").mkdirs()
    project.file("src/main/java/OkFile.java").createNewFile()
    project.file("src/main/java/AnnoyingFile.java").createNewFile()

    project.junitPlatform {
      jacoco {
        // In addition to the default exclusion rules for R.class,
        // also exclude any class prefixed with "Second"
        excludedClasses += "Second*.class"
        excludedSources += "AnnoyingFile.java"
      }
    }

    project.evaluate()

    then:
    def jacocoTask = project.tasks.getByName("jacocoTestReportDebug") as AndroidJUnit5JacocoReport

    def classFiles = jacocoTask.classDirectories.asFileTree.files
    classFiles.find { it.name == "R.class" } == null
    classFiles.find { it.name == "FirstFile.class" } != null
    classFiles.find { it.name == "SecondFile.class" } == null

    def sourceFiles = jacocoTask.sourceDirectories.asFileTree.files
    sourceFiles.find { it.name == "OkFile.java" } != null
    sourceFiles.find { it.name == "AnnoyingFile.java" } == null
  }

  def "Jacoco Exclude Rules: Replacement"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .applyJacocoPlugin()
        .build()

    // Create some fake class files to verify the Jacoco task's tree
    project.file("build/intermediates/classes/debug").mkdirs()
    project.file("build/intermediates/classes/debug/R.class").createNewFile()
    project.file("build/intermediates/classes/debug/FirstFile.class").createNewFile()
    project.file("build/intermediates/classes/debug/SecondFile.class").createNewFile()
    project.file("src/main/java").mkdirs()
    project.file("src/main/java/OkFile.java").createNewFile()
    project.file("src/main/java/AnnoyingFile.java").createNewFile()

    project.junitPlatform {
      jacoco {
        // Replace the default exclusion rules
        // and only exclude any class prefixed with "Second"
        excludedClasses = ["Second*.class"]
        excludedSources = ["AnnoyingFile.java"]
      }
    }

    project.evaluate()

    then:
    def jacocoTask = project.tasks.getByName("jacocoTestReportDebug") as AndroidJUnit5JacocoReport

    def files = jacocoTask.classDirectories.asFileTree.files
    files.find { it.name == "R.class" } != null
    files.find { it.name == "FirstFile.class" } != null
    files.find { it.name == "SecondFile.class" } == null

    def sourceFiles = jacocoTask.sourceDirectories.asFileTree.files
    sourceFiles.find { it.name == "OkFile.java" } != null
    sourceFiles.find { it.name == "AnnoyingFile.java" } == null
  }

  def "Application: Custom Product Flavors"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      // "All flavors must now belong to a named flavor dimension"
      flavorDimensions "price"

      productFlavors {
        free { dimension "price" }
        paid { dimension "price" }
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
      // "All flavors must now belong to a named flavor dimension"
      flavorDimensions "price"

      productFlavors {
        free { dimension "price" }
        paid { dimension "price" }
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

  def "Feature: Basic Integration"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidFeature()
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

  def "Feature: Jacoco Integration"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidFeature()
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

  def "Feature: Jacoco Tasks not added if plugin absent"() {
    when:
    def project = factory.newProject(rootProject())
        .asAndroidFeature()
        .applyJunit5Plugin()
        .applyJacocoPlugin(false)
        .buildAndEvaluate()

    then:
    project.tasks.findByName("jacocoTestReport") == null
    project.tasks.findByName("jacocoTestReportDebug") == null
    project.tasks.findByName("jacocoTestReportRelease") == null
  }

  def "Instrumentation Test Integration: Attempting to use library without enabling throws Exception"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      testOptions.junitPlatform.instrumentationTests {
        enabled = false
      }
    }

    project.dependencies {
      androidTestImplementation junit5.instrumentationTests()
    }

    project.evaluate()

    then:
    def expect = thrown(ProjectConfigurationException)
    expect.message.contains("instrumentationTests.enabled true")
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

  def "Instrumentation Test Integration: Attaches RunnerBuilder"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      testOptions.junitPlatform.instrumentationTests {
        enabled true
      }
    }

    project.evaluate()

    then:
    def args = project.android.defaultConfig.getTestInstrumentationRunnerArguments()
    assert args.containsKey("runnerBuilder")
    assert args["runnerBuilder"].contains("AndroidJUnit5Builder")
  }

  def "Instrumentation Test Integration: Raise error if another RunnerBuilder replaces ours"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      defaultConfig {
        testInstrumentationRunnerArgument "runnerBuilder", "com.something.else.OtherRunnerBuilder"
      }

      testOptions.junitPlatform.instrumentationTests {
        enabled true
      }
    }

    project.evaluate()

    then:
    def expect = thrown(ProjectConfigurationException)
    assert expect.cause.message == "Custom runnerBuilder is overwriting JUnit 5 integration! Change your declaration to 'com.something.else.OtherRunnerBuilder,de.mannodermaus.junit5.AndroidJUnit5Builder'."
  }

  def "Instrumentation Test Integration: Don't raise error if another RunnerBuilder acknowledges ours"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .applyJunit5Plugin()
        .build()

    project.android {
      defaultConfig {
        testInstrumentationRunnerArgument "runnerBuilder", "com.something.else.OtherRunnerBuilder,de.mannodermaus.junit5.AndroidJUnit5Builder"
      }

      testOptions.junitPlatform.instrumentationTests {
        enabled true
      }
    }

    project.evaluate()

    then:
    def args = project.android.defaultConfig.getTestInstrumentationRunnerArguments()
    assert args.containsKey("runnerBuilder")
    assert args["runnerBuilder"].contains("com.something.else.OtherRunnerBuilder")
    assert args["runnerBuilder"].contains("de.mannodermaus.junit5.AndroidJUnit5Builder")
  }

  def "Instrumentation Test Integration: Runner Library is added automatically"() {
    when:
    Project project = factory.newProject(rootProject())
        .asAndroidApplication()
        .build()

    project.android {
      testOptions.junitPlatform {
        instrumentationTests {
          enabled true
          version = "2.88.9"
        }
      }
    }

    project.evaluate()

    then:
    def config = TestExtensionsKt.findConfiguration(project.configurations, null,
        ConfigurationKind.ANDROID_TEST, ConfigurationScope.RUNTIME_ONLY)
    assert config.dependencies.find {
      it.group == "de.mannodermaus.junit5" &&
          it.name ==
          "android-instrumentation-test-runner" &&
          it.version ==
          "2.88.9"
    } != null
  }
}
