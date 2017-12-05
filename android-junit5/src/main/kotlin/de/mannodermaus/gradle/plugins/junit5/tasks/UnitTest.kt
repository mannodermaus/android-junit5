package de.mannodermaus.gradle.plugins.junit5.tasks

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import com.android.builder.core.VariantType
import de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.JUnit5UnitTest
import de.mannodermaus.gradle.plugins.junit5.engines
import de.mannodermaus.gradle.plugins.junit5.filters
import de.mannodermaus.gradle.plugins.junit5.getExcludeClassNamePatterns
import de.mannodermaus.gradle.plugins.junit5.getIncludeClassNamePatterns
import de.mannodermaus.gradle.plugins.junit5.isEmpty
import de.mannodermaus.gradle.plugins.junit5.junit5
import de.mannodermaus.gradle.plugins.junit5.junit5Info
import de.mannodermaus.gradle.plugins.junit5.packages
import de.mannodermaus.gradle.plugins.junit5.providers.DirectoryProvider
import de.mannodermaus.gradle.plugins.junit5.providers.classDirectories
import de.mannodermaus.gradle.plugins.junit5.safeProperty
import de.mannodermaus.gradle.plugins.junit5.selectors
import de.mannodermaus.gradle.plugins.junit5.tags
import de.mannodermaus.gradle.plugins.junit5.variantData
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.IdentityFileResolver
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaForkOptions
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.DefaultJavaForkOptions
import org.gradle.process.internal.DefaultProcessForkOptions
import org.junit.platform.console.ConsoleLauncher
import java.io.File

private const val TASK_NAME_DEFAULT = "junitPlatformTest"
private const val VERIFICATION_GROUP = JavaBasePlugin.VERIFICATION_GROUP

/**
 * Task class used for unit tests driven by JUnit 5.
 * Its API mimics the Android Gradle Plugin's [AndroidUnitTest]
 * pretty closely, and it takes advantage of the efforts related to
 * classpath construction prevalent in the platform's default implementation.
 */
open class AndroidJUnit5UnitTest : JavaExec(), JUnit5UnitTest {

  companion object {
    fun create(
        project: Project,
        variant: BaseVariant,
        directoryProviders: Collection<DirectoryProvider>): AndroidJUnit5UnitTest {
      val configAction = ConfigAction(project, variant, directoryProviders)
      return project.tasks.create(configAction.name, configAction.type, configAction)
    }
  }

  /** Android Variant connected to this test task */
  private lateinit var _variant: BaseVariant
  val variant get() = _variant

  @InputFiles
  @Optional
  var resCollection: FileCollection? = null

  @InputFiles
  @Optional
  var assetsCollection: FileCollection? = null

