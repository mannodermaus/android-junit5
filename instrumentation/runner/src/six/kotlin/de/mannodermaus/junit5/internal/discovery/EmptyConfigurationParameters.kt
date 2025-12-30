package de.mannodermaus.junit5.internal.discovery

import androidx.annotation.RequiresApi
import org.junit.platform.engine.ConfigurationParameters
import java.util.Optional

/**
 * JUnit 6 version of the [ConfigurationParameters] interface.
 */
@RequiresApi(26)
internal object EmptyConfigurationParameters : ConfigurationParameters {
    override fun get(key: String) = Optional.empty<String>()
    override fun getBoolean(key: String) = Optional.empty<Boolean>()
    override fun keySet() = emptySet<String>()
}
