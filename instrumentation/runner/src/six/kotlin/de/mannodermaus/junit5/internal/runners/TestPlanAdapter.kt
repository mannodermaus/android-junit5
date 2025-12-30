package de.mannodermaus.junit5.internal.runners

import org.junit.platform.launcher.TestPlan

/** JUnit 6 version of the [TestPlanAdapter]. */
internal open class TestPlanAdapter(val delegate: TestPlan) :
    TestPlan(
        /* containsTests = */ delegate.containsTests(),
        /* configurationParameters = */ delegate.configurationParameters,
        /* outputDirectoryCreator = */ delegate.outputDirectoryCreator,
    )