  /**
   * Configuration closure for an Android JUnit5 test task.
   */
  private class ConfigAction(
      val project: Project,
      val variant: BaseVariant,
      val directoryProviders: Collection<DirectoryProvider>
  ) : TaskConfigAction<AndroidJUnit5UnitTest> {

    private val scope: VariantScope = variant.variantData.scope

    override fun getName(): String = scope.getTaskName(TASK_NAME_DEFAULT)

    override fun getType() = AndroidJUnit5UnitTest::class.java

    override fun execute(task: AndroidJUnit5UnitTest) {
      // Basic configuration
      task.group = VERIFICATION_GROUP
      task._variant = variant
      task.description = "Runs tests on the JUnit Platform " +
          "for the ${variant.name.capitalize()} variant."

      // JUnit 5 properties configuration
      val junit5 = project.junit5
      configureTaskInputs(task, junit5)
      configureTaskDependencies(task, junit5)
      val reportsDir = configureTaskOutputs(task, junit5)

      // Share the task's classpath with the default unit tests managed by Android,
      // but append the JUnit Platform configuration at the end.
      //
      // Note: the user's test runtime classpath must come first; otherwise, code
      // instrumented by Clover in JUnit's build will be shadowed by JARs pulled in
      // via the junitPlatform configuration... leading to zero code coverage for
      // the respective modules.
      val defaultJUnit4Task = getDefaultJUnit4Task()
      val taskClasspath = defaultJUnit4Task.classpath +
          project.configurations.getByName("junitPlatform")

      // Aggregate test root directories from the given providers
      val testRootDirs = directoryProviders.classDirectories()
      project.logger.junit5Info("Assembled JUnit 5 Task '${task.name}':")
      project.logger.junit5Info("Root Directories:")
      testRootDirs.forEach { project.logger.junit5Info("|__ $it") }

      // Use classpath property & configure ConsoleLauncher as the main class
      task.classpath = taskClasspath
      task.main = ConsoleLauncher::class.java.name

      // Apply other arguments and properties from the default test task, unless disabled
      // (these are most likely provided by the AGP's testOptions closure)
      if (junit5.unitTests.applyDefaultTestOptions) {
        task.jvmArgs(defaultJUnit4Task.jvmArgs)
        task.systemProperties(defaultJUnit4Task.systemProperties)
        task.environment(defaultJUnit4Task.environment)
      }

      // Build the task arguments
      task.args = buildArgs(junit5, reportsDir, testRootDirs)
      project.logger.junit5Info("Launcher Arguments: ${task.args.joinToString()}")
      project.logger.junit5Info("JVM Arguments: ${task.jvmArgs.joinToString()}")

      // Hook into the main JUnit 5 task
      val defaultJUnit5Task = getDefaultJUnit5Task()
      defaultJUnit5Task.dependsOn(task)

      // Apply additional user configuration
      project.junit5.unitTests.applyConfiguration(task)
    }

    /* Private */

    private fun getDefaultJUnit4Task(): AndroidUnitTest {
      val name = scope.getTaskName(VariantType.UNIT_TEST.prefix, VariantType.UNIT_TEST.suffix)
      return project.tasks.getByName(name) as AndroidUnitTest
    }

    private fun getDefaultJUnit5Task(): Task {
      var defaultTask = project.tasks.findByName(TASK_NAME_DEFAULT)
      if (defaultTask == null) {
        defaultTask = project.tasks.create(TASK_NAME_DEFAULT, JUnit5UnitTestRunAll::class.java)
        project.junit5.unitTests.applyConfiguration(defaultTask)
      }
      return defaultTask!!
    }

    private fun configureTaskInputs(
        task: AndroidJUnit5UnitTest,
        junit5: AndroidJUnitPlatformExtension) {
      task.inputs.safeProperty("enableStandardTestTask", junit5.enableStandardTestTask)
      task.inputs.safeProperty("configurationParameters", junit5.configurationParameters)
      task.inputs.safeProperty("selectors.uris", junit5.selectors.uris)
      task.inputs.safeProperty("selectors.files", junit5.selectors.files)
      task.inputs.safeProperty("selectors.directories", junit5.selectors.directories)
      task.inputs.safeProperty("selectors.packages", junit5.selectors.packages)
      task.inputs.safeProperty("selectors.classes", junit5.selectors.classes)
      task.inputs.safeProperty("selectors.methods", junit5.selectors.methods)
      task.inputs.safeProperty("selectors.resources", junit5.selectors.resources)
      task.inputs.safeProperty("filters.engines.include", junit5.filters.engines.include)
      task.inputs.safeProperty("filters.engines.exclude", junit5.filters.engines.exclude)
      task.inputs.safeProperty("filters.tags.include", junit5.filters.tags.include)
      task.inputs.safeProperty("filters.tags.exclude", junit5.filters.tags.exclude)
      task.inputs.safeProperty("filters.includeClassNamePatterns",
          junit5.filters.getIncludeClassNamePatterns())
      task.inputs.safeProperty("filters.packages.include", junit5.filters.packages.include)
      task.inputs.safeProperty("filters.packages.exclude", junit5.filters.packages.exclude)

      junit5.logManager?.let {
        task.systemProperty("java.util.logging.manager", junit5.logManager)
      }
    }

    private fun configureTaskDependencies(
        task: AndroidJUnit5UnitTest,
        junit5: AndroidJUnitPlatformExtension) {
      // Connect to the default unit test task
      val variantUnitTestTask = this.getDefaultJUnit4Task()
      try {
        // Android Gradle Plugin 3.x provides additional input parameters
        task.resCollection = variantUnitTestTask.resCollection
        task.assetsCollection = variantUnitTestTask.assetsCollection
      } catch (ignored: Throwable) {
      }
      variantUnitTestTask.enabled = junit5.enableStandardTestTask
      variantUnitTestTask.dependsOn(task)

      // Require that test classes are assembled first
      val defaultAssembleTestName = scope.getTaskName("assemble", VariantType.UNIT_TEST.suffix)
      val defaultAssembleTask = project.tasks.getByName(defaultAssembleTestName)
      task.dependsOn(defaultAssembleTask)

      // Hook into the main test task
      val mainTestTask = project.tasks.getByName("test")
      mainTestTask.dependsOn(task)
    }

    private fun configureTaskOutputs(
        task: AndroidJUnit5UnitTest,
        junit5: AndroidJUnitPlatformExtension): File {
      val reportsDir = if (junit5.reportsDir != null) {
        // Ensure per-variant directory even with a custom "reportsDir"
        project.file("${junit5.reportsDir.absolutePath}/${variant.name}")
      } else {
        // Default path
        project.file("${project.buildDir}/test-results/${variant.name}/junit-platform")
      }

      task.outputs.dir(reportsDir)
      return reportsDir
    }

    private fun buildArgs(
        junit5: AndroidJUnitPlatformExtension,
        reportsDir: File,
        testRootDirs: List<File>): List<String> {
      val args = mutableListOf<String>()

      // Log Details
      junit5.details?.let { args += arrayOf("--details", it.name) }

      // Selectors
      if (junit5.selectors.isEmpty()) {
        // Employ classpath scanning if no selectors are given
        args += arrayOf("--scan-class-path",
            testRootDirs.joinToString(separator = File.pathSeparator))

      } else {
        // Otherwise, add each selector individually
        junit5.selectors.uris.forEach { args += arrayOf("-u", it) }
        junit5.selectors.files.forEach { args += arrayOf("-f", it) }
        junit5.selectors.directories.forEach { args += arrayOf("-d", it) }
        junit5.selectors.packages.forEach { args += arrayOf("-p", it) }
        junit5.selectors.classes.forEach { args += arrayOf("-c", it) }
        junit5.selectors.methods.forEach { args += arrayOf("-m", it) }
        junit5.selectors.resources.forEach { args += arrayOf("-r", it) }
      }

      // Filters
      junit5.filters.getIncludeClassNamePatterns().forEach { args += arrayOf("-n", it) }
      junit5.filters.getExcludeClassNamePatterns().forEach { args += arrayOf("-N", it) }
      junit5.filters.packages.include.forEach { args += arrayOf("--include-package", it) }
      junit5.filters.packages.exclude.forEach { args += arrayOf("--exclude-package", it) }
      junit5.filters.tags.include.forEach { args += arrayOf("-t", it) }
      junit5.filters.tags.exclude.forEach { args += arrayOf("-T", it) }
      junit5.filters.engines.include.forEach { args += arrayOf("-e", it) }
      junit5.filters.engines.exclude.forEach { args += arrayOf("-E", it) }

      // Custom Configuration Parameters
      junit5.configurationParameters.forEach { entry ->
        // Don't destructure directly to avoid Java 8 API
        args += arrayOf("--config", "${entry.key}=${entry.value}")
      }

      // Report Directory
      args += arrayOf("--reports-dir", reportsDir.absolutePath)

      return args
    }
  }
}

