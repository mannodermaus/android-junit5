package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.extend
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import de.mannodermaus.gradle.plugins.junit5.internal.extensionExists
import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.util.ConfigureUtil
import org.junit.platform.commons.util.Preconditions
import java.io.File

internal fun attachDsl(project: Project, projectConfig: ProjectConfig) {
  // Hook the JUnit Platform configuration into the Android testOptions,
  // adding an extension point for all variants, as well as the default one
  // shared between them
  project.android.testOptions
      .extend<AndroidJUnitPlatformExtension>(EXTENSION_NAME, arrayOf(project)) { ju5 ->
        // General-purpose filters
        ju5.attachFiltersDsl(qualifier = null)

        // Variant-specific filters:
        // This will add filters for build types (e.g. "debug" or "release")
        // as well as composed variants  (e.g. "freeDebug" or "paidRelease")
        // and product flavors (e.g. "free" or "paid")
        project.android.buildTypes.all { buildType ->
          ju5.attachFiltersDsl(qualifier = buildType.name)
        }

        projectConfig.unitTestVariants.all { variant ->
          ju5.attachFiltersDsl(qualifier = variant.name)
          ju5.attachFiltersDsl(qualifier = variant.buildType.name)
        }

        project.android.productFlavors.all { flavor ->
          ju5.attachFiltersDsl(qualifier = flavor.name)
        }
      }
}

internal fun evaluateDsl(project: Project) {
  val ju5 = project.android.testOptions
      .extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

  ju5._filters.forEach { qualifier, configs ->
    val extensionName = ju5.filtersExtensionName(qualifier)
    val extension = ju5.extensionByName<FiltersExtension>(extensionName)
    configs.forEach { config ->
      extension.config()
    }
  }
}

private fun AndroidJUnitPlatformExtension.attachFiltersDsl(qualifier: String? = null) {
  val extensionName = filtersExtensionName(qualifier)

  if (this.extensionExists(extensionName)) {
    return
  }

  this.extend<FiltersExtension>(extensionName)
}


/**
 * The main extension provided through the android-junit5 Gradle plugin.
 * It defines the root of the configuration tree exposed by the plugin,
 * and is located under "android.testOptions" using the name "junitPlatform".
 */
open class AndroidJUnitPlatformExtension(private val project: Project) : GroovyObjectSupport() {

  operator fun invoke(config: AndroidJUnitPlatformExtension.() -> Unit) {
    this.config()
  }

  // Interop with Groovy
  @Suppress("unused")
  open fun methodMissing(name: String, args: Any): Any? {
    if (name.endsWith("Filters")) {
      // Support for filters() DSL called from Groovy
      val qualifier = name.substring(0, name.indexOf("Filters"))
      val closure = (args as Array<*>)[0] as Closure<*>
      return this.filters(qualifier) {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call(this)
      }
    }

    return null
  }

  /**
   * The version of JUnit Platform to use
   */
  var platformVersion: String? = null

  fun platformVersion(version: String?) {
    this.platformVersion = version
  }

  /**
   * The version of JUnit Jupiter to use
   */
  var jupiterVersion: String? = null

  fun jupiterVersion(version: String?) {
    this.jupiterVersion = version
  }

  /**
   * The version of JUnit Vintage to use
   */
  var vintageVersion: String? = null

  fun vintageVersion(version: String?) {
    this.vintageVersion = version
  }

  /**
   * The additional configuration parameters to be used
   */
  private val _configurationParameters = mutableMapOf<String, String>()
  val configurationParameters: Map<String, String>
    @Input get() = _configurationParameters.toMap()

  /**
   * Add a configuration parameter
   */
  fun configurationParameter(key: String, value: String) {
    Preconditions.notBlank(key, "key must not be blank")
    Preconditions.condition(!key.contains('=')) { "key must not contain '=': \"$key\"" }
    Preconditions.notNull(value) { "value must not be null for key: \"$key\"" }
    _configurationParameters[key] = value
  }

  /**
   * Add a map of configuration parameters
   */
  fun configurationParameters(parameters: Map<String, String>) {
    parameters.forEach { configurationParameter(it.key, it.value) }
  }

