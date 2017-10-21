package de.mannodermaus.gradle.plugins.android_junit5

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.android_junit5.jacoco.AndroidJUnit5JacocoExtension
import de.mannodermaus.gradle.plugins.android_junit5.jacoco.AndroidJUnit5JacocoReport
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.util.GradleVersion
import org.junit.platform.gradle.plugin.*

/*
 * Android JUnit Platform plugin for Gradle.
 * Configures JUnit 5 tasks on all variants of an Android project.
 */

@SuppressWarnings("GrMethodMayBeStatic")
class AndroidJUnitPlatformPlugin implements Plugin<Project> {

  static final String LOG_TAG = "[android-junit5]"

  static final String MIN_REQUIRED_GRADLE_VERSION = "2.5"
  static final String VERSIONS_PROP_RESOURCE_NAME = "versions.properties"

  static final String EXTENSION_NAME = "junitPlatform"
  static final String DEP_CONFIGURATION_NAME = "junitPlatform"
  static final String SELECTORS_EXTENSION_NAME = "selectors"
  static final String FILTERS_EXTENSION_NAME = "filters"
  static final String PACKAGES_EXTENSION_NAME = "packages"
  static final String TAGS_EXTENSION_NAME = "tags"
  static final String ENGINES_EXTENSION_NAME = "engines"
  static final String JACOCO_EXTENSION_NAME = "jacoco"

  // Mirrored from "versions.properties" resource file
  static final String ANDROID_JUNIT5_VERSION_PROP = "androidJunit5Version"
  static final String JUNIT_PLATFORM_VERSION_PROP = "junitPlatformVersion"
  static final String JUNIT_JUPITER_VERSION_PROP = "junitJupiterVersion"
  static final String JUNIT_VINTAGE_VERSION_PROP = "junitVintageVersion"
  static final String JUNIT4_VERSION_PROP = "junit4Version"

  private ProjectConfig projectConfig

  @Override
  void apply(Project project) {
    if (GradleVersion.current() < GradleVersion.version(MIN_REQUIRED_GRADLE_VERSION)) {
      throw new GradleException(
          "android-junit5 plugin requires Gradle version $MIN_REQUIRED_GRADLE_VERSION or higher")
    }

    this.projectConfig = new ProjectConfig(project)

    // Validate that an Android plugin is applied
    if (!projectConfig.androidPluginApplied) {
      throw new ProjectConfigurationException(
          "The android or android-library plugin must be applied to this project", null)
    }

    configureExtensions(project)
    configureDependencies(project)

    project.afterEvaluate {
      configureTasks(project)
    }
  }

  /*
   * Read the content of the versions configuration file & return its properties.
   */

  private static def readVersionsFromProperties() {
    Properties properties = new Properties()
    def stream = AndroidJUnitPlatformPlugin.class.getResourceAsStream(VERSIONS_PROP_RESOURCE_NAME)
    stream.withReader { properties.load(it) }
    return properties
  }

  /*
   * Configures the exposed extensions for JUnit 5 customization on the given project.
   */

  private def configureExtensions(Project project) {
    // Construct the platform extension (use our Android variant of the Extension class though)
    def junit5 = project.extensions.create(EXTENSION_NAME, AndroidJUnitPlatformExtension, project)
    junit5.extensions.create(SELECTORS_EXTENSION_NAME, SelectorsExtension)
    junit5.extensions.create(FILTERS_EXTENSION_NAME, FiltersExtension)
    junit5.filters.extensions.create(PACKAGES_EXTENSION_NAME, PackagesExtension)
    junit5.filters.extensions.create(TAGS_EXTENSION_NAME, TagsExtension)
    junit5.filters.extensions.create(ENGINES_EXTENSION_NAME, EnginesExtension)
    junit5.extensions.create(JACOCO_EXTENSION_NAME, AndroidJUnit5JacocoExtension)
    return junit5
  }

  /*
   * Configures the required dependencies for JUnit 5 on the given project.
   */

  private def configureDependencies(Project project) {
    // Fallback versions read from a configuration file,
    // used whenever no explicit version is present
    def defaultVersions = readVersionsFromProperties()
    createConfiguration(project, defaultVersions)
    createDependencyHandlers(project, defaultVersions)
  }

  /*
   * Add required JUnit Platform dependencies to a custom configuration that
   * will later be used to create the classpath for the custom task created by this plugin.
   */

