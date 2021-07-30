package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.config.FILTERS_EXTENSION_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionByName
import de.mannodermaus.gradle.plugins.junit5.internal.utils.IncludeExcludeContainer
import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import org.junit.platform.commons.util.Preconditions
import java.io.File


/**
 * The main extension provided through the android-junit5 Gradle plugin.
 * It defines the root of the configuration tree exposed by the plugin,
 * and is registered under the name "junitPlatform".
 */
abstract class AndroidJUnitPlatformExtension : GroovyObjectSupport(), ExtensionAware {

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
   * for tests that belong to the provided build variant
   */
  internal fun findFilters(qualifier: String? = null): FiltersExtension {
    val extensionName = this.filtersExtensionName(qualifier)
    return extensionByName(extensionName)
  }

  /**
   * Return the {@link FiltersExtension}
   * for all executed tests, applied to all variants
   */
  val filters: FiltersExtension
    get() = findFilters(qualifier = null)

  /**
   * Configure the {@link FiltersExtension}
   * for all executed tests, applied to all variants
   */
  fun filters(action: Action<FiltersExtension>) = filters(null) {
    action.execute(this)
  }

  /**
   * Configure the {@link FiltersExtension}
   * for tests that belong to the provided build variant
   */
  fun filters(qualifier: String?, action: Action<FiltersExtension>) {
    filters(qualifier) {
      action.execute(this)
    }
  }

  fun filters(qualifier: String? = null, action: FiltersExtension.() -> Unit) {
    val actions = _filters.getOrDefault(qualifier, mutableListOf())
    actions += action
    _filters[qualifier] = actions
  }

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
  fun instrumentationTests(action: Action<InstrumentationTestOptions>) {
    action.execute(instrumentationTests)
  }

  /* Jacoco Reporting Integration */

  /**
   * Options for controlling Jacoco reporting
   */
  val jacocoOptions = JacocoOptions()

  /**
   * Options for controlling Jacoco reporting
   */
  fun jacocoOptions(action: Action<JacocoOptions>) {
    action.execute(jacocoOptions)
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
  fun includePattern(pattern: String) {
    includePatterns(pattern)
  }

  /**
   * Add patterns to the list of <em>included</em> patterns
   */
  fun includePatterns(vararg patterns: String) {
    this.patterns.include(*patterns)
  }

  /**
   * Add a pattern to the list of <em>excluded</em> patterns
   */
  fun excludePattern(pattern: String) {
    excludePatterns(pattern)
  }

  /**
   * Add patterns to the list of <em>excluded</em> patterns
   */
  fun excludePatterns(vararg patterns: String) {
    this.patterns.exclude(*patterns)
  }

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

/**
 * Options for controlling instrumentation test execution
 */
class InstrumentationTestOptions {

  operator fun invoke(config: InstrumentationTestOptions.() -> Unit) {
    this.config()
  }

  /**
   * Whether or not to check if the instrumentation tests
   * are correctly set up. If this is disabled, the plugin
   * won't raise an error during evaluation if the instrumentation
   * libraries or the test runner are missing.
   */
  var integrityCheckEnabled = true

  /**
   * Whether or not to check if the instrumentation tests
   * are correctly set up. If this is disabled, the plugin
   * won't raise an error during evaluation if the instrumentation
   * libraries or the test runner are missing.
   */
  fun integrityCheckEnabled(state: Boolean) {
    this.integrityCheckEnabled = state
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
  fun html(action: Action<Report>) {
    action.execute(html)
  }

  /**
   * Options for controlling the CSV Report generated by Jacoco
   */
  val csv = Report()

  /**
   * Options for controlling the CSV Report generated by Jacoco
   */
  fun csv(action: Action<Report>) {
    action.execute(csv)
  }

  /**
   * Options for controlling the XML Report generated by Jacoco
   */
  val xml = Report()

  /**
   * Options for controlling the XML Report generated by Jacoco
   */
  fun xml(action: Action<Report>) {
    action.execute(xml)
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