  /* Filters */

  internal val _filters = mutableMapOf<String?, MutableList<FiltersExtension.() -> Unit>>()

  internal fun filtersExtensionName(qualifier: String? = null) = if (qualifier.isNullOrEmpty())
    FILTERS_EXTENSION_NAME
  else
    "$qualifier${FILTERS_EXTENSION_NAME.capitalize()}"

  /**
   * Return the {@link FiltersExtension}
   * for all executed tests, applied to all variants
   */
  val filters: FiltersExtension get() = findFilters(qualifier = null)

  /**
   * Configure the {@link FiltersExtension}
   * for all executed tests, applied to all variants
   * (Kotlin version)
   */
  fun filters(config: FiltersExtension.() -> Unit) {
    filters(null, config)
  }

  /**
   * Configure the {@link FiltersExtension}
   * for all executed tests, applied to all variants
   * (Groovy version)
   */
  fun filters(action: Action<FiltersExtension>) = filters(null) { action.execute(this) }

  /**
   * Return the {@link FiltersExtension}
   * for tests that belong to the provided build variant
   */
  fun findFilters(qualifier: String? = null): FiltersExtension {
    val extensionName = this.filtersExtensionName(qualifier)
    return extensionByName(extensionName)
  }

  /**
   * Configure the {@link FiltersExtension}
   * for tests that belong to the provided build variant
   * (Kotlin version)
   */
  fun filters(qualifier: String? = null, config: FiltersExtension.() -> Unit) {
    val configs = _filters.getOrDefault(qualifier, mutableListOf())
    configs += config
    _filters[qualifier] = configs
  }

  /**
   * Configure the {@link FiltersExtension}
   * for tests that belong to the provided build variant
   * (Groovy version)
   */
  fun filters(qualifier: String? = null, action: Action<FiltersExtension>) = filters(
      qualifier) { action.execute(this) }

  /* Android Instrumentation Test support */

  /**
   * Options for controlling instrumentation test execution with JUnit 5
   *
   * @since 1.0.22
   */
  val instrumentationTests = InstrumentationTestOptions()

  /**
   * Options for controlling instrumentation test execution with JUnit 5
   *
   * @since 1.0.22
   */
  fun instrumentationTests(closure: Closure<InstrumentationTestOptions>) {
    ConfigureUtil.configure(closure, instrumentationTests)
  }

  /* Jacoco Reporting Integration */

  /**
   * Options for controlling Jacoco reporting
   */
  val jacocoOptions = JacocoOptions()

  /**
   * Options for controlling Jacoco reporting
   */
  fun jacocoOptions(closure: Closure<JacocoOptions>) {
    ConfigureUtil.configure(closure, jacocoOptions)
  }
}

/**
 * Discovery selector configuration options for the plugin
 */
open class SelectorsExtension {

  operator fun invoke(config: SelectorsExtension.() -> Unit) {
    this.config()
  }

  /* URIs */

  private val _uris = mutableListOf<String>()
  val uris @Input get() = _uris.toList()

  /**
   * Add a <em>URI</em> to be used for test discovery
   */
  fun uri(uri: String) = uris(uri)

  /**
   * Add one or more <em>URIs</em> to be used for test discovery
   */
  fun uris(vararg uris: String) = this._uris.addAll(uris)

  /* Files */

  private val _files = mutableListOf<String>()
  val files @Input get() = _files.toList()

  /**
   * Add a <em>file</em> to be used for test discovery
   */
  fun file(file: String) = files(file)

  /**
   * Add one or more <em>files</em> to be used for test discovery
   */
  fun files(vararg files: String) = this._files.addAll(files)

  /* Directories */

  private val _directories = mutableListOf<String>()
  val directories @Input get() = _directories.toList()

  /**
   * Add a <em>directory</em> to be used for test discovery
   */
  fun directory(directory: String) = directories(directory)

  /**
   * Add one or more <em>directories</em> to be used for test discovery
   */
  fun directories(vararg directories: String) = this._directories.addAll(directories)

  /* Packages */

