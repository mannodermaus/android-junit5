package de.mannodermaus.gradle.plugins.android_junit5

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import com.android.builder.core.VariantType
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.junit.platform.console.ConsoleLauncher

/*
 * Task class used for unit tests driven by JUnit 5.
 * Its API mimics the Android Gradle Plugin's {@link AndroidUnitTest}
 * pretty closely, and it takes advantage of the efforts related to
 * classpath construction prevalent in the platform's default implementation.
 */

class AndroidJUnit5Test extends JavaExec {

  FileCollection resCollection
  FileCollection assetsCollection

  private BaseVariant variant

  BaseVariant getVariant() {
    return variant
  }

  @PackageScope
  def setVariant(BaseVariant variant) {
    this.variant = variant
  }

  @InputFiles
  @Optional
  FileCollection getResCollection() {
    return resCollection
  }

  @InputFiles
  @Optional
  FileCollection getAssetsCollection() {
    return assetsCollection
  }

  static AndroidJUnit5Test create(Project project, BaseVariant variant) {
    def configAction = new ConfigAction(project, variant)
    return project.tasks.create(configAction.getName(), configAction.getType(), configAction)
  }

  static class ConfigAction implements TaskConfigAction<AndroidJUnit5Test> {

    private static final String TASK_NAME_DEFAULT = "junitPlatformTest"
    private static final String TASK_GROUP = JavaBasePlugin.VERIFICATION_GROUP

    private final Project project
    private final BaseVariant variant
    private final VariantScope scope

    ConfigAction(Project project, BaseVariant variant) {
      this.project = project
      this.variant = variant
      this.scope = variant.variantData.scope
    }

    private String createDescription() {
      return "Runs tests on the JUnit Platform for the ${variant.name.capitalize()} variant."
    }

    @Override
    String getName() {
      return scope.getTaskName(TASK_NAME_DEFAULT)
    }

    @Override
    Class<AndroidJUnit5Test> getType() {
      return AndroidJUnit5Test.class
    }

    @Override
    void execute(AndroidJUnit5Test task) {
      task.group = TASK_GROUP
      task.description = createDescription()
      task.variant = variant

      // Configure JUnit 5 properties
      AndroidJUnitPlatformExtension junitExtension =
          project.extensions.getByName(
              AndroidJUnitPlatformPlugin.EXTENSION_NAME) as AndroidJUnitPlatformExtension
      configureTaskInputs(task, junitExtension)
      configureTaskDependencies(task, junitExtension)
      def reportsDir = configureTaskOutputs(task, junitExtension)

      // Share the classpath with the default unit tests managed by Android,
      // but append the JUnit Platform configuration at the end
      //
      // Note: the user's test runtime classpath must come first; otherwise, code
      // instrumented by Clover in JUnit's build will be shadowed by JARs pulled in
      // via the junitPlatform configuration... leading to zero code coverage for
      // the respective modules.
      task.setClasspath(defaultUnitTestTask.classpath + project.configurations.junitPlatform)

      // Aggregate the source folders for test cases
      // (usually, the unit test variant's folders should be enough,
      // however we aggregate the main scope's output as well)
      def testRootDirs = [// e.g. "build/intermediates/classes/debug/..."
                          scope.javaOutputDir,
                          // e.g. "build/intermediates/classes/test/debug/..."
                          variant.unitTestVariant.variantData.scope.javaOutputDir]
      project.logger.info(
          "$AndroidJUnitPlatformPlugin.LOG_TAG: Assembled JUnit 5 Task '$task.name':")
      testRootDirs.each {
        project.logger.info("$AndroidJUnitPlatformPlugin.LOG_TAG: |__ $it")
      }

      task.main = ConsoleLauncher.class.getName()
      task.args buildArgs(project, junitExtension, reportsDir, testRootDirs)

      project.logger.info("$AndroidJUnitPlatformPlugin.LOG_TAG: * JUnit 5 Arguments: $task.args")

      // Hook into main JUnit 5 task
      def defaultJUnit5Task = findOrCreateJUnit5Task()
      defaultJUnit5Task.dependsOn task
    }

    /* Begin private */

    private Task findOrCreateJUnit5Task() {
      def task = project.tasks.findByName(TASK_NAME_DEFAULT)
      if (!task) {
        task = project.tasks.create(TASK_NAME_DEFAULT)
        task.group = TASK_GROUP
      }
      return task
    }

