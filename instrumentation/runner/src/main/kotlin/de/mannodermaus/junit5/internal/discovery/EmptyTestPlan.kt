package de.mannodermaus.junit5.internal.discovery

import androidx.annotation.RequiresApi
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.OutputDirectoryCreator
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.util.Optional

/**
 * A JUnit TestPlan that does absolutely nothing.
 * Used by [de.mannodermaus.junit5.internal.runners.AndroidJUnitFramework] whenever a class
 * is not loadable through the JUnit Platform and should be discarded.
 */
@RequiresApi(35)
internal object EmptyTestPlan : TestPlan(
    false,
    emptyConfigurationParameters,
    emptyOutputDirectoryProvider
)

@RequiresApi(35)
private val emptyConfigurationParameters = object : ConfigurationParameters {
    override fun get(key: String) = Optional.empty<String>()
    override fun getBoolean(key: String) = Optional.empty<Boolean>()
    override fun keySet() = emptySet<String>()
}

@RequiresApi(35)
private val emptyOutputDirectoryProvider = object : OutputDirectoryCreator {
    private val path = File.createTempFile("empty-output", ".nop").toPath()
    override fun getRootDirectory() = path
    override fun createOutputDirectory(testDescriptor: TestDescriptor) = path
}
