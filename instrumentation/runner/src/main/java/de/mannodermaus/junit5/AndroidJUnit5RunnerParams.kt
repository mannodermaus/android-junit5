package de.mannodermaus.junit5

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.Filter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

data class AndroidJUnit5RunnerParams(
    private val selectors: List<DiscoverySelector> = emptyList(),
    private val filters: List<Filter<*>> = emptyList()
) {

  fun createDiscoveryRequest(): LauncherDiscoveryRequest =
      LauncherDiscoveryRequestBuilder.request()
          .selectors(this.selectors)
          .filters(*this.filters.toTypedArray())
          .build()
}