    private void configureTaskInputs(AndroidJUnit5Test task,
        AndroidJUnitPlatformExtension junitExtension) {
      // Setup JUnit 5 properties
      task.inputs.property("enableStandardTestTask", junitExtension.enableStandardTestTask)
      task.inputs.property("configurationParameters", junitExtension.configurationParameters)
      task.inputs.property("selectors.uris", junitExtension.selectors.uris)
      task.inputs.property("selectors.files", junitExtension.selectors.files)
      task.inputs.property("selectors.directories", junitExtension.selectors.directories)
      task.inputs.property("selectors.packages", junitExtension.selectors.packages)
      task.inputs.property("selectors.classes", junitExtension.selectors.classes)
      task.inputs.property("selectors.methods", junitExtension.selectors.methods)
      task.inputs.property("selectors.resources", junitExtension.selectors.resources)
      task.inputs.property("filters.engines.include", junitExtension.filters.engines.include)
      task.inputs.property("filters.engines.exclude", junitExtension.filters.engines.exclude)
      task.inputs.property("filters.tags.include", junitExtension.filters.tags.include)
      task.inputs.property("filters.tags.exclude", junitExtension.filters.tags.exclude)
      task.inputs.property("filters.includeClassNamePatterns",
          junitExtension.filters.includeClassNamePatterns)
      task.inputs.property("filters.packages.include", junitExtension.filters.packages.include)
      task.inputs.property("filters.packages.exclude", junitExtension.filters.packages.exclude)

      if (junitExtension.logManager) {
        systemProperty "java.util.logging.manager", junitExtension.logManager
      }
    }

    private def configureTaskOutputs(AndroidJUnit5Test task,
        AndroidJUnitPlatformExtension junitExtension) {
      def reportsDir = junitExtension.reportsDir ?:
          project.file("$project.buildDir/test-results/junit-platform")
      task.outputs.dir reportsDir

      return reportsDir
    }

    private def configureTaskDependencies(AndroidJUnit5Test task,
        AndroidJUnitPlatformExtension junitExtension) {
      // Connect to the default unit test task
      def variantUnitTestTask = this.defaultUnitTestTask
      if (variantUnitTestTask.hasProperty("resCollection")) {
        // 3.x provides additional input parameters
        task.resCollection = variantUnitTestTask.resCollection
        task.assetsCollection = variantUnitTestTask.assetsCollection
      }
      variantUnitTestTask.setEnabled(junitExtension.enableStandardTestTask)
      variantUnitTestTask.dependsOn task

      // Depend on the assembly of the test classes
      def defaultAssembleTestName = scope.getTaskName("assemble",
          VariantType.UNIT_TEST.getSuffix())
      def variantAssembleTask = project.tasks.getByName(defaultAssembleTestName)
      task.dependsOn variantAssembleTask

      // Hook into the main test task
      def mainTestTask = project.tasks.getByName("test")
      mainTestTask.dependsOn task
    }

    private def getDefaultUnitTestTask() {
      def defaultUnitTestName = scope.getTaskName(VariantType.UNIT_TEST.getPrefix(),
          VariantType.UNIT_TEST.getSuffix())
      return project.tasks.getByName(defaultUnitTestName) as AndroidUnitTest
    }

    private List<String> buildArgs(project, junitExtension, reportsDir, testRootDirs) {
      def args = []

      if (junitExtension.details) {
        args.addAll(["--details", junitExtension.details.name()])
      }

      addSelectors(project, junitExtension.selectors, testRootDirs, args)
      addFilters(junitExtension.filters, args)

      junitExtension.configurationParameters.each {
        key, value -> args.addAll("--config", "${key}=${value}")
      }

      args.addAll(["--reports-dir", reportsDir.getAbsolutePath()])

      return args
    }

    private void addFilters(filters, args) {
      filters.includeClassNamePatterns.each { args.addAll(["-n", it]) }
      filters.excludeClassNamePatterns.each { args.addAll(['-N', it]) }
      filters.packages.include.each { args.addAll(["--include-package", it]) }
      filters.packages.exclude.each { args.addAll(["--exclude-package", it]) }
      filters.tags.include.each { args.addAll(["-t", it]) }
      filters.tags.exclude.each { args.addAll(["-T", it]) }
      filters.engines.include.each { args.addAll(["-e", it]) }
      filters.engines.exclude.each { args.addAll(["-E", it]) }
    }

    private void addSelectors(project, selectors, testRootDirs, args) {
      if (selectors.empty) {
        args.addAll(["--scan-class-path", testRootDirs.join(File.pathSeparator)])
      } else {
        selectors.uris.each { args.addAll(["-u", it]) }
        selectors.files.each { args.addAll(["-f", it]) }
        selectors.directories.each { args.addAll(["-d", it]) }
        selectors.packages.each { args.addAll(["-p", it]) }
        selectors.classes.each { args.addAll(["-c", it]) }
        selectors.methods.each { args.addAll(["-m", it]) }
        selectors.resources.each { args.addAll(["-r", it]) }
      }
    }
  }
}
