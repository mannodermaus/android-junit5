package de.mannodermaus.junit5.internal.runners

import androidx.test.platform.app.InstrumentationRegistry
import de.mannodermaus.junit5.internal.discovery.GeneratedFilters
import de.mannodermaus.junit5.internal.discovery.ParsedSelectors
import de.mannodermaus.junit5.internal.discovery.PropertiesParser
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.Filter
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

internal data class AndroidJUnit5RunnerParams(
    private val selectors: List<DiscoverySelector> = emptyList(),
    private val filters: List<Filter<*>> = emptyList(),
    val environmentVariables: Map<String, String> = emptyMap(),
    val systemProperties: Map<String, String> = emptyMap(),
    private val configurationParameters: Map<String, String> = emptyMap()
) {

    fun createDiscoveryRequest(): LauncherDiscoveryRequest =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(this.selectors)
            .filters(*this.filters.toTypedArray())
            .configurationParameters(this.configurationParameters)
            .build()

    fun isIsolatedMethodRun(): Boolean {
        return selectors.size == 1 && selectors.first() is MethodSelector
    }
}

private const val ARG_ENVIRONMENT_VARIABLES = "environmentVariables"
private const val ARG_SYSTEM_PROPERTIES = "systemProperties"
private const val ARG_CONFIGURATION_PARAMETERS = "configurationParameters"

internal fun createRunnerParams(testClass: Class<*>): AndroidJUnit5RunnerParams {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val arguments = InstrumentationRegistry.getArguments()

    // Parse environment variables & pass them to the JVM
    val environmentVariables = arguments.getString(ARG_ENVIRONMENT_VARIABLES)
        ?.run { PropertiesParser.fromString(this) }
        ?: emptyMap()

    // Parse system properties & pass them to the JVM
    val systemProperties = arguments.getString(ARG_SYSTEM_PROPERTIES)
        ?.run { PropertiesParser.fromString(this) }
        ?: emptyMap()

    // Parse configuration parameters
    val configurationParameters = arguments.getString(ARG_CONFIGURATION_PARAMETERS)
        ?.run { PropertiesParser.fromString(this) }
        ?: emptyMap()

    // Parse the selectors to use from what's handed to the runner.
    val selectors = ParsedSelectors.fromBundle(testClass, arguments)

    // The user may apply test filters to their instrumentation tests through the Gradle plugin's DSL,
    // which aren't subject to the filtering imposed through adb.
    // A special resource file may be looked up at runtime, containing
    // the filters to apply by the AndroidJUnit5 runner.
    val filters = GeneratedFilters.fromContext(instrumentation.context)

    return AndroidJUnit5RunnerParams(
        selectors,
        filters,
        environmentVariables,
        systemProperties,
        configurationParameters
    )
}