/**
 * Facade for the main JUnit 5 Unit Test task.
 * Allows the default task to also be configured by unitTests.all.
 */
open class JUnit5UnitTestRunAll : DefaultTask(), JUnit5UnitTest {

  private val emptyJavaForkOptions = DefaultJavaForkOptions(IdentityFileResolver())
  private val emptyProcessForkOptions = DefaultProcessForkOptions(IdentityFileResolver())
  private val emptyFileCollection = project.files()

  override fun setSystemProperties(p0: MutableMap<String, *>?) {
  }

  override fun getExecutable() = ""

  override fun setDefaultCharacterEncoding(p0: String?) {
  }

  override fun jvmArgs(p0: MutableIterable<*>?) = emptyJavaForkOptions

  override fun jvmArgs(vararg p0: Any?) = emptyJavaForkOptions

  override fun environment(p0: MutableMap<String, *>?) = emptyProcessForkOptions

  override fun environment(p0: String?, p1: Any?) = emptyProcessForkOptions

  override fun setMinHeapSize(p0: String?) {
  }

  override fun systemProperties(p0: MutableMap<String, *>?) = emptyJavaForkOptions

  override fun setExecutable(p0: String?) {
  }

  override fun setExecutable(p0: Any?) {
  }

  override fun executable(p0: Any?) = emptyProcessForkOptions

  override fun setJvmArgs(p0: MutableList<String>?) {
  }

  override fun setJvmArgs(p0: MutableIterable<*>?) {
  }

  override fun setAllJvmArgs(p0: MutableList<String>?) {
  }

  override fun setAllJvmArgs(p0: MutableIterable<*>?) {
  }

  override fun setDebug(p0: Boolean) {
  }

  override fun getWorkingDir() = project.file("")

  override fun setBootstrapClasspath(p0: FileCollection?) {
  }

  override fun getDefaultCharacterEncoding() = ""

  override fun setMaxHeapSize(p0: String?) {
  }

  override fun systemProperty(p0: String?, p1: Any?) = emptyJavaForkOptions

  override fun getBootstrapClasspath() = emptyFileCollection

  override fun workingDir(p0: Any?) = emptyProcessForkOptions

  override fun setWorkingDir(p0: File?) {
  }

  override fun setWorkingDir(p0: Any?) {
  }

  override fun setEnvironment(p0: MutableMap<String, *>?) {
  }

  override fun getEnableAssertions() = false

  override fun setEnableAssertions(p0: Boolean) {
  }

  override fun getMaxHeapSize() = ""

  override fun copyTo(p0: JavaForkOptions?) = emptyJavaForkOptions

  override fun copyTo(p0: ProcessForkOptions?) = emptyProcessForkOptions

  override fun getJvmArgs(): MutableList<String> {
    return mutableListOf()
  }

  override fun getSystemProperties(): MutableMap<String, Any> {
    return mutableMapOf()
  }

  override fun getMinHeapSize() = ""

  override fun getEnvironment(): MutableMap<String, Any> {
    return mutableMapOf()
  }

  override fun getAllJvmArgs(): MutableList<String> {
    return mutableListOf()
  }

  override fun getDebug() = false

  override fun bootstrapClasspath(vararg p0: Any?) = emptyJavaForkOptions
}
