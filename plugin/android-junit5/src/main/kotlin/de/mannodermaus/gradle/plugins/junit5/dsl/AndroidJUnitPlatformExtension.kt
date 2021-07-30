package de.mannodermaus.gradle.plugins.junit5.dsl

import de.mannodermaus.gradle.plugins.junit5.internal.config.FILTERS_EXTENSION_NAME
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionByName
import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import org.junit.platform.commons.util.Preconditions
import javax.inject.Inject

public abstract class AndroidJUnitPlatformExtension
@Inject constructor(project: Project)
    : GroovyObjectSupport(), ExtensionAware {

    public operator fun invoke(config: AndroidJUnitPlatformExtension.() -> Unit) {
        this.config()
    }

    // Interop with Groovy
    @Suppress("unused")
    public fun methodMissing(name: String, args: Any): Any? {
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

    @get:Input
    public val configurationParameters: Map<String, String>
        get() = _configurationParameters.toMap()

    /**
     * Add a configuration parameter
     */
    public fun configurationParameter(key: String, value: String) {
        Preconditions.notBlank(key, "key must not be blank")
        Preconditions.condition(!key.contains('=')) { "key must not contain '=': \"$key\"" }
        Preconditions.notNull(value) { "value must not be null for key: \"$key\"" }
        _configurationParameters[key] = value
    }

    /**
     * Add a map of configuration parameters
     */
    public fun configurationParameters(parameters: Map<String, String>) {
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
    public val filters: FiltersExtension
        get() = findFilters(qualifier = null)

    /**
     * Configure the {@link FiltersExtension}
     * for tests that belong to the provided build variant
     */
    public fun filters(qualifier: String, action: Action<FiltersExtension>) {
        filters(qualifier) {
            action.execute(this)
        }
    }

    public fun filters(qualifier: String, action: FiltersExtension.() -> Unit) {
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
    public val instrumentationTests: InstrumentationTestOptions =
            project.objects.newInstance(InstrumentationTestOptions::class.java)

    /* Jacoco Reporting Integration */

    /**
     * Options for controlling Jacoco reporting
     */
    public val jacocoOptions: JacocoOptions =
            project.objects.newInstance(JacocoOptions::class.java, project)
}
