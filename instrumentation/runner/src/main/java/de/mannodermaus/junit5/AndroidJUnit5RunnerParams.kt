package de.mannodermaus.junit5

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.Filter
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

data class AndroidJUnit5RunnerParams(
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
