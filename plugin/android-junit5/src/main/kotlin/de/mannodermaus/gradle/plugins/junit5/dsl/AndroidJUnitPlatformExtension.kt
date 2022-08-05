package de.mannodermaus.gradle.plugins.junit5.dsl

import de.mannodermaus.gradle.plugins.junit5.internal.config.EXTENSION_NAME
import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.junit.platform.commons.util.Preconditions
import javax.inject.Inject

public abstract class AndroidJUnitPlatformExtension @Inject constructor(
    private val objects: ObjectFactory
) : GroovyObjectSupport() {

    internal companion object {
        fun Project.createJUnit5Extension() =
            extensions.create(EXTENSION_NAME, AndroidJUnitPlatformExtension::class.java)
    }

    internal operator fun invoke(block: AndroidJUnitPlatformExtension.() -> Unit) {
        this.block()
    }

    @get:Input
    public abstract val configurationParameters: MapProperty<String, String>

    /**
     * Add a configuration parameter
     */
    public fun configurationParameter(key: String, value: String) {
        Preconditions.notBlank(key, "key must not be blank")
        Preconditions.condition(!key.contains('=')) { "key must not contain '=': \"$key\"" }
        Preconditions.notNull(value) { "value must not be null for key: \"$key\"" }
        configurationParameters.put(key, value)
    }

    /**
     * Add a map of configuration parameters
     */
    public fun configurationParameters(parameters: Map<String, String>) {
        parameters.forEach { configurationParameter(it.key, it.value) }
    }

    /* Filters */

    private val _filters = mutableMapOf<String?, FiltersExtension>()

    /**
     * Configure the {@link FiltersExtension}
     * for tests that belong to the provided build variant
     */
    public fun filters(qualifier: String?, action: Action<FiltersExtension>) {
        action.execute(filters(qualifier))
    }

    /**
     * Configure the global {@link FiltersExtension} for all variants.
     */
    public fun filters(action: Action<FiltersExtension>) {
        filters(null, action)
    }

    internal fun filters(qualifier: String? = null): FiltersExtension {
        return _filters.getOrPut(qualifier) {
            objects.newInstance(FiltersExtension::class.java)
        }
    }

    @Suppress("unused")
    public fun methodMissing(name: String, args: Any): Any? {
        // Interop with Groovy
        if (name.endsWith("Filters")) {
            // Support for filters() DSL called from Groovy
            val qualifier = name.substring(0, name.indexOf("Filters"))
            val closure = (args as Array<*>)[0] as Closure<*>
            return filters(qualifier) {
                closure.delegate = this
                closure.resolveStrategy = Closure.DELEGATE_FIRST
                closure.call(this)
            }
        }

        return null
    }
    
    /* Android Instrumentation Test support */

    /**
     * Options for controlling instrumentation test execution with JUnit 5
     */
    public val instrumentationTests: InstrumentationTestOptions =
        objects.newInstance(InstrumentationTestOptions::class.java)

    public fun instrumentationTests(action: Action<InstrumentationTestOptions>) {
        action.execute(instrumentationTests)
    }

    /* Jacoco Reporting Integration */

    /**
     * Options for controlling Jacoco reporting
     */
    public val jacocoOptions: JacocoOptions =
        objects.newInstance(JacocoOptions::class.java)

    public fun jacocoOptions(action: Action<JacocoOptions>) {
        action.execute(jacocoOptions)
    }
}