  private def createConfiguration(Project project, Properties defaultVersions) {
    def junit5 = project.extensions.getByName(EXTENSION_NAME) as AndroidJUnitPlatformExtension

    def configuration = project.configurations.maybeCreate(DEP_CONFIGURATION_NAME)
    configuration.defaultDependencies { deps ->
      // By default, include both TestEngines and the
      // Launcher-related dependencies on the runtime classpath
      def platformVersion = junit5.platformVersion ?:
          defaultVersions.getProperty(JUNIT_PLATFORM_VERSION_PROP)
      def jupiterVersion = junit5.jupiterVersion ?:
          defaultVersions.getProperty(JUNIT_JUPITER_VERSION_PROP)
      def vintageVersion = junit5.vintageVersion ?:
          defaultVersions.getProperty(JUNIT_VINTAGE_VERSION_PROP)

      deps.add(project.dependencies.create(
          "org.junit.platform:junit-platform-launcher:$platformVersion"))
      deps.add(
          project.dependencies.create("org.junit.platform:junit-platform-console:$platformVersion"))

      deps.add(
          project.dependencies.create("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion"))

      deps.add(
          project.dependencies.create("org.junit.vintage:junit-vintage-engine:$vintageVersion"))
    }
  }

  /*
   * Add custom dependency handlers, providing convenience for users seeking to include
   * all relevant JUnit 5 dependencies at once.
   *
   * This adds the following handlers:
   * - junit5()
   * - junit5Params()
   * - junit5EmbeddedRuntime()
   */

  private def createDependencyHandlers(Project project, Properties defaultVersions) {
    def junit5 = project.extensions.getByName(EXTENSION_NAME) as AndroidJUnitPlatformExtension

    project.dependencies.ext.junit5 = {
      def platformVersion = junit5.platformVersion ?:
          defaultVersions.getProperty(JUNIT_PLATFORM_VERSION_PROP)
      def jupiterVersion = junit5.jupiterVersion ?:
          defaultVersions.getProperty(JUNIT_JUPITER_VERSION_PROP)
      def vintageVersion = junit5.vintageVersion ?:
          defaultVersions.getProperty(JUNIT_VINTAGE_VERSION_PROP)

      def junit4Version = defaultVersions.getProperty(JUNIT4_VERSION_PROP)

      return [project.dependencies.create("junit:junit:$junit4Version"),
              project.dependencies.create("org.junit.jupiter:junit-jupiter-api:$jupiterVersion"),
              project.dependencies.create(
                  "org.junit.platform:junit-platform-engine:$platformVersion"),

              // Only needed to run tests in an Android Studio that bundles an older version
              // (see also http://junit.org/junit5/docs/current/user-guide/#running-tests-ide-intellij-idea)
              project.dependencies.create(
                  "org.junit.platform:junit-platform-launcher:$platformVersion"),
              project.dependencies.create(
                  "org.junit.platform:junit-platform-console:$platformVersion"),
              project.dependencies.create("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion"),
              project.dependencies.create("org.junit.vintage:junit-vintage-engine:$vintageVersion")]
    }

    project.dependencies.ext.junit5Params = {
      def jupiterVersion = junit5.jupiterVersion ?:
          defaultVersions.getProperty(JUNIT_JUPITER_VERSION_PROP)

      return project.dependencies.create("org.junit.jupiter:junit-jupiter-params:$jupiterVersion")
    }

    project.dependencies.ext.junit5EmbeddedRuntime = {
      def embeddedRuntimeVersion = defaultVersions.getProperty(ANDROID_JUNIT5_VERSION_PROP)

      return project.dependencies.create(
          "de.mannodermaus.gradle.plugins:android-junit5-embedded-runtime:$embeddedRuntimeVersion")
    }
  }

  /* Add relevant JUnit 5 tasks for test execution & coverage reports to the given project. */

  private def configureTasks(Project project) {
    // Add the test task to each of the project's unit test variants,
    // and connect a Code Coverage report to it if Jacoco is enabled.
    def allVariants = projectConfig.androidLibraryPluginApplied ? "libraryVariants" :
        "applicationVariants"
    def testVariants = project.android[allVariants].findAll { it.hasProperty("unitTestVariant") }

    def isJacocoApplied = projectConfig.jacocoPluginApplied

    testVariants.each { variant ->
      def testTask = AndroidJUnit5Test.create(projectConfig, variant as BaseVariant)

      if (isJacocoApplied) {
        AndroidJUnit5JacocoReport.create(project, testTask)
      }
    }
  }
}