  private val _packages = mutableListOf<String>()
  val packages @Input get() = _packages.toList()

  /**
   * Add a <em>package</em> to be used for test discovery
   */
  @JvmName("aPackage")
  fun `package`(aPackage: String) = packages(aPackage)

  /**
   * Add one or more <em>packages</em> to be used for test discovery
   */
  fun packages(vararg packages: String) = this._packages.addAll(packages)

  fun packages() = _packages.toList()

  /* Classes */

  private val _classes = mutableListOf<String>()
  val classes @Input get() = _classes.toList()

  /**
   * Add a <em>class</em> to be used for test discovery
   */
  @JvmName("aClass")
  fun `class`(aClass: String) = classes(aClass)

  /**
   * Add one or more <em>classes</em> to be used for test discovery
   */
  fun classes(vararg classes: String) = this._classes.addAll(classes)

  fun classes() = _classes.toList()

  /* Methods */

  private val _methods = mutableListOf<String>()
  val methods @Input get() = _methods.toList()

  /**
   * Add a <em>method</em> to be used for test discovery
   */
  fun method(method: String) = methods(method)

  /**
   * Add one or more <em>methods</em> to be used for test discovery
   */
  fun methods(vararg methods: String) = this._methods.addAll(methods)

  /* Resources */

  private val _resources = mutableListOf<String>()
  val resources @Input get() = _resources.toList()

  /**
   * Add a <em>resource</em> to be used for test discovery
   */
  fun resource(resource: String) = resources(resource)

  /**
   * Add one or more <em>resources</em> to be used for test discovery
   */
  fun resources(vararg resources: String) = this._resources.addAll(resources)

  fun isEmpty() = _uris.isEmpty() && _files.isEmpty() && _directories.isEmpty() && _packages.isEmpty() && _classes.isEmpty() && _methods.isEmpty() && _resources.isEmpty()
}

open class FiltersExtension {

  operator fun invoke(config: FiltersExtension.() -> Unit) {
    this.config()
  }

  /**
   * Class name patterns in the form of regular expressions for
   * classes that should be <em>included</em> in the test plan.
   *
   * <p>The patterns are combined using OR semantics, i.e. if the fully
   * qualified name of a class matches against at least one of the patterns,
   * the class will be included in the test plan.
   */
  internal val patterns = IncludeExcludeContainer()

  /**
   * Add a pattern to the list of <em>included</em> patterns
   */
  fun includePattern(pattern: String) = includePatterns(pattern)

  /**
   * Add patterns to the list of <em>included</em> patterns
   */
  fun includePatterns(vararg patterns: String) {
    this.patterns.include(*patterns)
  }

  /**
   * Add a pattern to the list of <em>excluded</em> patterns
   */
  fun excludePattern(pattern: String) = excludePatterns(pattern)

  /**
   * Add patterns to the list of <em>excluded</em> patterns
   */
  fun excludePatterns(vararg patterns: String) =
      this.patterns.exclude(*patterns)

  /**
   * Included & Excluded JUnit 5 tags.
   */
  internal val tags = IncludeExcludeContainer()

  /**
   * Add tags to the list of <em>included</em> tags
   */
  fun includeTags(vararg tags: String) {
    this.tags.include(*tags)
  }

  /**
   * Add tags to the list of <em>excluded</em> tags
   */
  fun excludeTags(vararg tags: String) {
    this.tags.exclude(*tags)
  }

  /**
   * Included & Excluded JUnit 5 engines.
   */
  internal val engines = IncludeExcludeContainer()

  /**
   * Add engines to the list of <em>included</em> engines
   */
  fun includeEngines(vararg engines: String) {
    this.engines.include(*engines)
  }

  /**
   * Add engines to the list of <em>excluded</em> engines
   */
  fun excludeEngines(vararg engines: String) {
    this.engines.exclude(*engines)
  }
}

open class IncludeExcludeContainer {
  private val _include = mutableSetOf<String>()
  val include @Input get() = _include.toSet()
  fun include(vararg items: String) = this.apply {
    this._include.addAll(items)
    this._exclude.removeAll(items)
  }

