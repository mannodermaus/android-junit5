package de.mannodermaus.junit5.internal.runners

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import de.mannodermaus.junit5.internal.discovery.GeneratedFilters
import de.mannodermaus.junit5.internal.discovery.ParsedSelectors
import de.mannodermaus.junit5.internal.discovery.PropertiesParser
import de.mannodermaus.junit5.internal.discovery.ShardingFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.Filter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

internal data class JUnitFrameworkRunnerParams(
    private val arguments: Bundle = Bundle(),
    private val filters: List<Filter<*>> = emptyList(),
    val environmentVariables: Map<String, String> = emptyMap(),
    val systemProperties: Map<String, String> = emptyMap(),
    private val configurationParameters: Map<String, String> = emptyMap()
) {
    fun createSelectors(testClass: Class<*>): List<DiscoverySelector> {
        return ParsedSelectors.fromBundle(testClass, arguments)
    }

    fun createDiscoveryRequest(selectors: List<DiscoverySelector>): LauncherDiscoveryRequest {
        return LauncherDiscoveryRequestBuilder.request()
            .selectors(selectors)
            .filters(*this.filters.toTypedArray())
            .configurationParameters(this.configurationParameters)
            .build()
    }

    val isParallelExecutionEnabled: Boolean
        get() = configurationParameters["junit.jupiter.execution.parallel.enabled"] == "true"

    val isUsingOrchestrator: Boolean
        get() = arguments.getString("orchestratorService") != null

    internal companion object {
        fun create(): JUnitFrameworkRunnerParams {
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

            // The user may apply test filters to their instrumentation tests through the Gradle plugin's DSL,
            // which aren't subject to the filtering imposed through adb.
            // A special resource file may be looked up at runtime, containing
            // the filters to apply by the AndroidJUnit5 runner.
            val filters = GeneratedFilters.fromContext(instrumentation.context) +
                    listOfNotNull(ShardingFilter.fromArguments(arguments))

            return JUnitFrameworkRunnerParams(
                arguments,
                filters,
                environmentVariables,
                systemProperties,
                configurationParameters
            )
        }
    }
}

private const val ARG_ENVIRONMENT_VARIABLES = "environmentVariables"
private const val ARG_SYSTEM_PROPERTIES = "systemProperties"
private const val ARG_CONFIGURATION_PARAMETERS = "configurationParameters"
