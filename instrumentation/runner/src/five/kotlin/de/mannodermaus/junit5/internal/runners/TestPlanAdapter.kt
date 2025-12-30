package de.mannodermaus.junit5.internal.runners

import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

/**
 * JUnit 5 version of the [TestPlanAdapter],
 * including the deprecated APIs that were removed in subsequent versions of the framework.
 */
internal open class TestPlanAdapter(
    val delegate: TestPlan
) : TestPlan(
    /* containsTests = */ delegate.containsTests(),
    /* configurationParameters = */ delegate.configurationParameters,
    /* outputDirectoryCreator = */ delegate.outputDirectoryCreator
) {
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun getChildren(parentId: String): Set<TestIdentifier> {
        return delegate.getChildren(parentId)
    }
}