  private val _exclude = mutableSetOf<String>()
  val exclude @Input get() = _exclude.toSet()
  fun exclude(vararg items: String) = this.apply {
    this._exclude.addAll(items)
    this._include.removeAll(items)
  }

  fun isEmpty() = _include.isEmpty() && _exclude.isEmpty()

  operator fun plus(other: IncludeExcludeContainer): IncludeExcludeContainer {
    // Fast path, where nothing needs to be merged
    if (this.isEmpty()) return other
    if (other.isEmpty()) return this

    // Slow path, where rules need to be merged
    val result = IncludeExcludeContainer()

    result._include.addAll(this.include)
    result._include.addAll(other.include)
    result._include.removeAll(other.exclude)

    result._exclude.addAll(this.exclude)
    result._exclude.addAll(other.exclude)
    result._exclude.removeAll(other.include)

    return result
  }

  override fun toString(): String {
    return "${super.toString()}(include=$_include, exclude=$_exclude)"
  }
}

/**
 * Options for controlling instrumentation test execution
 */
class InstrumentationTestOptions {

  operator fun invoke(config: InstrumentationTestOptions.() -> Unit) {
    this.config()
  }

  /**
   * Whether or not to enable support for JUnit 5 instrumentation tests
   */
  var enabled: Boolean = true

  fun enabled(state: Boolean) {
    this.enabled = state
  }

  /**
   * The version of the instrumentation companion library to use
   */
  var version: String? = null

  fun version(version: String?) {
    this.version = version
  }
}


/**
 * Options for controlling Jacoco reporting
 */
class JacocoOptions {

  operator fun invoke(config: JacocoOptions.() -> Unit) {
    this.config()
  }

  /**
   * Whether or not to enable Jacoco task integration
   */
  var taskGenerationEnabled = true

  fun taskGenerationEnabled(state: Boolean) {
    this.taskGenerationEnabled = state
  }

  private val _onlyGenerateTasksForVariants = mutableSetOf<String>()
  val onlyGenerateTasksForVariants @Input get() = _onlyGenerateTasksForVariants.toSet()

  /**
   * Filter the generated Jacoco tasks,
   * so that only the given build variants are provided with a companion task.
   * Make sure to add the full product flavor name if necessary
   * (i.e. "paidDebug" if you use a "paid" product flavor and the "debug" build type)
   */
  fun onlyGenerateTasksForVariants(vararg variants: String) {
    _onlyGenerateTasksForVariants.addAll(variants)
  }

  /**
   * Options for controlling the HTML Report generated by Jacoco
   */
  val html = Report()

  /**
   * Options for controlling the HTML Report generated by Jacoco
   */
  fun html(closure: Closure<Report>) {
    ConfigureUtil.configure(closure, html)
  }

  /**
   * Options for controlling the CSV Report generated by Jacoco
   */
  val csv = Report()

  /**
   * Options for controlling the CSV Report generated by Jacoco
   */
  fun csv(closure: Closure<Report>) {
    ConfigureUtil.configure(closure, csv)
  }

  /**
   * Options for controlling the XML Report generated by Jacoco
   */
  val xml = Report()

  /**
   * Options for controlling the XML Report generated by Jacoco
   */
  fun xml(closure: Closure<Report>) {
    ConfigureUtil.configure(closure, xml)
  }

  /**
   * List of class name patterns that should be excluded from being processed by Jacoco.
   * By default, this will exclude R.class & BuildConfig.class
   */
  var excludedClasses = mutableListOf("**/R.class", "**/R$*.class", "**/BuildConfig.*")

  fun excludedClasses(vararg classes: String) = excludedClasses.addAll(classes)

  class Report {

    operator fun invoke(config: Report.() -> Unit) {
      this.config()
    }

    /**
     * Whether or not this report should be generated
     */
    var enabled: Boolean = true

    fun enabled(state: Boolean) {
      this.enabled = state
    }

    /**
     * Name of the file to be generated; note that
     * due to the variant-aware nature of the plugin,
     * each variant will be assigned a distinct folder if necessary
     */
    var destination: File? = null

    fun destination(file: File?) {
      this.destination = file
    }
  }
}
