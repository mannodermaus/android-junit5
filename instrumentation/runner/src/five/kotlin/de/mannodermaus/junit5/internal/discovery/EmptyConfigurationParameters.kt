package de.mannodermaus.junit5.internal.discovery

import androidx.annotation.RequiresApi
import org.junit.platform.engine.ConfigurationParameters
import java.util.Optional

/**
 * JUnit 5 version of the [ConfigurationParameters] interface,
 * including the deprecated APIs that were removed in subsequent versions of the framework.
 */
@RequiresApi(26)
internal object EmptyConfigurationParameters : ConfigurationParameters {
    override fun get(key: String) = Optional.empty<String>()
    override fun getBoolean(key: String) = Optional.empty<Boolean>()
    override fun keySet() = emptySet<String>()

    @Deprecated("Deprecated in Java", ReplaceWith("keySet().size"))
    override fun size() = 0
}
