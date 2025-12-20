package de.mannodermaus.junit5.internal.discovery

import androidx.annotation.RequiresApi
import org.junit.platform.engine.OutputDirectoryCreator
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.TestPlan
import java.io.File

/**
 * A JUnit TestPlan that does absolutely nothing.
 * Used by [de.mannodermaus.junit5.internal.runners.AndroidJUnit5] whenever a class
 * is not loadable through the JUnit Platform and should be discarded.
 */
@RequiresApi(26)
internal object EmptyTestPlan : TestPlan(
    /* containsTests = */ false,
    /* configurationParameters = */ EmptyConfigurationParameters,
    /* outputDirectoryCreator = */ emptyOutputDirectoryCreator
)

@RequiresApi(26)
private val emptyOutputDirectoryCreator = object : OutputDirectoryCreator {
    private val path = File.createTempFile("empty-output", ".nop").toPath()
    override fun getRootDirectory() = path
    override fun createOutputDirectory(testDescriptor: TestDescriptor) = path
}
